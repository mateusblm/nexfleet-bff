package br.com.nexfleet.bff.gerenciarabastecimentos.application;

import java.math.BigDecimal;
import java.util.UUID;

/** Consulta, edição e exclusão de um abastecimento no Operações. */
public interface AbastecimentoPorIdRemoto {
  /** Campos editáveis: data, veículo e motorista ficam de fora e nunca chegam ao Operações. */
  record Atualizacao(String combustivel, BigDecimal litros, BigDecimal valorTotal,
      Long quilometragem) {}

  AbastecimentosRemotos.Dados consultar(String token, UUID id);
  AbastecimentosRemotos.Dados atualizar(String token, UUID id, Atualizacao atualizacao);
  void excluir(String token, UUID id);
}
