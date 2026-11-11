package br.com.nexfleet.bff.consultaragregado.application;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Function {@code POST /api/calcular-eficiencia}; autenticada pela chave da função, não pelo JWT. */
public interface EficienciaRemota {
  record AbastecimentoCalculo(
      Instant data, BigDecimal litros, BigDecimal valor, long quilometragem) {}

  record ManutencaoCalculo(BigDecimal valor) {}

  record VeiculoHistorico(
      UUID veiculoId,
      String placa,
      List<AbastecimentoCalculo> abastecimentos,
      List<ManutencaoCalculo> manutencoes) {}

  record Entrada(List<VeiculoHistorico> veiculos) {}

  record EficienciaVeiculo(
      UUID veiculoId,
      String placa,
      BigDecimal consumoMedioKmL,
      BigDecimal custoPorKm,
      BigDecimal gastoAbastecimento,
      BigDecimal gastoManutencao,
      BigDecimal gastoTotal,
      long distanciaKm) {}

  record EficienciaFrota(
      BigDecimal consumoMedioKmL, BigDecimal custoPorKm, BigDecimal gastoTotal, long distanciaKm) {}

  record Eficiencia(List<EficienciaVeiculo> veiculos, EficienciaFrota frota) {
    public static Eficiencia vazia() {
      return new Eficiencia(List.of(), null);
    }
  }

  Eficiencia calcular(Entrada entrada);
}
