package br.com.nexfleet.bff.consultaragregado;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

import br.com.nexfleet.bff.consultaragregado.application.EficienciaRemota;
import br.com.nexfleet.bff.consultaragregado.infrastructure.HttpEficiencia;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.*;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class HttpEficienciaTest {
  final RestClient.Builder builder = RestClient.builder().baseUrl("http://funcao");
  final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
  final HttpEficiencia client = new HttpEficiencia(builder.build(), "chave-da-funcao");
  final UUID veiculo = UUID.fromString("00000000-0000-0000-0000-00000000000a");

  final EficienciaRemota.Entrada entrada =
      new EficienciaRemota.Entrada(
          List.of(
              new EficienciaRemota.VeiculoHistorico(
                  veiculo,
                  "ABC1D23",
                  List.of(
                      new EficienciaRemota.AbastecimentoCalculo(
                          Instant.parse("2026-10-01T10:00:00Z"),
                          new BigDecimal("40"),
                          new BigDecimal("200.00"),
                          10000)),
                  List.of(new EficienciaRemota.ManutencaoCalculo(new BigDecimal("99.25"))))));

  @Test
  void enviaChaveDaFuncaoEHistoricoEMapeiaResposta() {
    server
        .expect(requestTo("http://funcao/api/calcular-eficiencia"))
        .andExpect(method(HttpMethod.POST))
        .andExpect(header("x-functions-key", "chave-da-funcao"))
        .andExpect(headerDoesNotExist("Authorization"))
        .andExpect(
            content()
                .json(
                    """
                    {"veiculos":[{"veiculoId":"00000000-0000-0000-0000-00000000000a",
                      "placa":"ABC1D23",
                      "abastecimentos":[{"data":"2026-10-01T10:00:00Z","litros":40,
                        "valor":200.00,"quilometragem":10000}],
                      "manutencoes":[{"valor":99.25}]}]}
                    """,
                    org.springframework.test.json.JsonCompareMode.STRICT))
        .andRespond(
            withSuccess(
                """
                {"veiculos":[{"veiculoId":"00000000-0000-0000-0000-00000000000a",
                  "placa":"ABC1D23","consumoMedioKmL":null,"custoPorKm":null,
                  "gastoAbastecimento":200.00,"gastoManutencao":99.25,"gastoTotal":299.25,
                  "distanciaKm":0}],
                 "frota":{"consumoMedioKmL":6.25,"custoPorKm":1.50,"gastoTotal":299.25,
                  "distanciaKm":500}}
                """,
                MediaType.APPLICATION_JSON));

    var resposta = client.calcular(entrada);

    server.verify();
    var item = resposta.veiculos().getFirst();
    assertEquals(veiculo, item.veiculoId());
    assertEquals("ABC1D23", item.placa());
    assertNull(item.consumoMedioKmL());
    assertNull(item.custoPorKm());
    assertEquals(new BigDecimal("299.25"), item.gastoTotal());
    assertEquals(new BigDecimal("99.25"), item.gastoManutencao());
    assertEquals(0, item.distanciaKm());
    assertEquals(new BigDecimal("6.25"), resposta.frota().consumoMedioKmL());
    assertEquals(500, resposta.frota().distanciaKm());
  }

  @Test
  void erroDaFuncaoViraFalhaSemVazarCorpo() {
    server.expect(anything()).andRespond(withStatus(HttpStatus.UNAUTHORIZED).body("chave errada"));
    var falha = assertThrows(RuntimeException.class, () -> client.calcular(entrada));
    assertFalse(falha.getMessage().contains("chave"));
  }

  @Test
  void respostaSemCamposObrigatoriosViraFalha() {
    server.expect(anything()).andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
    assertThrows(RuntimeException.class, () -> client.calcular(entrada));
  }
}
