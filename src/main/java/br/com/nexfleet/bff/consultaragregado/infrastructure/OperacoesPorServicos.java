package br.com.nexfleet.bff.consultaragregado.infrastructure;

import br.com.nexfleet.bff.consultaragregado.application.OperacoesAgregadasRemotas;
import br.com.nexfleet.bff.gerenciarabastecimentos.application.AbastecimentosRemotos;
import br.com.nexfleet.bff.gerenciarmanutencoes.application.ManutencoesRemotas;

/** Reaproveita os clientes de histórico de Operações; o BFF não acessa o banco do serviço. */
public class OperacoesPorServicos implements OperacoesAgregadasRemotas {
  private final AbastecimentosRemotos abastecimentos;
  private final ManutencoesRemotas manutencoes;

  public OperacoesPorServicos(AbastecimentosRemotos abastecimentos, ManutencoesRemotas manutencoes) {
    this.abastecimentos = abastecimentos;
    this.manutencoes = manutencoes;
  }

  @Override
  public Historico consultar(String token) {
    return new Historico(
        Paginas.todas((pagina, tamanho) -> abastecimentos.listar(token, null, pagina, tamanho))
            .stream()
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
        Paginas.todas((pagina, tamanho) -> manutencoes.listar(token, null, pagina, tamanho))
            .stream()
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
            .toList());
  }
}
