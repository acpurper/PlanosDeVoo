# Planos de Voo

Backend do **Sistema de Gestão de Planos de Voo**, trabalho final de Projeto e Arquitetura de Software
(PUCRS). Grupo: Ana Clara Purper, Bianca Piassini, Maria Melloni e Tarciso Mota.

> **Data de referência dos dados iniciais: `2026-12-01`**
> (configurada em `app.data-referencia`, no arquivo `src/main/resources/application.yml`)

## Escopo desta entrega

5 dos 11 casos de uso do [modelo](docs/arquitetura.pdf):

| UC | Caso de uso | Endpoint |
|---|---|---|
| UC02 | Criar plano | `POST /planos` |
| UC03 | Editar percurso | `POST /planos/{id}/trechos`, `PUT` e `DELETE /planos/{id}/trechos/{ordem}` |
| UC06 | Validar plano | `POST /planos/{id}/validacao` |
| UC07 | Aprovar plano | `POST /planos/{id}/aprovacao` |
| UC11 | Cancelar plano | `POST /planos/{id}/cancelamento` |

Para apoiar a demonstração existe também `GET /planos/{id}`, que devolve o plano com trechos, horários
calculados, situação e reservas. Ficam para a próxima entrega: UC01, UC04, UC05, UC08 completo, UC09 e UC10.

