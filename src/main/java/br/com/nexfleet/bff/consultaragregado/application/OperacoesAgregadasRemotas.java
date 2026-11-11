package br.com.nexfleet.bff.consultaragregado.application;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Histórico de Operações; alimenta tanto os últimos registros quanto o cálculo da Function. */
public interface OperacoesAgregadasRemotas {
  record Abastecimento(
      UUID id,
      UUID veiculoId,
      Instant realizadoEm,
      String combustivel,
      BigDecimal litros,
      BigDecimal valorTotal,
      long quilometragem) {}

  record Manutencao(
      UUID id,
      UUID veiculoId,
      String tipo,
      LocalDate realizadaEm,
      long quilometragem,
      BigDecimal valor,
      LocalDate proximaManutencao) {}

  /** {@code truncado}: Operações tinha mais registros do que o teto lido pelo BFF. */
  record Historico(
      List<Abastecimento> abastecimentos, List<Manutencao> manutencoes, boolean truncado) {
    public Historico(List<Abastecimento> abastecimentos, List<Manutencao> manutencoes) {
      this(abastecimentos, manutencoes, false);
    }
  }

  Historico consultar(String token);
}
