package br.com.nexfleet.bff.consultaragregado;

import static org.junit.jupiter.api.Assertions.*;

import br.com.nexfleet.bff.consultaragregado.infrastructure.OperacoesPorServicos;
import br.com.nexfleet.bff.gerenciarabastecimentos.application.AbastecimentosRemotos;
import br.com.nexfleet.bff.gerenciarmanutencoes.application.ManutencoesRemotas;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class OperacoesPorServicosTest {
  static final UUID VEICULO = UUID.fromString("00000000-0000-0000-0000-00000000000a");

  @Test
  void leAteQuatroMilDeCadaHistoricoSemMarcarTruncado() {
    var historico =
        new OperacoesPorServicos(abastecimentos(4000), manutencoes(4000)).consultar("Bearer t");

    assertEquals(4000, historico.abastecimentos().size());
    assertEquals(4000, historico.manutencoes().size());
    assertFalse(historico.truncado());
  }

  @Test
  void acimaDeQuatroMilAbastecimentosCortaEMarcaTruncado() {
    var historico =
        new OperacoesPorServicos(abastecimentos(4001), manutencoes(3)).consultar("Bearer t");

    assertEquals(4000, historico.abastecimentos().size());
    assertEquals(3, historico.manutencoes().size());
    assertTrue(historico.truncado());
  }

  @Test
  void acimaDeQuatroMilManutencoesCortaEMarcaTruncado() {
    var historico =
        new OperacoesPorServicos(abastecimentos(0), manutencoes(4250)).consultar("Bearer t");

    assertEquals(0, historico.abastecimentos().size());
    assertEquals(4000, historico.manutencoes().size());
    assertTrue(historico.truncado());
  }

  /** Operações fictício que pagina {@code total} registros como o serviço real. */
  private static AbastecimentosRemotos abastecimentos(int total) {
    return new AbastecimentosRemotos() {
      @Override
      public List<Dados> listar(String token, UUID veiculoId, int pagina, int tamanho) {
        return pagina(total, pagina, tamanho)
            .mapToObj(
                i ->
                    new Dados(
                        UUID.randomUUID(),
                        VEICULO,
                        UUID.randomUUID(),
                        Instant.parse("2026-10-01T10:00:00Z"),
                        "DIESEL",
                        BigDecimal.ONE,
                        BigDecimal.TEN,
                        i,
                        Instant.parse("2026-10-01T10:00:00Z")))
            .toList();
      }

      @Override
      public Dados registrar(String token, Entrada entrada) {
        throw new UnsupportedOperationException();
      }
    };
  }

  private static ManutencoesRemotas manutencoes(int total) {
    return new ManutencoesRemotas() {
      @Override
      public List<Dados> listar(String token, UUID veiculoId, int pagina, int tamanho) {
        return pagina(total, pagina, tamanho)
            .mapToObj(
                i ->
                    new Dados(
                        UUID.randomUUID(),
                        VEICULO,
                        "PREVENTIVA",
                        LocalDate.parse("2026-10-05"),
                        i,
                        "Troca de óleo",
                        "Oficina",
                        BigDecimal.TEN,
                        null,
                        Instant.parse("2026-10-05T10:00:00Z")))
            .toList();
      }

      @Override
      public Dados registrar(String token, Entrada entrada) {
        throw new UnsupportedOperationException();
      }
    };
  }

  private static IntStream pagina(int total, int pagina, int tamanho) {
    int inicio = Math.min(total, pagina * tamanho);
    return IntStream.range(inicio, Math.min(total, inicio + tamanho));
  }
}
