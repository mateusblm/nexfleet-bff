package br.com.nexfleet.bff.consultaragregado.infrastructure;

import br.com.nexfleet.bff.consultaragregado.application.OperacoesAgregadasRemotas;
import br.com.nexfleet.bff.gerenciarabastecimentos.application.AbastecimentosRemotos;
import br.com.nexfleet.bff.gerenciarmanutencoes.application.ManutencoesRemotas;

/**
 * Reaproveita os clientes de histórico de Operações; o BFF não acessa o banco do serviço. Lê até
 * {@link #LIMITE_POR_HISTORICO} abastecimentos e o mesmo de manutenções; acima disso corta e marca
 * o histórico como truncado.
 */
public class OperacoesPorServicos implements OperacoesAgregadasRemotas {
  public static final int LIMITE_POR_HISTORICO = 4000;
  private static final int LIMITE_PAGINAS = LIMITE_POR_HISTORICO / Paginas.TAMANHO;

  private final AbastecimentosRemotos abastecimentos;
  private final ManutencoesRemotas manutencoes;

  public OperacoesPorServicos(AbastecimentosRemotos abastecimentos, ManutencoesRemotas manutencoes) {
    this.abastecimentos = abastecimentos;
    this.manutencoes = manutencoes;
  }

  @Override
  public Historico consultar(String token) {
    var loteAbastecimentos =
        Paginas.ate(
            (pagina, tamanho) -> abastecimentos.listar(token, null, pagina, tamanho),
            LIMITE_PAGINAS);
    var loteManutencoes =
        Paginas.ate(
            (pagina, tamanho) -> manutencoes.listar(token, null, pagina, tamanho), LIMITE_PAGINAS);
    return new Historico(
        loteAbastecimentos.itens().stream()
            .map(
                a ->
                    new Abastecimento(
                        a.id(),
                        a.veiculoId(),
                        a.realizadoEm(),
                        a.combustivel(),
                        a.litros(),
                        a.valorTotal(),
                        a.quilometragem()))
            .toList(),
        loteManutencoes.itens().stream()
            .map(
                m ->
                    new Manutencao(
                        m.id(),
                        m.veiculoId(),
                        m.tipo(),
                        m.realizadaEm(),
                        m.quilometragem(),
                        m.valor(),
                        m.proximaManutencao()))
            .toList(),
        loteAbastecimentos.truncado() || loteManutencoes.truncado());
  }
}
