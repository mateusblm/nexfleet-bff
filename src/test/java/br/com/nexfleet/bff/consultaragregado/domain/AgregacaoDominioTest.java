package br.com.nexfleet.bff.consultaragregado.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.function.Function;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class AgregacaoDominioTest {
  @Test
  void fonteQueRespondeuFicaOkEAsDemaisFalharam() {
    assertEquals(new Fontes("ok", "falhou", "falhou"), Fontes.de(true, false, false));
    assertFalse(Fontes.de(false, false, true).nenhumaRespondeu());
    assertTrue(Fontes.de(false, false, false).nenhumaRespondeu());
  }

  @Test
  void mantemOsDezMaisRecentesDoMaisNovoParaOMaisAntigo() {
    var datas = IntStream.rangeClosed(1, 15).boxed().toList();
    assertEquals(
        List.of(15, 14, 13, 12, 11, 10, 9, 8, 7, 6),
        UltimosRegistros.maisRecentes(datas, Function.identity()));
    assertEquals(List.of(3, 1), UltimosRegistros.maisRecentes(List.of(1, 3), Function.identity()));
  }
}
