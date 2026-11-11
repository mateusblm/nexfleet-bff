package br.com.nexfleet.bff.consultaragregado.application;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Compõe Frota, Operações e a Function de eficiência (spec 13–14, ADR 0018).
 *
 * <p>Frota e Operações são chamadas em paralelo, cada uma com o próprio tempo limite. A Function
 * depende do histórico de Operações: se Operações falhar, ela não é chamada e {@code
 * fontes.funcao} vem "falhou". A placa enviada à Function vem de Frota quando disponível.
 */
public class ConsultarAgregado {
  public static final int ULTIMOS_REGISTROS = 10;
  private static final Logger LOG = LoggerFactory.getLogger(ConsultarAgregado.class);
  private static final String OK = "ok";
  private static final String FALHOU = "falhou";

  private final FrotaAgregadaRemota frota;
  private final OperacoesAgregadasRemotas operacoes;
  private final EficienciaRemota eficiencia;
  private final Executor executor;
  private final Duration timeout;
  private final Clock clock;

  public ConsultarAgregado(
      FrotaAgregadaRemota frota,
      OperacoesAgregadasRemotas operacoes,
      EficienciaRemota eficiencia,
      Executor executor,
      Duration timeout,
      Clock clock) {
    this.frota = frota;
    this.operacoes = operacoes;
    this.eficiencia = eficiencia;
    this.executor = executor;
    this.timeout = timeout;
    this.clock = clock;
  }

  public Resultado consultar(String token) {
    String bearer = bearer(token);
    var frotaFutura = chamar(() -> frota.consultar(bearer));
    var operacoesFutura = chamar(() -> operacoes.consultar(bearer));
    var placas =
        frotaFutura.handle(
            (dados, erro) ->
                dados == null
                    ? Map.<UUID, String>of()
                    : dados.veiculos().stream()
                        .collect(
                            Collectors.toMap(
                                FrotaAgregadaRemota.Veiculo::id,
                                FrotaAgregadaRemota.Veiculo::placa,
                                (a, b) -> a)));
    var eficienciaFutura =
        operacoesFutura
            .thenCombine(placas, ConsultarAgregado::entrada)
            .thenCompose(entrada -> chamar(() -> eficiencia.calcular(entrada)));

    var dadosFrota = aguardar("frota", frotaFutura);
    var dadosOperacoes = aguardar("operacoes", operacoesFutura);
    var dadosEficiencia =
        dadosOperacoes.isEmpty()
            ? Optional.<EficienciaRemota.Eficiencia>empty()
            : aguardar("funcao", eficienciaFutura);
    if (dadosOperacoes.isEmpty())
      LOG.warn("agregacao_fonte_falhou fonte=funcao motivo=operacoes_indisponivel");

    if (dadosFrota.isEmpty() && dadosOperacoes.isEmpty() && dadosEficiencia.isEmpty())
      throw new FalhaAgregado(502);

    return new Resultado(
        dadosFrota.orElse(FrotaAgregadaRemota.Frota.vazia()),
        dadosOperacoes.map(ConsultarAgregado::ultimos).orElse(Operacoes.vazia()),
        dadosEficiencia.orElse(EficienciaRemota.Eficiencia.vazia()),
        new Fontes(
            situacao(dadosFrota), situacao(dadosOperacoes), situacao(dadosEficiencia)),
        Instant.now(clock));
  }

  private <T> CompletableFuture<T> chamar(Supplier<T> chamada) {
    return CompletableFuture.supplyAsync(chamada, executor)
        .orTimeout(timeout.toMillis(), TimeUnit.MILLISECONDS);
  }

  private static <T> Optional<T> aguardar(String fonte, CompletableFuture<T> futura) {
    try {
      return Optional.ofNullable(futura.join());
    } catch (CompletionException | CancellationException erro) {
      // Só o tipo do erro: a mensagem pode carregar dados da resposta, token ou chave.
      Throwable causa = erro.getCause() == null ? erro : erro.getCause();
      String motivo =
          causa instanceof TimeoutException ? "timeout" : causa.getClass().getSimpleName();
      LOG.warn("agregacao_fonte_falhou fonte={} motivo={}", fonte, motivo);
      return Optional.empty();
    }
  }

  private static String situacao(Optional<?> dados) {
    return dados.isPresent() ? OK : FALHOU;
  }

  static EficienciaRemota.Entrada entrada(
      OperacoesAgregadasRemotas.Historico historico, Map<UUID, String> placas) {
    var abastecimentos = new LinkedHashMap<UUID, List<EficienciaRemota.AbastecimentoCalculo>>();
    var manutencoes = new LinkedHashMap<UUID, List<EficienciaRemota.ManutencaoCalculo>>();
    var veiculos = new LinkedHashSet<UUID>();
    for (var item : historico.abastecimentos()) {
      veiculos.add(item.veiculoId());
      abastecimentos
          .computeIfAbsent(item.veiculoId(), id -> new ArrayList<>())
          .add(
              new EficienciaRemota.AbastecimentoCalculo(
                  item.realizadoEm(), item.litros(), item.valorTotal(), item.quilometragem()));
    }
    for (var item : historico.manutencoes()) {
      veiculos.add(item.veiculoId());
      manutencoes
          .computeIfAbsent(item.veiculoId(), id -> new ArrayList<>())
          .add(new EficienciaRemota.ManutencaoCalculo(item.valor()));
    }
    return new EficienciaRemota.Entrada(
        veiculos.stream()
            .map(
                id ->
                    new EficienciaRemota.VeiculoHistorico(
                        id,
                        placas.get(id),
                        List.copyOf(abastecimentos.getOrDefault(id, List.of())),
                        List.copyOf(manutencoes.getOrDefault(id, List.of()))))
            .toList());
  }

  private static Operacoes ultimos(OperacoesAgregadasRemotas.Historico historico) {
    return new Operacoes(
        historico.abastecimentos().stream()
            .sorted(
                Comparator.comparing(OperacoesAgregadasRemotas.Abastecimento::realizadoEm)
                    .reversed())
            .limit(ULTIMOS_REGISTROS)
            .toList(),
        historico.manutencoes().stream()
            .sorted(
                Comparator.comparing(OperacoesAgregadasRemotas.Manutencao::realizadaEm)
                    .reversed())
            .limit(ULTIMOS_REGISTROS)
            .toList());
  }

  private static String bearer(String token) {
    if (token == null || !token.startsWith("Bearer ") || token.length() <= 7)
      throw new FalhaAgregado(401);
    return token;
  }

  public record Operacoes(
      List<OperacoesAgregadasRemotas.Abastecimento> abastecimentos,
      List<OperacoesAgregadasRemotas.Manutencao> manutencoes) {
    static Operacoes vazia() {
      return new Operacoes(List.of(), List.of());
    }
  }

  public record Fontes(String frota, String operacoes, String funcao) {}

  public record Resultado(
      FrotaAgregadaRemota.Frota frota,
      Operacoes operacoes,
      EficienciaRemota.Eficiencia eficiencia,
      Fontes fontes,
      Instant geradoEm) {}
}