Por orientação do professor, **não há casos de uso de cadastro**. Aeroportos, aerovias, pilotos e aeronaves
são carregados por script (ver [decisão I1](docs/decisoes.md#i1-dispensa-dos-casos-de-uso-de-cadastro)).

## Pré-requisitos

- **JDK 21** ou mais novo (`java -version`)
- **Docker** com Docker Compose (`docker compose version`)
- Não é preciso instalar o Maven: o projeto inclui o Maven Wrapper (`./mvnw`)

## Como executar

```bash
# 1. Subir o PostgreSQL 16 (porta 5432, banco/usuário/senha: planosvoo)
docker compose up -d

# 2. Compilar e executar a aplicação (porta 8080)
./mvnw spring-boot:run
```

Na primeira inicialização, a aplicação:

1. aplica as migrações Flyway (`src/main/resources/db/migration`):
   - `V1__schema.sql`: tabelas e restrições, incluindo as `UNIQUE` das reservas;
   - `V2__malha_inicial.sql`: POA (41 vagas), FLN (16), CWB (26), GRU (115) e as aerovias R1 a R8;
   - `V3__frota_pilotos.sql`: 4 pilotos e 6 aeronaves, duas por categoria;
2. executa o `DadosIniciaisRunner`, que cria e aprova os planos iniciais **pelos próprios casos de uso**
   (só roda se o banco não tiver nenhum plano).

Os dados ficam no volume `pgdata` e **persistem entre reinicializações**. Para voltar ao estado inicial:

```bash
docker compose down -v && docker compose up -d && ./mvnw spring-boot:run
```

Para gerar o jar: `./mvnw package` e depois `java -jar target/gestaoplanosvoo-0.1.0.jar`.

### Configuração

| Variável | Padrão | Uso |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/planosvoo` | Endereço do banco |
| `DB_USER` / `DB_PASSWORD` | `planosvoo` / `planosvoo` | Credenciais |
| `APP_RELOGIO_FIXO` | vazio (relógio do sistema) | Fixa o "agora", ex.: `2026-11-20T10:00` |

## Acessando a API

- **Swagger UI:** <http://localhost:8080/swagger-ui.html>
- **OpenAPI (JSON):** <http://localhost:8080/v3/api-docs>
- **Roteiro de demonstração:** [`docs/demo.http`](docs/demo.http). Cobre o ciclo completo e cada tipo de
  falha (percurso inconsistente, horário incompatível, restrições da aeronave, autonomia, conflito de
  aerovia, vaga ocupada, aprovação sem reserva parcial, cancelamento e edição de plano não editável). Pode
  ser executado com a extensão REST Client do VS Code ou pelo HTTP Client do IntelliJ.

Exemplo rápido:

```bash
curl -s localhost:8080/planos/1                       # plano aprovado da carga inicial
curl -s -X POST localhost:8080/planos/7/validacao     # AUTONOMIA_EXCEDIDA
curl -s -X POST localhost:8080/planos/6/cancelamento  # 409: voo já executado
```

## Dados iniciais

Na data de referência **2026-12-01**, os planos aprovados ocupam **7 das 8 aerovias** (R1, R2, R3, R4, R5,
R7 e R8). A R6 fica livre, e em cada aerovia só uma altitude está ocupada, o que permite demonstrar tanto
conflitos quanto recursos disponíveis.

| Plano | Situação | Resumo |
|---|---|---|
| 1 | APROVADO | PR-AZA POA→GRU por R2 a 30.000 pés, 08:10→09:12, pouso GRU vaga 1 |
| 2 | APROVADO | PP-JAT POA→FLN (sobrevoo)→CWB por R1/R4 a 26.000, 14:00→15:02, pouso CWB vaga 1 |
| 3 | APROVADO | PR-CGA (carga) GRU→CWB→FLN por R7/R5 a 32.000, 01:00→02:17 |
| 4 | APROVADO | PR-GOB FLN→POA por R8 a 29.000, 10:30→10:57 |
| 5 | APROVADO | PR-AZA GRU→POA por R3 a 30.000, 18:00→19:02 |
| 6 | APROVADO em **2026-09-01** | voo já executado: não pode ser cancelado |
| 7 | EM_EDICAO | PP-LEN POA→FLN→CWB em sobrevoo: 608 km > 595 km (autonomia) |

| Aeronave | Categoria | km/h | Autonomia (85%) |
|---|---|---|---|
| PP-LEN | Particular | 400 | 700 km (595 km) |
| PP-JAT | Particular | 600 | 1.500 km (1.275 km) |
| PR-AZA | Passageiros | 850 | 5.000 km (4.250 km) |
| PR-GOB | Passageiros | 830 | 4.000 km (3.400 km) |
| PR-CGA | Carga | 780 | 3.500 km (2.975 km) |
| PR-CGB | Carga | 700 | 1.000 km (850 km) |

A data de referência precisa ser **futura** na apresentação, senão o cancelamento de plano aprovado futuro
não pode ser demonstrado. Se ela já tiver passado, suba com `APP_RELOGIO_FIXO=2026-11-20T10:00`.

## Testes

```bash
./mvnw verify
```

- **Unidade** (`src/test/.../domain`, sem Spring nem banco): tempo e chegada, slots (incluindo os exemplos do
  enunciado), altitude, regras por categoria, autonomia, percurso e horários, pouso e disponibilidade,
  acúmulo de violações e ciclo de vida.
- **Integração** (`PlanoDeVooIT`, com Testcontainers e PostgreSQL 16 real; precisa do Docker rodando):
  validar não cria reservas; aprovação com violação não grava nada; duas aprovações concorrentes do mesmo
  slot resultam em uma aprovada e outra com 409, sem reserva parcial; cancelar plano futuro libera as
  reservas; cancelar plano passado responde 409; editar plano aprovado responde 409. Também cobrem os erros
  da API: 400 (campos ausentes), 404 (plano, piloto, aeronave, aeroporto, aerovia, vaga ou trecho
  inexistente), 422 (origem igual ao destino, altitude inválida) e 409 (cancelar plano em edição).

Total: 63 testes de unidade e 15 de integração.

## Arquitetura e documentação

**Clean Architecture / Ports and Adapters**, raiz `br.pucrs.projarc.gestaoplanosvoo`:

```
domain/          Java puro: model/ (PlanoDeVoo, Trecho, Pouso, ...), model/aeronave/ (Strategy),
                 model/reserva/, repository/ (portas), exception/
application/     usecases/ (um por caso de uso, @Transactional em aprovar e cancelar), dto/
infrastructure/  api/ (controller + ProblemDetail), persistence/ (entidades JPA, Spring Data,
                 adaptadores, mappers), config/ (Clock, OpenAPI, carga inicial)
```

| Documento | Conteúdo |
|---|---|
| [`docs/enunciado.md`](docs/enunciado.md) | Enunciado completo (prevalece em caso de dúvida) |
| [`docs/arquitetura.pdf`](docs/arquitetura.pdf) | Modelo de domínio UML, casos de uso, C4 (níveis 1 a 3), estrutura de pacotes e decisões; atualizado para a versão implementada |
| [`docs/dominio.png`](docs/dominio.png), [`docs/casos-de-uso.png`](docs/casos-de-uso.png) | Diagramas UML do domínio e dos casos de uso; fontes em [`dominio.puml`](docs/dominio.puml) e [`casos-de-uso.puml`](docs/casos-de-uso.puml) |
| [`docs/c4.md`](docs/c4.md) | C4 níveis 1 a 3 da versão implementada, com tabelas de componentes e a dinâmica da aprovação; fontes PlantUML em [`docs/c4/`](docs/c4/) |
| [`docs/decisoes.md`](docs/decisoes.md) | Decisões de implementação, com alternativas e justificativas |
| [`docs/demo.http`](docs/demo.http) | Roteiro de requisições para a apresentação |
