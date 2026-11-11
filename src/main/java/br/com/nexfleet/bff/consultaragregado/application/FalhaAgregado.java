package br.com.nexfleet.bff.consultaragregado.application;

public class FalhaAgregado extends RuntimeException {
  private final int status;

  public FalhaAgregado(int status) {
    super(
        status == 401
            ? "Autenticação necessária."
            : "Nenhuma fonte da visão consolidada respondeu.");
    this.status = status;
  }

  public int status() {
    return status;
  }
}
