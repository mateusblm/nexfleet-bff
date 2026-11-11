package br.com.nexfleet.bff.consultaragregado.domain;

import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

/** A visão consolidada mostra só os registros mais recentes de cada histórico. */
public final class UltimosRegistros {
  public static final int QUANTIDADE = 10;

  private UltimosRegistros() {}

  /** Os {@value #QUANTIDADE} registros de data mais recente, do mais novo para o mais antigo. */
  public static <T, D extends Comparable<? super D>> List<T> maisRecentes(
      List<T> registros, Function<? super T, ? extends D> data) {
    return registros.stream()
        .sorted(Comparator.<T, D>comparing(data).reversed())
        .limit(QUANTIDADE)
        .toList();
  }
}
