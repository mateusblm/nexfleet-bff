package br.com.nexfleet.bff.gerenciarabastecimentos.application;

public class FalhaAbastecimentos extends RuntimeException {
  private final int status;
  public FalhaAbastecimentos(int status) { this(status, null); }
  /** Usa a mensagem do Operações quando houver; senão, a mensagem padrão do status. */
  public FalhaAbastecimentos(int status, String mensagem) {
    super(mensagem == null || mensagem.isBlank() ? mensagem(status) : mensagem);
    this.status = status;
  }
  public int status() { return status; }
  private static String mensagem(int status) {
    return switch (status) {
      case 401 -> "Sessão ausente ou inválida.";
      case 403 -> "Seu perfil não pode acessar Operações.";
      case 404 -> "Abastecimento não encontrado.";
      case 409 -> "O abastecimento conflita com outro registro.";
      case 400, 422 -> "Dados inválidos, vínculo ausente ou quilometragem regressiva.";
      default -> "Serviço Operações indisponível.";
    };
  }
}
