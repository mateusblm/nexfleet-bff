package br.com.nexfleet.bff.consultaragregado;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import br.com.nexfleet.bff.consultaragregado.application.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "agregado.timeout=300ms")
@AutoConfigureMockMvc
@ActiveProfiles("jwt-rsa")
@ExtendWith(OutputCaptureExtension.class)
class ConsultarAgregadoHttpTest {
  static final String TOKEN = "Bearer token-valido";
  static final String CORRELATION = "7d1f0a52-4c1e-4a43-9a51-2f5a3a3e9b10";
  static final UUID VEICULO = UUID.fromString("00000000-0000-0000-0000-00000000000a");
  static final UUID MOTORISTA = UUID.fromString("00000000-0000-0000-0000-0000000000b1");

  @Autowired MockMvc mvc;
  @MockitoBean JwtDecoder jwtDecoder;
  @MockitoBean FrotaAgregadaRemota frota;
  @MockitoBean OperacoesAgregadasRemotas operacoes;
  @MockitoBean EficienciaRemota eficiencia;

  @BeforeEach
  void autenticacao() {
    when(jwtDecoder.decode(anyString())).thenThrow(new BadJwtException("invalido"));
    doReturn(
            Jwt.withTokenValue("token-valido")
                .header("alg", "RS256")
                .subject("e2f6b588-00cf-45bc-927e-c4a08647dfde")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(600))
                .build())
        .when(jwtDecoder)
        .decode("token-valido");
  }

  FrotaAgregadaRemota.Frota dadosFrota() {
    return new FrotaAgregadaRemota.Frota(
        List.of(
            new FrotaAgregadaRemota.Veiculo(
                VEICULO, "ABC1D23", "Volkswagen", "Delivery", 2025, "Caminhão", 10500, "EM_USO")),
        List.of(
            new FrotaAgregadaRemota.Motorista(
                MOTORISTA, "Ana Souza", "D", LocalDate.parse("2030-01-01"), "ATIVO")),
        List.of(
            new FrotaAgregadaRemota.VinculoAtivo(
                UUID.randomUUID(),
                VEICULO,
                "ABC1D23",
                MOTORISTA,
                "Ana Souza",
                Instant.parse("2026-10-01T08:00:00Z"))));
  }

  OperacoesAgregadasRemotas.Historico dadosOperacoes() {
    return new OperacoesAgregadasRemotas.Historico(
        List.of(
            new OperacoesAgregadasRemotas.Abastecimento(
                UUID.randomUUID(),
                VEICULO,
                Instant.parse("2026-10-01T10:00:00Z"),
                "DIESEL",
                new BigDecimal("40.00"),
                new BigDecimal("200.00"),
                10000),
            new OperacoesAgregadasRemotas.Abastecimento(
                UUID.randomUUID(),
                VEICULO,
                Instant.parse("2026-10-20T10:00:00Z"),
                "DIESEL",
                new BigDecimal("80.00"),
                new BigDecimal("400.00"),
                10500)),
        List.of(
            new OperacoesAgregadasRemotas.Manutencao(
                UUID.randomUUID(),
                VEICULO,
                "PREVENTIVA",
                LocalDate.parse("2026-10-05"),
                10100,
                new BigDecimal("150.00"),
                null)));
  }

  EficienciaRemota.Eficiencia dadosEficiencia() {
    return new EficienciaRemota.Eficiencia(
        List.of(
            new EficienciaRemota.EficienciaVeiculo(
                VEICULO,
                "ABC1D23",
                new BigDecimal("6.25"),
                new BigDecimal("1.50"),
                new BigDecimal("600.00"),
                new BigDecimal("150.00"),
                new BigDecimal("750.00"),
                500)),
        new EficienciaRemota.EficienciaFrota(
            new BigDecimal("6.25"), new BigDecimal("1.50"), new BigDecimal("750.00"), 500));
  }

  @Test
  void compoeAsTresFontesNasDuasRotas() throws Exception {
    when(frota.consultar(TOKEN)).thenReturn(dadosFrota());
    when(operacoes.consultar(TOKEN)).thenReturn(dadosOperacoes());
    when(eficiencia.calcular(any())).thenReturn(dadosEficiencia());

    for (String rota : List.of("/api/v1/aggregated-data", "/aggregated-data")) {
      mvc.perform(get(rota).header("Authorization", TOKEN))
          .andExpect(status().isOk())
          .andExpect(header().string("Cache-Control", "no-store"))
          .andExpect(jsonPath("$.fontes.frota").value("ok"))
          .andExpect(jsonPath("$.fontes.operacoes").value("ok"))
          .andExpect(jsonPath("$.fontes.funcao").value("ok"))
          .andExpect(jsonPath("$.frota.veiculos[0].placa").value("ABC1D23"))
          .andExpect(jsonPath("$.frota.motoristas[0].nome").value("Ana Souza"))
          .andExpect(jsonPath("$.frota.motoristas[0].cpf").doesNotExist())
          .andExpect(jsonPath("$.frota.vinculosAtivos[0].motoristaNome").value("Ana Souza"))
          // Últimos registros primeiro.
          .andExpect(jsonPath("$.operacoes.abastecimentos[0].quilometragem").value(10500))
          .andExpect(jsonPath("$.operacoes.manutencoes[0].tipo").value("PREVENTIVA"))
          .andExpect(jsonPath("$.eficiencia.frota.consumoMedioKmL").value(6.25))
          .andExpect(jsonPath("$.eficiencia.veiculos[0].gastoTotal").value(750.00))
          .andExpect(jsonPath("$.geradoEm").isNotEmpty());
    }

    var entrada = ArgumentCaptor.forClass(EficienciaRemota.Entrada.class);
    verify(eficiencia, times(2)).calcular(entrada.capture());
    var veiculo = entrada.getValue().veiculos().getFirst();
    assertThat(veiculo.veiculoId()).isEqualTo(VEICULO);
    assertThat(veiculo.placa()).isEqualTo("ABC1D23");
    assertThat(veiculo.abastecimentos())
        .extracting(EficienciaRemota.AbastecimentoCalculo::quilometragem)
        .containsExactlyInAnyOrder(10000L, 10500L);
    assertThat(veiculo.abastecimentos().getFirst().valor()).isNotNull();
    assertThat(veiculo.manutencoes())
        .containsExactly(new EficienciaRemota.ManutencaoCalculo(new BigDecimal("150.00")));
  }

  @Test
  void falhaDaFuncaoMantemFrotaEOperacoes(CapturedOutput saida) throws Exception {
    when(frota.consultar(TOKEN)).thenReturn(dadosFrota());
    when(operacoes.consultar(TOKEN)).thenReturn(dadosOperacoes());
    when(eficiencia.calcular(any())).thenThrow(new IllegalStateException("x-functions-key=segredo"));

    mvc.perform(
            get("/aggregated-data")
                .header("Authorization", TOKEN)
                .header("X-Correlation-ID", CORRELATION))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.fontes.frota").value("ok"))
        .andExpect(jsonPath("$.fontes.operacoes").value("ok"))
        .andExpect(jsonPath("$.fontes.funcao").value("falhou"))
        .andExpect(jsonPath("$.frota.veiculos.length()").value(1))
        .andExpect(jsonPath("$.operacoes.abastecimentos.length()").value(2))
        .andExpect(jsonPath("$.eficiencia.veiculos.length()").value(0));

    assertThat(saida.getAll())
        .contains("fonte=funcao")
        .contains("correlationId=" + CORRELATION)
        .doesNotContain("token-valido")
        .doesNotContain("segredo");
  }

  @Test
  void falhaDeOperacoesNaoChamaAFuncao(CapturedOutput saida) throws Exception {
    when(frota.consultar(TOKEN)).thenReturn(dadosFrota());
    when(operacoes.consultar(TOKEN)).thenThrow(new IllegalStateException("indisponivel"));

    mvc.perform(
            get("/api/v1/aggregated-data")
                .header("Authorization", TOKEN)
                .header("X-Correlation-ID", CORRELATION))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.fontes.frota").value("ok"))
        .andExpect(jsonPath("$.fontes.operacoes").value("falhou"))
        .andExpect(jsonPath("$.fontes.funcao").value("falhou"))
        .andExpect(jsonPath("$.frota.veiculos[0].placa").value("ABC1D23"))
        .andExpect(jsonPath("$.operacoes.abastecimentos.length()").value(0))
        .andExpect(jsonPath("$.operacoes.manutencoes.length()").value(0));

    verifyNoInteractions(eficiencia);
    assertThat(saida.getAll())
        .contains("fonte=operacoes")
        .contains("correlationId=" + CORRELATION)
        .doesNotContain("token-valido");
  }

  @Test
  void fonteLentaEstouraOTempoLimiteEMarcaFalha() throws Exception {
    when(frota.consultar(TOKEN))
        .thenAnswer(
            invocation -> {
              Thread.sleep(3000);
              return dadosFrota();
            });
    when(operacoes.consultar(TOKEN)).thenReturn(dadosOperacoes());
    when(eficiencia.calcular(any())).thenReturn(dadosEficiencia());

    long inicio = System.nanoTime();
    mvc.perform(get("/api/v1/aggregated-data").header("Authorization", TOKEN))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.fontes.frota").value("falhou"))
        .andExpect(jsonPath("$.fontes.operacoes").value("ok"))
        .andExpect(jsonPath("$.fontes.funcao").value("ok"))
        .andExpect(jsonPath("$.frota.veiculos.length()").value(0))
        .andExpect(jsonPath("$.frota.motoristas.length()").value(0))
        .andExpect(jsonPath("$.frota.vinculosAtivos.length()").value(0));
    assertThat(Duration.ofNanos(System.nanoTime() - inicio)).isLessThan(Duration.ofMillis(2500));
  }

  @Test
  void tresFontesFalhandoRespondem502() throws Exception {
    when(frota.consultar(TOKEN)).thenThrow(new IllegalStateException("frota"));
    when(operacoes.consultar(TOKEN)).thenThrow(new IllegalStateException("operacoes"));

    mvc.perform(get("/api/v1/aggregated-data").header("Authorization", TOKEN))
        .andExpect(status().isBadGateway())
        .andExpect(jsonPath("$.message").isNotEmpty());
  }

  @Test
  void semTokenValidoResponde401SemConsultarFontes() throws Exception {
    mvc.perform(get("/api/v1/aggregated-data")).andExpect(status().isUnauthorized());
    mvc.perform(get("/aggregated-data")).andExpect(status().isUnauthorized());
    mvc.perform(get("/aggregated-data").header("Authorization", "Bearer forjado"))
        .andExpect(status().isUnauthorized());

    verifyNoInteractions(frota, operacoes, eficiencia);
  }

  @Test
  void historicoTruncadoMarcaOperacoesOkComTruncadoEAvisaNoLog(CapturedOutput saida)
      throws Exception {
    var completo = dadosOperacoes();
    when(frota.consultar(TOKEN)).thenReturn(dadosFrota());
    when(operacoes.consultar(TOKEN))
        .thenReturn(
            new OperacoesAgregadasRemotas.Historico(
                completo.abastecimentos(), completo.manutencoes(), true));
    when(eficiencia.calcular(any())).thenReturn(dadosEficiencia());

    mvc.perform(
            get("/api/v1/aggregated-data")
                .header("Authorization", TOKEN)
                .header("X-Correlation-ID", CORRELATION))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.fontes.operacoes").value("ok"))
        .andExpect(jsonPath("$.operacoes.truncado").value(true))
        .andExpect(jsonPath("$.operacoes.abastecimentos.length()").value(2));

    assertThat(saida.getAll())
        .containsPattern("WARN .*correlationId=" + CORRELATION + ".*agregacao_operacoes_truncadas");
  }

  @Test
  void historicoCompletoInformaTruncadoFalso() throws Exception {
    when(frota.consultar(TOKEN)).thenReturn(dadosFrota());
    when(operacoes.consultar(TOKEN)).thenReturn(dadosOperacoes());
    when(eficiencia.calcular(any())).thenReturn(dadosEficiencia());

    mvc.perform(get("/api/v1/aggregated-data").header("Authorization", TOKEN))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.operacoes.truncado").value(false));
  }
}
