package br.com.nexfleet.bff.consultaragregado.domain;

/**
 * Situação de cada fonte da visão consolidada: {@value #OK} se respondeu, {@value #FALHOU} caso
 * contrário. A visão só falha quando nenhuma fonte respondeu.
 */
public record Fontes(String frota, String operacoes, String funcao) {
  public static final String OK = "ok";
  public static final String FALHOU = "falhou";

  public static Fontes de(
      boolean frotaRespondeu, boolean operacoesRespondeu, boolean funcaoRespondeu) {
    return new Fontes(
        situacao(frotaRespondeu), situacao(operacoesRespondeu), situacao(funcaoRespondeu));
  }

  public boolean nenhumaRespondeu() {
    return !OK.equals(frota) && !OK.equals(operacoes) && !OK.equals(funcao);
  }

  private static String situacao(boolean respondeu) {
    return respondeu ? OK : FALHOU;
  }
}
