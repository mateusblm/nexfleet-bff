package br.com.nexfleet.bff.gerenciarabastecimentos.application;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

public interface AbastecimentosRemotos {
  record Dados(UUID id, UUID veiculoId, UUID motoristaId, Instant realizadoEm,
      String combustivel, BigDecimal litros, BigDecimal valorTotal,
      long quilometragem, Instant criadoEm, Integer versao) {
    /** Registro sem versão informada (Operações anterior à v1.1.0). */
    public Dados(UUID id, UUID veiculoId, UUID motoristaId, Instant realizadoEm,
        String combustivel, BigDecimal litros, BigDecimal valorTotal,
        long quilometragem, Instant criadoEm) {
      this(id, veiculoId, motoristaId, realizadoEm, combustivel, litros, valorTotal,
          quilometragem, criadoEm, null);
    }
  }
  record Entrada(UUID veiculoId, UUID motoristaId, Instant realizadoEm,
      String combustivel, BigDecimal litros, BigDecimal valorTotal, Long quilometragem) {}

  List<Dados> listar(String token, UUID veiculoId, int pagina, int tamanho);
  Dados registrar(String token, Entrada entrada);
}
