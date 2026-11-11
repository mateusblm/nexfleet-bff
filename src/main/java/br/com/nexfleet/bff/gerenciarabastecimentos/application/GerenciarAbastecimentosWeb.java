package br.com.nexfleet.bff.gerenciarabastecimentos.application;

import java.util.*;

public class GerenciarAbastecimentosWeb {
  private final AbastecimentosRemotos abastecimentos;
  private final AbastecimentoPorIdRemoto porId;
  public GerenciarAbastecimentosWeb(
      AbastecimentosRemotos abastecimentos, AbastecimentoPorIdRemoto porId) {
    this.abastecimentos = abastecimentos;
    this.porId = porId;
  }
  public List<AbastecimentosRemotos.Dados> listar(
      String token, UUID veiculoId, int pagina, int tamanho) {
    return abastecimentos.listar(bearer(token), veiculoId, pagina, tamanho);
  }
  public AbastecimentosRemotos.Dados registrar(
      String token, AbastecimentosRemotos.Entrada entrada) {
    return abastecimentos.registrar(bearer(token), entrada);
  }
  public AbastecimentosRemotos.Dados consultar(String token, UUID id) {
    return porId.consultar(bearer(token), id);
  }
  public AbastecimentosRemotos.Dados atualizar(
      String token, UUID id, AbastecimentoPorIdRemoto.Atualizacao atualizacao) {
    return porId.atualizar(bearer(token), id, atualizacao);
  }
  public void excluir(String token, UUID id) {
    porId.excluir(bearer(token), id);
  }
  private String bearer(String valor) {
    if (valor == null || valor.length() > 8192 || !valor.matches("Bearer [^\\s]+"))
      throw new FalhaAbastecimentos(401);
    return valor;
  }
}
