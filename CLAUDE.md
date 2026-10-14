# Sistema de Gestão de Planos de Voo — guia para o Claude Code

Trabalho Final de Projeto e Arquitetura de Software (PUCRS). Grupo: Ana Clara Purper, Bianca Piassini, Maria Melloni, Tarciso Mota.
Responda e comente o código em português do Brasil.

## Fontes de verdade

- `docs/enunciado.md`: enunciado completo. Em caso de dúvida, ele prevalece.
- `docs/arquitetura.pdf`: modelo de domínio, casos de uso e C4 (versão de 7/10, 11 casos de uso, ator único Operador de Voo).
- O professor confirmou que **não haverá casos de uso de cadastro**. Aeroportos, aerovias, pilotos e aeronaves entram por script de carga (Flyway).

## Escopo desta entrega (5 de 11 casos de uso, ~45%)

| UC | Caso de uso | Endpoint |
|---|---|---|
| UC02 | Criar plano | `POST /planos` |
| UC03 | Editar percurso | `POST /planos/{id}/trechos`, `PUT /planos/{id}/trechos/{ordem}`, `DELETE /planos/{id}/trechos/{ordem}` |
| UC06 | Validar plano | `POST /planos/{id}/validacao` |
| UC07 | Aprovar plano | `POST /planos/{id}/aprovacao` |
| UC11 | Cancelar plano | `POST /planos/{id}/cancelamento` |

Suporte mínimo para demonstrar os casos acima: `GET /planos/{id}`, que devolve o plano com trechos, horários calculados, situação e reservas. Ele não conta como UC entregue.

Ficam fora desta entrega: UC01, UC04, UC05, UC08 completo, UC09 e UC10.

## Stack

Java 21, Spring Boot 3.x, Maven, Spring Web, Spring Data JPA, Bean Validation, Flyway, PostgreSQL 16 via `docker-compose.yml`, springdoc-openapi (Swagger UI), JUnit 5, AssertJ e Testcontainers (Postgres) nos testes de integração.

## Arquitetura (Clean Architecture / Ports and Adapters)

Siga a árvore de pacotes da seção 3.5 de `docs/arquitetura.pdf`, com raiz `br.pucrs.projarc.gestaoplanosvoo`:

- `domain/`: Java puro. Nenhuma anotação Spring ou JPA. Contém entidades, objetos de valor, `RegraOperacao` (Strategy) e as portas de repositório.
- `application/`: casos de uso e DTOs. O `@Transactional` fica aqui, nos casos de uso de aprovar e cancelar.
- `infrastructure/api`: controllers e `@RestControllerAdvice`. Erros seguem RFC 7807 (`ProblemDetail`).
- `infrastructure/persistence`: entidades JPA separadas das do domínio, repositórios Spring Data, adaptadores e mappers entre domínio e JPA.
- `infrastructure/config`: beans, incluindo um `java.time.Clock` injetável.

Toda mutação de plano passa pelos métodos de `PlanoDeVoo`, que é a raiz do agregado. Esses métodos lançam exceção se a situação não for `EM_EDICAO`, e a API responde 409.

## Regras de domínio e decisões de implementação

Registre cada item marcado com **[decisão]** em `docs/decisoes.md`, com a alternativa considerada e a justificativa.

1. **Tempo.** Horários são `LocalDateTime` com precisão de minuto. A duração de um trecho é `distanciaKm / velocidadeKmh` horas, convertida em minutos e **arredondada para cima** (`ceil`). **[decisão]** Assim o usuário consegue informar a entrada seguinte exatamente, e a ocupação de slots fica conservadora.
2. **Chegada.** `chegada = entrada + duração`. O valor é derivado e nunca vem do cliente.
3. **Slots.** O intervalo de uso é semiaberto, `[início, fim)`. Um slot `[h, h+1h)` está ocupado se `h < fim` e `h + 1h > início`. **[decisão]** Portanto, uma saída exatamente às 17:00 não ocupa o slot das 17:00. A lógica vale igual para trecho (entrada→chegada) e pouso (chegada→chegada+permanência).
4. **Altitude.** Objeto de valor com faixa de 25.000 a 35.000 pés, em passos de 1.000. Valor inválido gera 422 já na edição.
5. **Regras de operação (Strategy).** Cada categoria de aeronave tem sua `RegraOperacao`:
   - particular: somente 25.000, 26.000 ou 27.000 pés;
   - passageiros: 28.000 pés ou mais;
   - carga: `início >= 00:00` e `fim <= 06:00` do mesmo dia, valendo para o trecho inteiro.
   Adicionar uma regra nova deve exigir apenas uma classe nova.
6. **Autonomia.** A distância acumulada desde o último pouso, ou desde a partida, deve ser `<= 0,85 × autonomiaKm`. Sobrevoo acumula distância. Pouso zera o acumulado.
7. **Percurso.** A validação verifica:
   - existe pelo menos um trecho;
   - a origem da primeira aerovia é a origem do plano;
   - o destino da última aerovia é o destino do plano;
   - cada aerovia começa onde a anterior terminou;
   - a entrada do primeiro trecho é igual à partida;
   - após sobrevoo, a próxima entrada é igual à chegada;
   - após pouso, a próxima entrada é igual a chegada + permanência;
   - há pouso no último trecho;
   - a data da chegada final é a data da partida (a permanência final pode passar da meia-noite).
