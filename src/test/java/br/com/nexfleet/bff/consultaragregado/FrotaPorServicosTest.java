package br.com.nexfleet.bff.consultaragregado;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import br.com.nexfleet.bff.consultaragregado.infrastructure.FrotaPorServicos;
import br.com.nexfleet.bff.gerenciarmotoristas.application.MotoristasRemotos;
import br.com.nexfleet.bff.gerenciarveiculos.application.VeiculosRemotos;
import br.com.nexfleet.bff.gerenciarvinculos.application.VinculosRemotos;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class FrotaPorServicosTest {
  @Test
  void trazSoVinculosAtivosEPercorreTodasAsPaginas() {
    var veiculos = mock(VeiculosRemotos.class);
    var motoristas = mock(MotoristasRemotos.class);
    var vinculos = mock(VinculosRemotos.class);
    var ativo = UUID.randomUUID();
    var pagina0 =
        java.util.stream.IntStream.range(0, 100)
            .mapToObj(i -> vinculo(UUID.randomUUID(), Instant.parse("2026-10-02T00:00:00Z")))
            .toList();
    when(vinculos.listar("Bearer t", null, 0, 100)).thenReturn(pagina0);
    when(vinculos.listar("Bearer t", null, 1, 100)).thenReturn(List.of(vinculo(ativo, null)));
    when(veiculos.listar("Bearer t", null, 0, 100)).thenReturn(List.of());
    when(motoristas.listar("Bearer t", null, 0, 100)).thenReturn(List.of());

    var frota = new FrotaPorServicos(veiculos, motoristas, vinculos).consultar("Bearer t");

    assertEquals(1, frota.vinculosAtivos().size());
    assertEquals(ativo, frota.vinculosAtivos().getFirst().id());
    verify(vinculos, never()).listar("Bearer t", null, 2, 100);
  }

  private VinculosRemotos.Dados vinculo(UUID id, Instant fim) {
    var agora = Instant.parse("2026-10-01T00:00:00Z");
    return new VinculosRemotos.Dados(
        id, UUID.randomUUID(), "ABC1D23", UUID.randomUUID(), "Ana", agora, fim, agora, agora);
  }
}
