package br.com.nexfleet.bff.config;

import br.com.nexfleet.bff.consultaragregado.application.*;
import br.com.nexfleet.bff.consultaragregado.infrastructure.*;
import br.com.nexfleet.bff.gerenciarabastecimentos.infrastructure.HttpAbastecimentos;
import br.com.nexfleet.bff.gerenciarmanutencoes.infrastructure.HttpManutencoes;
import br.com.nexfleet.bff.gerenciarmotoristas.infrastructure.HttpMotoristas;
import br.com.nexfleet.bff.gerenciarveiculos.infrastructure.HttpVeiculos;
import br.com.nexfleet.bff.gerenciarvinculos.infrastructure.HttpVinculos;
import br.com.nexfleet.bff.observability.CorrelationIds;
import java.net.http.HttpClient;
import java.time.Clock;
import java.time.Duration;
import java.util.concurrent.Executors;
import org.springframework.beans.factory.annotation.*;
import org.springframework.context.annotation.*;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/** Agregação {@code GET /aggregated-data}: Frota, Operações e Function em paralelo. */
@Configuration
public class AgregadoConfig {
  @Bean
  FrotaAgregadaRemota frotaAgregada(@Qualifier("frotaRestClient") RestClient frota) {
    return new FrotaPorServicos(
        new HttpVeiculos(frota), new HttpMotoristas(frota), new HttpVinculos(frota));
  }

  @Bean
  OperacoesAgregadasRemotas operacoesAgregadas(
      @Qualifier("operacoesRestClient") RestClient operacoes) {
    return new OperacoesPorServicos(new HttpAbastecimentos(operacoes), new HttpManutencoes(operacoes));
  }

  @Bean
  EficienciaRemota eficienciaRemota(
      @Value("${eficiencia.function.url}") String url,
      @Value("${eficiencia.function.key}") String chave,
      @Value("${agregado.timeout}") Duration timeout) {
    var factory =
        new JdkClientHttpRequestFactory(
            HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build());
    factory.setReadTimeout(timeout);
    return new HttpEficiencia(
        CorrelationIds.propagate(RestClient.builder()).baseUrl(url).requestFactory(factory).build(),
        chave);
  }

  @Bean
  ConsultarAgregado consultarAgregado(
      FrotaAgregadaRemota frota,
      OperacoesAgregadasRemotas operacoes,
      EficienciaRemota eficiencia,
      @Value("${agregado.timeout}") Duration timeout) {
    return new ConsultarAgregado(
        frota,
        operacoes,
        eficiencia,
        CorrelationIds.contextual(Executors.newVirtualThreadPerTaskExecutor()),
        timeout,
        Clock.systemUTC());
  }
}