8. **Pouso.** A vaga informada precisa pertencer ao aeroporto de destino da aerovia daquele trecho.
9. **Disponibilidade.** A altitude da aerovia precisa estar livre em todos os slots do trecho, e a vaga precisa estar livre em todos os slots da permanência. A checagem consulta as reservas atuais no banco.
10. **`ResultadoValidacao`.** Acumula **todas** as violações. Cada uma tem `codigo` (por exemplo `PERCURSO_DESCONTINUO`, `HORARIO_INCOMPATIVEL`, `ALTITUDE_PROIBIDA_CATEGORIA`, `AUTONOMIA_EXCEDIDA`, `ALTITUDE_OCUPADA`, `VAGA_OCUPADA`, `DIAS_DIFERENTES`, `SEM_POUSO_FINAL`), `trecho` (a ordem, quando aplicável) e `mensagem`.
11. **Validar (UC06).** Não altera o estado nem grava nada. Responde 200 com o resultado, válido ou não. Plano fora de edição gera 409.
12. **Aprovar (UC07).** O fluxo é:
   - uma transação no caso de uso;
   - revalida tudo (reaproveitando a lógica de UC06);
   - se houver violações, responde 422 com a lista e o plano continua `EM_EDICAO`;
   - senão, cria uma `ReservaAerovia` por slot de cada trecho e uma `ReservaVaga` por slot de cada pouso;
   - muda o plano para `APROVADO` e chama `flush()` dentro da transação.
   As constraints `UNIQUE (aerovia_id, altitude_pes, slot_inicio)` e `UNIQUE (vaga_id, slot_inicio)` garantem a atomicidade sob concorrência. Uma `DataIntegrityViolationException` responde 409 e faz rollback total.
13. **Cancelar (UC11).** Só vale para plano `APROVADO` com `dataHoraPartida > LocalDateTime.now(clock)`. Numa única transação, apaga as reservas do plano e muda a situação para `CANCELADO`. Plano já executado ou não aprovado gera 409. Os dados do plano são preservados.
14. **Criar (UC02).** Piloto, aeronave ou aeroporto inexistente gera 404. Origem igual ao destino gera 422.
15. **Editar (UC03).** `POST` acrescenta um trecho no fim; `PUT` e `DELETE` agem sobre a ordem indicada e reordenam os seguintes. **[decisão]** Atende a construção progressiva da seção 4. Aerovia ou vaga inexistente gera 404. A resposta traz o percurso com os horários calculados.

## Banco e carga inicial

- `V1__schema.sql`: todas as tabelas, incluindo as constraints de unicidade das reservas. Aerovia não tem operação de alteração nem de exclusão no código.
- `V2__malha_inicial.sql`: os 4 aeroportos com suas vagas numeradas (POA 41, FLN 16, CWB 26, GRU 115) e as 8 aerovias R1–R8 da seção 2 do enunciado.
- `V3__frota_pilotos.sql`: pelo menos 3 pilotos e 2 aeronaves por categoria. Escolha números que gerem casos de demonstração. Por exemplo, uma particular a 400 km/h com autonomia de 700 km: POA→FLN (363 km) passa, mas POA→FLN→CWB em sobrevoo (608 km) e POA→GRU (865 km) estouram os 595 km.
- **Planos aprovados e reservas da data de referência.** Use um `ApplicationRunner` (`DadosIniciaisRunner`) que cria e aprova planos **chamando os próprios casos de uso** e só roda se não houver nenhum plano. **[decisão]** Assim as reservas são sempre consistentes com os planos, sem cálculo manual de slots em SQL.
  - Data de referência: `app.data-referencia=2026-12-01` em `application.yml`. Ela precisa ser **futura**, senão o cancelamento não pode ser demonstrado.
  - Pelo menos 5 das 8 aerovias com alguma ocupação nessa data, com conflitos possíveis sem esgotar os recursos.
  - Inclua também 1 plano aprovado com partida já passada, para demonstrar a recusa de cancelamento.
- Tudo precisa ser reproduzível com `docker compose up -d` seguido de `./mvnw spring-boot:run`.

## Testes

- **Unidade (domínio, sem Spring):** um teste para cada regra dos itens 1 a 10, cobrindo o caso de sucesso e o de falha. Inclua o exemplo dos slots do enunciado: 15:52→17:07 ocupa 3 slots, e chegada 15:50 com permanência de 20 min ocupa 2.
- **Integração (Testcontainers):**
  - validar não cria reservas;
  - aprovação com conflito não deixa nenhuma reserva parcial;
  - duas aprovações concorrentes do mesmo slot resultam em uma aprovada e uma com 409;
  - cancelar plano futuro libera as reservas;
  - cancelar plano passado responde 409;
  - editar plano aprovado responde 409.

## Entregáveis desta etapa

1. Código e testes passando (`./mvnw verify`).
2. `README.md` com preparação, subida do banco, execução, endereço do Swagger UI e a data de referência.
3. `docs/decisoes.md` com as decisões marcadas acima. Inclua a decisão do professor de dispensar os cadastros, que contradiz o texto da seção 8.1 do enunciado.
4. `docs/demo.http`: roteiro de requisições cobrindo sucesso e cada tipo de falha na data de referência.
5. Correções na documentação C4:
   - Nível 2: a SPA fica **fora** da fronteira do sistema, como cliente externo. Use "Cliente HTTP / Swagger UI", porque o escopo é só backend.
   - Nível 3: `ValidarPlanoUseCase` é um caso de uso, não um "Domain Service". Os componentes do diagrama precisam bater com os pacotes realmente implementados.

## Ordem sugerida

Esqueleto Maven + docker-compose → domínio e testes de unidade → schema e seeds → adaptadores de persistência → casos de uso → controllers e tratamento de erros → `DadosIniciaisRunner` → testes de integração → README, `decisoes.md`, `demo.http` e ajustes no C4.
