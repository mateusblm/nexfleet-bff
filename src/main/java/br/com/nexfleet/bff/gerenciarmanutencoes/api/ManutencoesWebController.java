package br.com.nexfleet.bff.gerenciarmanutencoes.api;

import br.com.nexfleet.bff.gerenciarmanutencoes.application.*;
import java.net.URI;
import java.util.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/manutencoes")
public class ManutencoesWebController {
  private final GerenciarManutencoesWeb manutencoes;
  public ManutencoesWebController(GerenciarManutencoesWeb manutencoes) { this.manutencoes = manutencoes; }

  @GetMapping
  public ResponseEntity<List<ManutencoesRemotas.Dados>> listar(
      @RequestHeader(value = "Authorization", required = false) String token,
      @RequestParam(required = false) UUID veiculoId,
      @RequestParam(defaultValue = "0") int pagina,
      @RequestParam(defaultValue = "20") int tamanho) {
    return ResponseEntity.ok().cacheControl(CacheControl.noStore())
        .body(manutencoes.listar(token, veiculoId, pagina, tamanho));
  }

  @PostMapping
  public ResponseEntity<ManutencoesRemotas.Dados> registrar(
      @RequestHeader(value = "Authorization", required = false) String token,
      @RequestBody ManutencoesRemotas.Entrada entrada) {
    var novo = manutencoes.registrar(token, entrada);
    return ResponseEntity.created(URI.create("/api/v1/manutencoes/" + novo.id()))
        .cacheControl(CacheControl.noStore()).body(novo);
  }

  @GetMapping("/{id}")
  public ResponseEntity<ManutencoesRemotas.Dados> consultar(
      @RequestHeader(value = "Authorization", required = false) String token,
      @PathVariable UUID id) {
    return ResponseEntity.ok().cacheControl(CacheControl.noStore())
        .body(manutencoes.consultar(token, id));
  }

  @PutMapping("/{id}")
  public ResponseEntity<ManutencoesRemotas.Dados> atualizar(
      @RequestHeader(value = "Authorization", required = false) String token,
      @PathVariable UUID id, @RequestBody ManutencaoPorIdRemota.Atualizacao atualizacao) {
    return ResponseEntity.ok().cacheControl(CacheControl.noStore())
        .body(manutencoes.atualizar(token, id, atualizacao));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> excluir(
      @RequestHeader(value = "Authorization", required = false) String token,
      @PathVariable UUID id) {
    manutencoes.excluir(token, id);
    return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
  }

  @ExceptionHandler(FalhaManutencoes.class)
  public ResponseEntity<Map<String, String>> falha(FalhaManutencoes erro) {
    return ResponseEntity.status(erro.status()).cacheControl(CacheControl.noStore())
        .body(Map.of("message", erro.getMessage()));
  }
}
