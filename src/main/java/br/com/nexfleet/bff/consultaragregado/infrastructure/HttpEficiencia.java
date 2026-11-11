package br.com.nexfleet.bff.consultaragregado.infrastructure;

import br.com.nexfleet.bff.consultaragregado.application.EficienciaRemota;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/** Chama a Function com a chave em {@code x-functions-key}; o JWT do usuário não é repassado. */
public class HttpEficiencia implements EficienciaRemota {
  private static final String PATH = "/api/calcular-eficiencia";
  private final RestClient client;
  private final String chave;

  public HttpEficiencia(RestClient client, String chave) {
    this.client = client;
    this.chave = chave;
  }

  /**
   * Monta o cliente a partir de {@code EFICIENCIA_FUNCTION_URL}. O valor esperado é só a base
   * ({@code https://<host>}); se já vier com {@code /api/calcular-eficiencia}, o caminho não é
   * duplicado.
   */
  public static HttpEficiencia comUrl(
      RestClient.Builder builder, String urlConfigurada, String chave) {
    return new HttpEficiencia(builder.baseUrl(base(urlConfigurada)).build(), chave);
  }

  static String base(String urlConfigurada) {
    var url = urlConfigurada.strip();
    while (url.endsWith("/")) url = url.substring(0, url.length() - 1);
    if (url.endsWith(PATH)) url = url.substring(0, url.length() - PATH.length());
    return url;
  }

  @Override
  public Eficiencia calcular(Entrada entrada) {
    Eficiencia resposta;
    try {
      resposta =
          client
              .post()
              .uri(PATH)
              .header("x-functions-key", chave)
              .body(entrada)
              .retrieve()
              .body(Eficiencia.class);
    } catch (RestClientException erro) {
      throw new FalhaEficiencia();
    }
    if (resposta == null
        || resposta.veiculos() == null
        || resposta.frota() == null
        || resposta.veiculos().stream().anyMatch(item -> item == null || item.veiculoId() == null))
      throw new FalhaEficiencia();
    return resposta;
  }

  static class FalhaEficiencia extends RuntimeException {
    FalhaEficiencia() {
      super("Function de eficiência indisponível.");
    }
  }
}
