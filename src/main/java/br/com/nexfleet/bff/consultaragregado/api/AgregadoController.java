package br.com.nexfleet.bff.consultaragregado.api;

import br.com.nexfleet.bff.consultaragregado.application.*;
import java.util.Map;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
public class AgregadoController {
  private final ConsultarAgregado agregado;

  public AgregadoController(ConsultarAgregado agregado) {
    this.agregado = agregado;
  }

  @GetMapping({"/api/v1/aggregated-data", "/aggregated-data"})
  ResponseEntity<ConsultarAgregado.Resultado> consultar(
      @RequestHeader(value = "Authorization", required = false) String token) {
    return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(agregado.consultar(token));
  }

  @ExceptionHandler(FalhaAgregado.class)
  ResponseEntity<Map<String, String>> falha(FalhaAgregado error) {
    return ResponseEntity.status(error.status())
        .cacheControl(CacheControl.noStore())
        .body(Map.of("message", error.getMessage()));
  }
}
