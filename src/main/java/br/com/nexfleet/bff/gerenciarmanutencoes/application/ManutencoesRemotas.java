package br.com.nexfleet.bff.gerenciarmanutencoes.application;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;

public interface ManutencoesRemotas {
  record Dados(UUID id, UUID veiculoId, String tipo, LocalDate realizadaEm,
      long quilometragem, String descricao, String oficina, BigDecimal valor,
      LocalDate proximaManutencao, Instant criadoEm, Integer versao) {
    /** Registro sem versão informada (Operações anterior à v1.1.0). */
    public Dados(UUID id, UUID veiculoId, String tipo, LocalDate realizadaEm,
        long quilometragem, String descricao, String oficina, BigDecimal valor,
        LocalDate proximaManutencao, Instant criadoEm) {
      this(id, veiculoId, tipo, realizadaEm, quilometragem, descricao, oficina, valor,
          proximaManutencao, criadoEm, null);
    }
  }
  record Entrada(UUID veiculoId, String tipo, LocalDate realizadaEm, Long quilometragem,
      String descricao, String oficina, BigDecimal valor, LocalDate proximaManutencao) {}

  List<Dados> listar(String token, UUID veiculoId, int pagina, int tamanho);
  Dados registrar(String token, Entrada entrada);
}
