package br.com.nexfleet.bff.consultaragregado.application;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Visão de Frota usada na agregação; omite CPF, CNH e telefone dos motoristas. */
public interface FrotaAgregadaRemota {
  record Veiculo(
      UUID id,
      String placa,
      String marca,
      String modelo,
      int ano,
      String tipo,
      long quilometragem,
      String status) {}

  record Motorista(UUID id, String nome, String categoria, LocalDate validadeCnh, String status) {}

  record VinculoAtivo(
      UUID id,
      UUID veiculoId,
      String veiculoPlaca,
      UUID motoristaId,
      String motoristaNome,
      Instant inicio) {}

  record Frota(List<Veiculo> veiculos, List<Motorista> motoristas, List<VinculoAtivo> vinculosAtivos) {
    public static Frota vazia() {
      return new Frota(List.of(), List.of(), List.of());
    }
  }

  Frota consultar(String token);
}
