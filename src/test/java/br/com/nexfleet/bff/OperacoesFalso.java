package br.com.nexfleet.bff;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

/** Operações falso: guarda a última chamada recebida e responde o status e corpo combinados. */
final class OperacoesFalso {
  record Chamada(String metodo, String caminho, String authorization, String correlationId,
      String corpo) {}

  private final HttpServer server;
  private volatile Chamada ultima;
  private volatile int status = 200;
  private volatile String corpo = "";

  OperacoesFalso() {
    try {
      server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
    server.createContext("/", exchange -> {
      var headers = exchange.getRequestHeaders();
      ultima = new Chamada(exchange.getRequestMethod(), exchange.getRequestURI().getPath(),
          headers.getFirst("Authorization"), headers.getFirst("X-Correlation-ID"),
          new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
      byte[] bytes = corpo.getBytes(StandardCharsets.UTF_8);
      exchange.getResponseHeaders().add("Content-Type", "application/json");
      exchange.sendResponseHeaders(status, bytes.length == 0 ? -1 : bytes.length);
      try (var saida = exchange.getResponseBody()) {
        saida.write(bytes);
      }
    });
    server.start();
  }

  String url() { return "http://127.0.0.1:" + server.getAddress().getPort(); }

  void responde(int status, String corpo) {
    this.ultima = null;
    this.status = status;
    this.corpo = corpo;
  }

  Chamada ultima() { return ultima; }
}
