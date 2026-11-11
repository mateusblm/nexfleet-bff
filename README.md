# NexFleet — BFF (Backend for Frontend)

Camada de composição entre o frontend e os microsserviços do NexFleet, plataforma de gestão de frotas. Parte da v1.0.0; o sistema completo está no repositório guarda-chuva [nexfleet](https://github.com/mateusblm/nexfleet).

## Arquitetura

O BFF recebe as chamadas do Gateway (e, na nuvem, do APIM), valida o JWT emitido pela Identidade e repassa ou compõe as respostas dos serviços internos. Não tem banco próprio.

```text
Navegador → shell + microfrontends (Static Web Apps)
          → Azure API Management (APIM) → Gateway → BFF
              ├─ MS1 Frota ........ MongoDB Atlas
              ├─ MS2 Operações .... Azure SQL (schema operacoes)
              ├─ Identidade, Indicadores ... Azure SQL (um schema e um login por serviço)
              └─ Azure Function HTTP calcular-eficiencia
Operações → outbox → Service Bus (nuvem) / RabbitMQ (local) → Indicadores + Function (notificação)
```

**`GET /aggregated-data`** (também `GET /api/v1/aggregated-data`, exige `Authorization: Bearer <token>`):

- chama Frota e Operações em paralelo, cada uma com tempo limite próprio (`AGREGADO_TIMEOUT`, padrão 5 s); com o histórico de Operações chama a Function `POST /api/calcular-eficiencia`, enviando a chave em `x-functions-key`;
- responde `200` com `frota`, `operacoes` (últimos 10 abastecimentos e manutenções), `eficiencia` (por veículo e da frota), `fontes` e `geradoEm`;
- **retorno parcial:** `fontes` traz `frota`, `operacoes` e `funcao` com `"ok"` ou `"falhou"`; a seção da fonte que falhou vem vazia e o restante continua preenchido. Se Operações falhar, a Function não é chamada (`funcao = "falhou"`);
- se as três fontes falharem, responde `502`; sem token, `401`;
- **limites:** lê até **4.000 abastecimentos** e **4.000 manutenções** de Operações; acima disso corta e marca `operacoes.truncado = true`.

**Clean Architecture + Vertical Slice.** Cada caso de uso é uma fatia em `br.com.nexfleet.bff`, com as camadas `api` (controllers), `application` (caso de uso e portas) e `infrastructure` (clientes HTTP):

| Fatia | Responsabilidade |
|---|---|
| `autenticar` | login e sessão, repassados à Identidade |
| `gerenciarusuarios` | administração de usuários |
| `gerenciarveiculos`, `gerenciarmotoristas`, `gerenciarvinculos` | cadastros de Frota |
| `gerenciarabastecimentos`, `gerenciarmanutencoes` | registros de Operações |
| `consultardashboard` | painel vindo de Indicadores |
| `consultaragregado` | `GET /aggregated-data` |
| `consultarambiente` | situação do ambiente e dos serviços |

Pacotes técnicos: `config` (composição e segurança) e `observability` (propagação do `X-Correlation-ID`).

**Testes:** unitários e de API com MockMvc e portas mockadas (ex.: `ConsultarAgregadoHttpTest`, `HttpEficienciaTest`, `OperacoesPorServicosTest`) e de arquitetura com ArchUnit (`ArchitectureTest`: `application` não depende de framework nem de adaptadores; `api` não acessa a persistência).

## Tecnologias

- Java 21, Spring Boot 4.1.1 (Web MVC, Security, OAuth2 Resource Server, Actuator)
- springdoc-openapi 3.2.0 (OpenAPI em `/v3/api-docs`)
- Micrometer + Prometheus
- JUnit 5, MockMvc, ArchUnit 1.4.1
- Maven Wrapper (Maven 3.9.16)
- Docker (`eclipse-temurin:21`), GitHub Actions (testes em todo push; imagem no Docker Hub em tag `v*`)

## Como rodar localmente

```bash
./mvnw -B -ntp verify                              # testes unitários e de arquitetura
docker build -t mateusblm/nexfleet-bff:local .     # imagem (porta 8080)
```

Variáveis de ambiente: `PORT`, `IDENTIDADE_URL`, `FROTA_URL`, `OPERACOES_URL`, `INDICADORES_URL`, `EFICIENCIA_FUNCTION_URL`, `EFICIENCIA_FUNCTION_KEY`, `AGREGADO_TIMEOUT` e `SPRING_PROFILES_ACTIVE=jwt-rsa` (valida o token pela JWKS da Identidade).

**Ambiente completo** (todos os serviços, SQL Server, MongoDB e RabbitMQ locais) sobe pelo repositório guarda-chuva [nexfleet](https://github.com/mateusblm/nexfleet), com os repositórios clonados lado a lado (`scripts/clonar-tudo.sh`):

```bash
scripts/iniciar-local.sh                                     # 1ª vez: gera o .env e a chave RSA local
docker compose -f compose.yaml -f compose.build.yaml up -d --build
scripts/seed-demo.sh                                         # dados de demonstração
```

Sem o override `compose.build.yaml`, o `docker compose up -d` usa as imagens publicadas no Docker Hub.

## URLs na nuvem

O BFF não tem URL pública direta: é acessado só pelo APIM (`/api/v1/...` e `/aggregated-data`).

Imagem Docker: [`mateusblm/nexfleet-bff:v1.0.0`](https://hub.docker.com/r/mateusblm/nexfleet-bff)

| Recurso | Endereço |
|---|---|
| Frontend | https://white-mushroom-0cadda010.3.azurestaticapps.net |
| API (APIM) | https://apim-nexfleet-homolog-ukzjodf42myfe.azure-api.net |
| Swagger UI (BFF, Frota, Operações, Identidade, Indicadores) | https://apim-nexfleet-homolog-ukzjodf42myfe.azure-api.net/swagger-ui.html |
| Dados agregados (exige token) | https://apim-nexfleet-homolog-ukzjodf42myfe.azure-api.net/aggregated-data |
| AsyncAPI dos eventos | https://white-mushroom-0cadda010.3.azurestaticapps.net/docs/eventos/ |
| Repositório guarda-chuva | https://github.com/mateusblm/nexfleet |

> **Partida a frio:** os Container Apps escalam a zero e o Azure SQL pausa sem uso. A primeira chamada após um período de inatividade pode levar de 1 a 2 minutos (ou responder 503/504); tente de novo em seguida.

## Equipe

- Vinícius dos Reis Góes
- Mateus Burlamaqui
- Lucas Roussenq
- Erik Carvalho
