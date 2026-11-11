package br.com.nexfleet.bff.gerenciarmanutencoes.application;

public class FalhaManutencoes extends RuntimeException {
  private final int status;
  public FalhaManutencoes(int status) { this(status, null); }
  /** Usa a mensagem do Operações quando houver; senão, a mensagem padrão do status. */
  public FalhaManutencoes(int status, String mensagem) {
    super(mensagem != null && !mensagem.isBlank() ? mensagem
        : status == 503 ? "Serviço de operações indisponível."
        : "Não foi possível processar a manutenção.");
    this.status = status;
  }
  public int status() { return status; }
}
