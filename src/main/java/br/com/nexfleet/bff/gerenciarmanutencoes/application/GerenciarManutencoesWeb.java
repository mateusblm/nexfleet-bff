package br.com.nexfleet.bff.gerenciarmanutencoes.application;

import java.util.*;

public class GerenciarManutencoesWeb {
  private final ManutencoesRemotas manutencoes;
  private final ManutencaoPorIdRemota porId;
  public GerenciarManutencoesWeb(ManutencoesRemotas manutencoes, ManutencaoPorIdRemota porId) {
    this.manutencoes = manutencoes;
    this.porId = porId;
  }
  public List<ManutencoesRemotas.Dados> listar(
      String token, UUID veiculoId, int pagina, int tamanho) {
    return manutencoes.listar(bearer(token), veiculoId, pagina, tamanho);
  }
  public ManutencoesRemotas.Dados registrar(String token, ManutencoesRemotas.Entrada entrada) {
    return manutencoes.registrar(bearer(token), entrada);
  }
  public ManutencoesRemotas.Dados consultar(String token, UUID id) {
    return porId.consultar(bearer(token), id);
  }
  public ManutencoesRemotas.Dados atualizar(
      String token, UUID id, ManutencaoPorIdRemota.Atualizacao atualizacao) {
    return porId.atualizar(bearer(token), id, atualizacao);
  }
  public void excluir(String token, UUID id) {
    porId.excluir(bearer(token), id);
  }
  private String bearer(String valor) {
    if (valor == null || valor.length() > 8192 || !valor.matches("Bearer [^\\s]+"))
      throw new FalhaManutencoes(401);
    return valor;
  }
}
