package br.com.nexfleet.bff;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.test.context.*;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("jwt-rsa")
class AbastecimentoPorIdHttpTest {
  static final OperacoesFalso operacoes = new OperacoesFalso();
  static final String TOKEN = "Bearer token-valido";
  static final String CORRELATION = "7d1f0a52-4c1e-4a43-9a51-2f5a3a3e9b10";
  static final String ID = "1d4be0d1-4801-41e1-9c5f-9002b2fc6dcc";
  static final String DADOS = "{\"id\":\"" + ID + "\","
      + "\"veiculoId\":\"b176f1b1-bc54-4f22-9b2e-fb69469ab033\","
      + "\"motoristaId\":\"3ce0bdb7-f1de-47c2-baa3-7b99e343650c\","
      + "\"realizadoEm\":\"2026-11-05T14:00:00Z\",\"combustivel\":\"ETANOL\","
      + "\"litros\":40.00,\"valorTotal\":210.50,\"quilometragem\":13000,"
      + "\"criadoEm\":\"2026-11-05T15:00:00Z\",\"versao\":3}";

  @Autowired MockMvc mvc;
  @MockitoBean JwtDecoder jwtDecoder;

  @DynamicPropertySource
  static void propriedades(DynamicPropertyRegistry registry) {
    registry.add("services.operacoes.url", operacoes::url);
  }

  @BeforeEach
  void autenticacao() {
    when(jwtDecoder.decode(anyString())).thenThrow(new BadJwtException("invalido"));
    doReturn(Jwt.withTokenValue("token-valido").header("alg", "RS256")
            .subject("e2f6b588-00cf-45bc-927e-c4a08647dfde")
            .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(600)).build())
        .when(jwtDecoder).decode("token-valido");
  }

  @Test
  void consultaPorIdRepassandoBearerECorrelationId() throws Exception {
    operacoes.responde(200, DADOS);
    mvc.perform(get("/api/v1/abastecimentos/" + ID)
            .header("Authorization", TOKEN).header("X-Correlation-ID", CORRELATION))
        .andExpect(status().isOk())
        .andExpect(header().string("Cache-Control", "no-store"))
        .andExpect(jsonPath("$.id").value(ID))
        .andExpect(jsonPath("$.combustivel").value("ETANOL"))
        .andExpect(jsonPath("$.quilometragem").value(13000))
        .andExpect(jsonPath("$.versao").value(3));
    var chamada = operacoes.ultima();
    assertEquals("GET", chamada.metodo());
    assertEquals("/api/v1/operacoes/abastecimentos/" + ID, chamada.caminho());
    assertEquals(TOKEN, chamada.authorization());
    assertEquals(CORRELATION, chamada.correlationId());
  }

  @Test
  void editaRepassandoSomenteOsCamposEditaveis() throws Exception {
    operacoes.responde(200, DADOS);
    mvc.perform(put("/api/v1/abastecimentos/" + ID)
            .header("Authorization", TOKEN).header("X-Correlation-ID", CORRELATION)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"combustivel\":\"ETANOL\",\"litros\":40.00,\"valorTotal\":210.50,"
                + "\"quilometragem\":13000,"
                + "\"veiculoId\":\"00000000-0000-0000-0000-000000000099\","
                + "\"realizadoEm\":\"2020-01-01T00:00:00Z\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.versao").value(3))
        .andExpect(jsonPath("$.valorTotal").value(210.50));
    var chamada = operacoes.ultima();
    assertEquals("PUT", chamada.metodo());
    assertEquals("/api/v1/operacoes/abastecimentos/" + ID, chamada.caminho());
    assertEquals(TOKEN, chamada.authorization());
    assertEquals(CORRELATION, chamada.correlationId());
    assertEquals("{\"combustivel\":\"ETANOL\",\"litros\":40.00,\"valorTotal\":210.50,"
        + "\"quilometragem\":13000}", chamada.corpo());
  }

  @Test
  void excluiERespondeSemCorpo() throws Exception {
    operacoes.responde(204, "");
    mvc.perform(delete("/api/v1/abastecimentos/" + ID)
            .header("Authorization", TOKEN).header("X-Correlation-ID", CORRELATION))
        .andExpect(status().isNoContent())
        .andExpect(content().string(""));
    var chamada = operacoes.ultima();
    assertEquals("DELETE", chamada.metodo());
    assertEquals("/api/v1/operacoes/abastecimentos/" + ID, chamada.caminho());
    assertEquals(TOKEN, chamada.authorization());
    assertEquals(CORRELATION, chamada.correlationId());
  }

  @Test
  void repassaStatusEMensagemDoOperacoes() throws Exception {
    for (int codigo : new int[] {403, 404, 422}) {
      operacoes.responde(codigo, "{\"message\":\"Mensagem do Operações " + codigo + ".\"}");
      mvc.perform(put("/api/v1/abastecimentos/" + ID).header("Authorization", TOKEN)
              .contentType(MediaType.APPLICATION_JSON)
              .content("{\"combustivel\":\"ETANOL\",\"litros\":40.00,\"valorTotal\":210.50,"
                  + "\"quilometragem\":13000}"))
          .andExpect(status().is(codigo))
          .andExpect(jsonPath("$.message").value("Mensagem do Operações " + codigo + "."));
      mvc.perform(get("/api/v1/abastecimentos/" + ID).header("Authorization", TOKEN))
          .andExpect(status().is(codigo))
          .andExpect(jsonPath("$.message").value("Mensagem do Operações " + codigo + "."));
      mvc.perform(delete("/api/v1/abastecimentos/" + ID).header("Authorization", TOKEN))
          .andExpect(status().is(codigo))
          .andExpect(jsonPath("$.message").value("Mensagem do Operações " + codigo + "."));
    }
  }

  @Test
  void falhaDoOperacoesViraServicoIndisponivel() throws Exception {
    operacoes.responde(500, "{\"message\":\"detalhe interno\"}");
    mvc.perform(get("/api/v1/abastecimentos/" + ID).header("Authorization", TOKEN))
        .andExpect(status().isServiceUnavailable())
        .andExpect(jsonPath("$.message").value("Serviço Operações indisponível."));
  }

  @Test
  void semTokenRespondeNaoAutorizadoSemChamarOOperacoes() throws Exception {
    operacoes.responde(200, DADOS);
    mvc.perform(get("/api/v1/abastecimentos/" + ID)).andExpect(status().isUnauthorized());
    mvc.perform(delete("/api/v1/abastecimentos/" + ID)).andExpect(status().isUnauthorized());
    assertNull(operacoes.ultima());
  }
}
