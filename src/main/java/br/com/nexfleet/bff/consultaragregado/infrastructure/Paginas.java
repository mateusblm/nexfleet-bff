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

  private Paginas() {}

  static <T> List<T> todas(Pagina<T> fonte) {
    var todos = new ArrayList<T>();
    for (int pagina = 0; pagina < LIMITE_PAGINAS; pagina++) {
      var itens = fonte.buscar(pagina, TAMANHO);
      todos.addAll(itens);
      if (itens.size() < TAMANHO) break;
    }
    return List.copyOf(todos);
  }
}
