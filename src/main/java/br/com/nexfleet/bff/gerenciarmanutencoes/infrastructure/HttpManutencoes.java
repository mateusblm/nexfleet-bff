package br.com.nexfleet.bff.gerenciarmanutencoes.infrastructure;

import br.com.nexfleet.bff.gerenciarmanutencoes.application.*;
import java.util.*;
import java.util.function.Supplier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.*;

public class HttpManutencoes implements ManutencoesRemotas, ManutencaoPorIdRemota {
  private static final String PATH = "/api/v1/operacoes/manutencoes";
  private final RestClient client;
  public HttpManutencoes(RestClient client) { this.client = client; }

  @Override
  public List<Dados> listar(String token, UUID veiculoId, int pagina, int tamanho) {
    return chamada(() -> {
      var lista = client.get().uri(builder -> {
        var uri = builder.path(PATH).queryParam("pagina", pagina).queryParam("tamanho", tamanho);
        if (veiculoId != null) uri.queryParam("veiculoId", veiculoId);
        return uri.build();
      }).header("Authorization", token).retrieve()
          .body(new ParameterizedTypeReference<List<Dados>>() {});
      if (lista == null) throw new FalhaManutencoes(503);
      lista.forEach(this::validar);
      return lista;
    });
  }

  @Override
  public Dados registrar(String token, Entrada entrada) {
    return chamada(() -> validar(client.post().uri(PATH).header("Authorization", token)
        .body(entrada).retrieve().body(Dados.class)));
  }

  @Override
  public Dados consultar(String token, UUID id) {
    return chamada(() -> validar(client.get().uri(PATH + "/{id}", id)
        .header("Authorization", token).retrieve().body(Dados.class)));
  }

  @Override
  public Dados atualizar(String token, UUID id, Atualizacao atualizacao) {
    return chamada(() -> validar(client.put().uri(PATH + "/{id}", id)
        .header("Authorization", token).body(atualizacao).retrieve().body(Dados.class)));
  }

  @Override
  public void excluir(String token, UUID id) {
    chamada(() -> client.delete().uri(PATH + "/{id}", id)
        .header("Authorization", token).retrieve().toBodilessEntity());
  }

  private Dados validar(Dados dados) {
    if (dados == null || dados.id() == null || dados.veiculoId() == null || dados.tipo() == null
        || dados.realizadaEm() == null || dados.descricao() == null || dados.oficina() == null
        || dados.valor() == null || dados.criadoEm() == null)
      throw new FalhaManutencoes(503);
    return dados;
  }

  private <T> T chamada(Supplier<T> request) {
    try { return request.get(); }
    catch (RestClientResponseException erro) {
      int status = erro.getStatusCode().value();
      if (Set.of(403,404,422).contains(status))
        throw new FalhaManutencoes(status, mensagemDoOperacoes(erro));
      throw new FalhaManutencoes(Set.of(400,401,409).contains(status) ? status : 503);
    } catch (RestClientException erro) { throw new FalhaManutencoes(503); }
  }

  private record Mensagem(String message) {}

  /** Repassa só o campo público {@code message} do Operações; corpo fora desse formato é ignorado. */
  private static String mensagemDoOperacoes(RestClientResponseException erro) {
    try {
      var corpo = erro.getResponseBodyAs(Mensagem.class);
      var texto = corpo == null ? null : corpo.message();
      return texto == null || texto.length() > 500 ? null : texto;
    } catch (RuntimeException ignorada) {
      return null;
    }
  }
}
