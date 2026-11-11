package br.com.nexfleet.bff.gerenciarabastecimentos.api;

import br.com.nexfleet.bff.gerenciarabastecimentos.application.*;
import java.net.URI;
import java.util.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/abastecimentos")
public class AbastecimentosWebController {
  private final GerenciarAbastecimentosWeb abastecimentos;
  public AbastecimentosWebController(GerenciarAbastecimentosWeb abastecimentos) {
    this.abastecimentos = abastecimentos;
  }
  @GetMapping
  public ResponseEntity<List<AbastecimentosRemotos.Dados>> listar(
      @RequestHeader(value="Authorization", required=false) String token,
      @RequestParam(required=false) UUID veiculoId,
      @RequestParam(defaultValue="0") int pagina,
      @RequestParam(defaultValue="20") int tamanho) {
    return ResponseEntity.ok().cacheControl(CacheControl.noStore())
        .body(abastecimentos.listar(token, veiculoId, pagina, tamanho));
  }
  @PostMapping
  public ResponseEntity<AbastecimentosRemotos.Dados> registrar(
      @RequestHeader(value="Authorization", required=false) String token,
      @RequestBody AbastecimentosRemotos.Entrada entrada) {
    var novo = abastecimentos.registrar(token, entrada);
    return ResponseEntity.created(URI.create("/api/v1/abastecimentos/" + novo.id()))
        .cacheControl(CacheControl.noStore()).body(novo);
  }
  @GetMapping("/{id}")
  public ResponseEntity<AbastecimentosRemotos.Dados> consultar(
      @RequestHeader(value="Authorization", required=false) String token,
      @PathVariable UUID id) {
    return ResponseEntity.ok().cacheControl(CacheControl.noStore())
        .body(abastecimentos.consultar(token, id));
  }
  @PutMapping("/{id}")
  public ResponseEntity<AbastecimentosRemotos.Dados> atualizar(
      @RequestHeader(value="Authorization", required=false) String token,
      @PathVariable UUID id, @RequestBody AbastecimentoPorIdRemoto.Atualizacao atualizacao) {
    return ResponseEntity.ok().cacheControl(CacheControl.noStore())
        .body(abastecimentos.atualizar(token, id, atualizacao));
  }
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> excluir(
      @RequestHeader(value="Authorization", required=false) String token,
      @PathVariable UUID id) {
    abastecimentos.excluir(token, id);
    return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
  }
  @ExceptionHandler(FalhaAbastecimentos.class)
  public ResponseEntity<Map<String,String>> falha(FalhaAbastecimentos error) {
    return ResponseEntity.status(error.status()).cacheControl(CacheControl.noStore())
        .body(Map.of("message", error.getMessage()));
  }
}
