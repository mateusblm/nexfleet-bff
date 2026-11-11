package br.com.nexfleet.bff.consultaragregado.infrastructure;

import br.com.nexfleet.bff.consultaragregado.application.FrotaAgregadaRemota;
import br.com.nexfleet.bff.gerenciarmotoristas.application.MotoristasRemotos;
import br.com.nexfleet.bff.gerenciarveiculos.application.VeiculosRemotos;
import br.com.nexfleet.bff.gerenciarvinculos.application.VinculosRemotos;

/** Reaproveita os clientes de listagem de Frota já usados pelas telas de cadastro. */
public class FrotaPorServicos implements FrotaAgregadaRemota {
  private final VeiculosRemotos veiculos;
  private final MotoristasRemotos motoristas;
  private final VinculosRemotos vinculos;

  public FrotaPorServicos(
      VeiculosRemotos veiculos, MotoristasRemotos motoristas, VinculosRemotos vinculos) {
    this.veiculos = veiculos;
    this.motoristas = motoristas;
    this.vinculos = vinculos;
  }

  @Override
  public Frota consultar(String token) {
    return new Frota(
        Paginas.todas((pagina, tamanho) -> veiculos.listar(token, null, pagina, tamanho)).stream()
            .map(
                v ->
                    new Veiculo(
                        v.id(),
                        v.placa(),
                        v.marca(),
                        v.modelo(),
                        v.ano(),
                        v.tipo(),
                        v.quilometragem(),
                        v.status()))
            .toList(),
        Paginas.todas((pagina, tamanho) -> motoristas.listar(token, null, pagina, tamanho))
            .stream()
            .map(m -> new Motorista(m.id(), m.nome(), m.categoria(), m.validadeCnh(), m.status()))
            .toList(),
        Paginas.todas((pagina, tamanho) -> vinculos.listar(token, null, pagina, tamanho)).stream()
            .filter(v -> v.fim() == null)
            .map(
                v ->
                    new VinculoAtivo(
                        v.id(),
                        v.veiculoId(),
                        v.veiculoPlaca(),
                        v.motoristaId(),
                        v.motoristaNome(),
                        v.inicio()))
            .toList());
  }
}
