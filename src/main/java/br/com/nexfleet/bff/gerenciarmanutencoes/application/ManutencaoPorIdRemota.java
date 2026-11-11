package br.com.nexfleet.bff.gerenciarmanutencoes.application;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** Consulta, edição e exclusão de uma manutenção no Operações. */
public interface ManutencaoPorIdRemota {
  /** Campos editáveis: o veículo fica de fora e nunca chega ao Operações. */
  record Atualizacao(String tipo, LocalDate realizadaEm, Long quilometragem, String descricao,
      String oficina, BigDecimal valor, LocalDate proximaManutencao) {}

  ManutencoesRemotas.Dados consultar(String token, UUID id);
  ManutencoesRemotas.Dados atualizar(String token, UUID id, Atualizacao atualizacao);
  void excluir(String token, UUID id);
}
