package br.com.nexfleet.bff;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/** O OpenAPI é público (lido pela Swagger UI do Gateway); as operações continuam exigindo JWT. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("jwt-rsa")
class OpenApiPublicoTest {
  @Autowired MockMvc http;

  @Test
  void openApiRespondeSemTokenComOsPathsDoServico() throws Exception {
    http.perform(get("/v3/api-docs"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.info.version").value("1.0.0"))
        .andExpect(jsonPath("$.servers[0].url").value("/"))
        .andExpect(jsonPath("$.components.securitySchemes['bearer-jwt'].scheme").value("bearer"))
        .andExpect(jsonPath("$.paths['/api/v1/veiculos']").exists())
        .andExpect(jsonPath("$.paths['/api/v1/aggregated-data']").exists())
        .andExpect(jsonPath("$.paths['/api/v1/auth/login']").exists());
  }

  @Test
  void operacaoDeNegocioContinuaRespondendo401SemToken() throws Exception {
    http.perform(get("/api/v1/veiculos")).andExpect(status().isUnauthorized());
  }
}
