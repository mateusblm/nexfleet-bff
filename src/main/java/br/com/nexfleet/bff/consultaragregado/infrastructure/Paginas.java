package br.com.nexfleet.bff.consultaragregado.infrastructure;

import java.util.ArrayList;
import java.util.List;

/** Percorre listagens paginadas (tamanho máximo 100 nos contratos) com teto de segurança. */
final class Paginas {
  static final int TAMANHO = 100;
  static final int LIMITE_PAGINAS = 10;

  interface Pagina<T> {
    List<T> buscar(int pagina, int tamanho);
  }

  /** Itens lidos até o teto; {@code truncado} indica que a fonte tinha mais. */
  record Lote<T>(List<T> itens, boolean truncado) {}

  private Paginas() {}

  static <T> List<T> todas(Pagina<T> fonte) {
    return ate(fonte, LIMITE_PAGINAS).itens();
  }

  static <T> Lote<T> ate(Pagina<T> fonte, int limitePaginas) {
    var todos = new ArrayList<T>();
    for (int pagina = 0; pagina < limitePaginas; pagina++) {
      var itens = fonte.buscar(pagina, TAMANHO);
      todos.addAll(itens);
      if (itens.size() < TAMANHO) return new Lote<>(List.copyOf(todos), false);
    }
    // Teto atingido com a última página cheia: uma consulta a mais diz se havia mais itens.
    boolean truncado = !fonte.buscar(limitePaginas, TAMANHO).isEmpty();
    return new Lote<>(List.copyOf(todos), truncado);
  }
}
