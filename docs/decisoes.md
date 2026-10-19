# Decisões de projeto e implementação

Este documento complementa as decisões de modelagem D1–D7 de [`arquitetura.pdf`](arquitetura.pdf)
(seção 2.3), que continuam valendo. Aqui ficam as decisões tomadas durante a implementação.
Cada uma traz a escolha feita, a alternativa considerada e o motivo.

## Resumo

| # | Decisão | Onde no código |
|---|---|---|
| I1 | Sem casos de uso de cadastro: a malha, os pilotos e a frota entram por script | `db/migration/V2__*.sql`, `V3__*.sql` |
| I2 | Duração arredondada **para cima** ao minuto inteiro | `Aerovia.duracao` |
| I3 | Intervalos semiabertos `[início, fim)` para slots | `SlotHorario.cobrindo` |
| I4 | Edição progressiva: POST no fim; PUT e DELETE por ordem, com renumeração | `PlanoDeVoo.adicionar/alterar/removerTrecho` |
| I5 | Planos iniciais criados pelos próprios casos de uso | `DadosIniciaisRunner` |
| I6 | Exclusividade de recurso garantida por `UNIQUE` no banco + `flush()` na aprovação | `V1__schema.sql`, `ReservaRepositoryAdapter` |
| I7 | Lock pessimista do plano na aprovação e no cancelamento | `PlanoVooJpaRepository.buscarComBloqueio` |
| I8 | Entidades JPA separadas das entidades de domínio | `infrastructure/persistence` |
| I9 | Regras de operação declaradas pela subclasse da aeronave (Strategy) | `domain/model/aeronave` |
| I10 | Validação acumulativa; aprovação revalida com a disponibilidade do momento | `PlanoDeVoo.validar`, `AprovarPlanoUseCase` |
| I11 | Vaga procurada no aeroporto de destino da aerovia do trecho | `EditarPercursoUseCase.pouso` |
| I12 | `Clock` injetável, com opção de relógio fixo | `RelogioConfig` |
| I13 | Data de referência 2026-12-01 | `application.yml` |
| I14 | Hierarquia de aeronaves em tabela única | `AeronaveEntity` |
| I15 | Esquema controlado só pelo Flyway | `application.yml` (`ddl-auto: none`) |
| I16 | Erros em RFC 7807 com código estável | `ApiExceptionHandler` |
| I17 | Spring Boot 3.5 | `pom.xml` |
| I18 | Disponibilidade: livre em **todos** os slots do período, calculada com uma consulta por recurso | `ConsultarAltitudesLivresUseCase`, `ConsultarVagasLivresUseCase` |
| I19 | Listagem de planos devolve um resumo; o detalhe fica em `GET /planos/{id}` | `ListarPlanosUseCase`, `PlanoResumoResponse` |
| I20 | Abandono pela mesma via de bloqueio do cancelamento | `AbandonarPlanoUseCase` |
| I21 | Carga inicial inclui um plano abandonado e um cancelado | `DadosIniciaisRunner` |
| I22 | A aprovação não exige partida futura | `PlanoDeVoo.aprovar` |

---

## I1. Dispensa dos casos de uso de cadastro

**Contexto.** A seção 8.1 do enunciado pede "cadastrar novos aeroportos e aerovias" e "cadastrar os
dados necessários" de pilotos e aeronaves. O **professor confirmou depois que não haverá casos de uso de
cadastro**, o que contradiz o texto da seção 8.1. O modelo de casos de uso (11 UCs, `arquitetura.pdf`) já
trata essas entidades como dados pré-existentes.

**Decisão.** Aeroportos (com vagas numeradas), aerovias, pilotos e aeronaves são carregados por
migrações Flyway versionadas. Para incluir novos aeroportos ou aerovias informados pelo professor, cria-se
uma nova migração (`V4__...sql`), **sem alterar código**. Isso continua atendendo à seção 2: novos
elementos entram "sem alterações na lógica da aplicação".

A dispensa vale só para o **cadastro**. A **consulta** desses dados continua no escopo (UC01) e está
disponível em `GET /aeroportos`, `/aerovias`, `/pilotos` e `/aeronaves`.

**Alternativa.** Endpoints `POST /aeroportos`, `/aerovias`, `/pilotos`, `/aeronaves`. Foram descartados
por orientação do professor. Se voltarem a ser exigidos, entram como novos casos de uso em
`application/usecases`, usando as mesmas portas.

## I2. Duração arredondada para cima ao minuto

**Decisão.** Os horários são `LocalDateTime` com precisão de minuto. A duração de um trecho é
`distânciaKm / velocidadeKmh` horas, convertida em minutos e arredondada **para cima** (`ceil`). A conta
usa `BigDecimal` para que divisões exatas (360 km a 600 km/h = 36 min) não virem 37 min por erro de
ponto flutuante. Entradas com segundos são truncadas para o minuto.

**Alternativas.**
- *Precisão de segundos.* O usuário teria de informar entradas como `14:36:18` para casar com a chegada
  calculada, o que é inviável na prática.
- *Arredondar para o minuto mais próximo.* Poderia encurtar o uso real da aerovia e liberar um slot que
  ainda está em uso.

**Motivo.** Com minutos inteiros, o usuário consegue informar a entrada seguinte exatamente. O
arredondamento para cima também deixa a ocupação de slots conservadora, nunca menor que a real.

## I3. Intervalos semiabertos `[início, fim)`

**Decisão.** Todo uso de recurso é um intervalo `[início, fim)`. O slot `[h, h+1h)` está ocupado se
`h < fim` e `h + 1h > início`. Isso vale para o trecho (entrada → chegada) e para o pouso
(chegada → chegada + permanência).

Consequências:
- 15:52 → 17:07 ocupa 15h, 16h e 17h; chegada 15:50 com 20 min ocupa 15h e 16h (os exemplos do enunciado,
  ambos testados em `SlotHorarioTest`);
- uma saída exatamente às 17:00 **não** ocupa o slot das 17h;
- carga: o uso precisa caber em `[00:00, 06:00)` do dia em que começa, e terminar exatamente às 06:00 é
  permitido.

**Alternativa.** Intervalo fechado `[início, fim]`. Uma chegada às 17:00 bloquearia o slot 17h–18h sem
uso real, e um voo de carga que termina às 06:00 seria recusado.

## I4. Edição trecho a trecho

**Decisão.** `POST /planos/{id}/trechos` acrescenta um trecho no fim. `PUT` e `DELETE
/planos/{id}/trechos/{ordem}` agem sobre a ordem indicada. A remoção renumera os trechos seguientes. Cada
resposta traz o plano com os horários calculados.

**Alternativa.** Um único `PUT /planos/{id}/trechos` que substitui o percurso inteiro.

**Motivo.** Atende à construção progressiva da seção 4 ("construir progressivamente um plano de voo") e
deixa o cliente ver a chegada calculada de cada trecho antes de informar a entrada do seguinte. A ordem
funciona como identificador do trecho dentro do agregado (D2).

## I5. Planos iniciais criados pelos casos de uso

**Decisão.** O `DadosIniciaisRunner` (`ApplicationRunner`) cria, edita e aprova os planos iniciais
**chamando os próprios casos de uso**. Ele só roda se não houver nenhum plano no banco.

**Alternativa.** Inserir planos, trechos e reservas direto em SQL, numa migração.

**Motivo.** As reservas ficam sempre consistentes com os planos e com as regras, sem cálculo manual de
slots em SQL. Se uma regra mudar, a carga quebra na inicialização em vez de gravar dados incoerentes. A
carga continua reproduzível: com o banco vazio, o resultado é sempre o mesmo.

Situação inicial na data de referência (2026-12-01):

| Plano | Aeronave | Percurso | Reservas |
|---|---|---|---|
| 1 | PR-AZA | POA→GRU por R2 a 30.000, 08:10→09:12, pouso GRU-1 por 60 min | R2/30000 08h e 09h; GRU-1 09h e 10h |
| 2 | PP-JAT | R1 a 26.000 14:00→14:37 com sobrevoo de FLN; R4 14:37→15:02, pouso CWB-1 por 45 min | R1/26000 14h; R4/26000 14h e 15h; CWB-1 15h |
| 3 | PR-CGA | R7 a 32.000 01:00→01:28, pouso CWB-2 por 30 min; R5 01:58→02:17, pouso FLN-1 por 120 min | R7 01h; R5 01h e 02h; CWB-2 01h; FLN-1 02h a 04h |
| 4 | PR-GOB | FLN→POA por R8 a 29.000, 10:30→10:57, pouso POA-1 por 60 min | R8/29000 10h; POA-1 10h e 11h |
| 5 | PR-AZA | GRU→POA por R3 a 30.000, 18:00→19:02, pouso POA-2 por 40 min | R3/30000 18h e 19h; POA-2 19h |
| 6 | PR-GOB | igual ao 4, mas em **2026-09-01** (voo já executado) | não pode ser cancelado |
| 7 | PP-LEN | **em edição**: R1 com sobrevoo de FLN + R4 = 608 km > 595 km | demonstra autonomia excedida |

**7 de 8 aerovias** ficam ocupadas na data (mínimo exigido: 5). A R6 fica livre, e cada aerovia tem só uma
altitude ocupada, então sempre existe alternativa disponível.

## I6. Exclusividade garantida pelo banco

**Decisão.** Há uma reserva por slot, com `UNIQUE (aerovia_id, altitude_pes, slot_inicio)` e
`UNIQUE (vaga_id, slot_inicio)`. Na aprovação, o caso de uso grava as reservas e chama `flush()`
**dentro da transação**. Se outro plano ocupou o recurso depois da validação, a restrição falha, o
adaptador converte a `DataIntegrityViolationException` em `ConflitoReservaException` (409) e a transação
inteira é desfeita. Nenhuma reserva parcial permanece. Isso é testado com duas aprovações simultâneas
reais (`PlanoDeVooIT`).

A validação também confere conflitos **entre trechos do próprio plano**. Por exemplo, um percurso que
passa duas vezes por R1 na mesma altitude dentro da mesma hora, ou dois pousos na mesma vaga no mesmo
slot. Nesses casos ela gera `ALTITUDE_OCUPADA` ou `VAGA_OCUPADA`, com uma mensagem que cita o trecho que
usou o recurso antes. Sem essa checagem, o plano passaria na validação e a UNIQUE só falharia na gravação,
com um 409 que atribuía o conflito a "outro plano". Agora o caso responde 422 com a lista de violações.

**Alternativas.**
- *Só verificar e depois inserir.* Duas aprovações simultâneas poderiam passar na verificação e gravar as
  duas.
- *Isolamento `SERIALIZABLE`.* Também funciona, mas gera falhas de serialização difíceis de explicar ao
  cliente e depende do comportamento do banco.

## I7. Lock pessimista do plano

**Decisão.** Toda operação que altera o plano carrega-o com `SELECT ... FOR UPDATE`: edição do percurso
(incluir, alterar e remover trecho), aprovação, cancelamento e abandono. A entidade também tem `@Version`,
como segunda barreira.

**Motivo.** Duas operações sobre o **mesmo** plano não podem se intercalar. A restrição UNIQUE resolve a
disputa entre planos diferentes; o lock resolve a disputa dentro do mesmo plano. O caso que motivou
estender o lock à edição: incluir um trecho só insere linhas em `trecho` e **não** incrementa a versão do
plano, então o `@Version` sozinho não garantia que uma edição concorrente com a aprovação falhasse de
forma previsível. Com o lock, a edição espera a aprovação terminar, encontra o plano `APROVADO` e recebe
409 `PLANO_NAO_EDITAVEL`, e o percurso aprovado fica igual ao que gerou as reservas. O teste
`edicaoDuranteAAprovacaoEsperaOLockERecebe409` reproduz essa corrida.

**Alternativa.** Só `@Version`. Não cobre alterações que mexem apenas na coleção de trechos e devolve um
erro genérico de edição concorrente. A validação (UC06) e as consultas não bloqueiam, porque não alteram
nada.

## I8. Entidades JPA separadas do domínio

**Decisão.** O pacote `domain` é Java puro, sem nenhuma anotação Spring ou JPA. A persistência tem
entidades próprias (`*Entity`), repositórios Spring Data e mappers.

**Alternativa.** Anotar as classes de domínio com JPA, o que exigiria menos código.

**Motivo.** O domínio fica testável sem banco nem Spring (63 testes rodam em milissegundos) e não precisa
de construtores vazios nem de setters que quebrariam invariantes, como a imutabilidade da `Aerovia`. O
custo é o código de mapeamento, concentrado em `MalhaMapper` e `PlanoMapper`.

## I9. Regras de operação como Strategy declarada pela subclasse

**Decisão.** `RegraOperacao` é uma interface com três implementações parametrizáveis: `AltitudesPermitidas`,
`AltitudeMinima` e `JanelaHoraria`. Cada subclasse de `Aeronave` declara a lista das suas regras.
`PlanoDeVoo` só pergunta `aeronave.regrasVioladas(...)`.

**Como evoluir:**
- *regra nova:* uma classe nova que implementa `RegraOperacao`, incluída na lista da categoria;
- *categoria nova:* uma subclasse de `Aeronave`, uma subclasse de `AeronaveEntity` e um ramo no
  `MalhaMapper`. Validação e casos de uso não mudam. Na API, `GET /aeronaves` já lista a categoria nova
  com os dados comuns e as regras; para mostrar os campos próprios dela, basta um ramo em
  `AeronaveDetalheResponse`.

**Alternativa.** Um `switch` por categoria dentro da validação, que obrigaria a mexer no validador a cada
categoria ou regra nova. É a alternativa já descartada em D1.

## I10. Validação acumulativa e revalidação na aprovação

**Decisão.** `PlanoDeVoo.validar` devolve um `ResultadoValidacao` com **todas** as violações. Cada
violação tem `codigo`, `trecho` e `mensagem`. O UC06 sempre responde 200 com esse laudo. O UC07 executa a
mesma validação (via `ValidarPlanoUseCase`, «include») com a disponibilidade do momento e responde 422 com
a lista se houver violação. Além disso, `PlanoDeVoo.aprovar` revalida internamente antes de gerar as
reservas, de modo que o agregado nunca aceita ser aprovado em estado inválido, qualquer que seja quem o
chame.

**Alternativa.** Lançar exceção na primeira falha. Isso obrigaria o usuário a corrigir uma
inconsistência por vez.

**Custo.** Na aprovação a validação roda duas vezes (no caso de uso e no agregado). São poucas consultas
indexadas, e em troca a invariante fica protegida no próprio domínio.

## I11. Vaga resolvida no destino da aerovia

**Decisão.** No trecho, o cliente informa só o **número** da vaga. O caso de uso procura essa vaga no
aeroporto de destino da aerovia daquele trecho e responde 404 se ela não existir. A validação também
verifica que a vaga pertence ao destino (`VAGA_FORA_DO_AEROPORTO`), como proteção da regra no próprio
domínio.

**Alternativa.** O cliente informar o id global da vaga, o que permitiria apontar por engano uma vaga de
outro aeroporto.

## I12. Relógio injetável

**Decisão.** Um `java.time.Clock` é injetado no cancelamento, que precisa saber se a partida já ocorreu.
Com `app.relogio-fixo` (ou a variável `APP_RELOGIO_FIXO`), o "agora" pode ser fixado.

**Motivo.** Os testes de integração fixam o agora em 2026-11-01 e não quebram depois que a data de
referência passar. Na apresentação, o mesmo recurso permite simular qualquer momento.

## I13. Data de referência 2026-12-01

**Decisão.** `app.data-referencia: 2026-12-01`. Ela precisa ser **futura** no momento da apresentação,
senão o cancelamento de plano aprovado futuro não pode ser demonstrado. O plano 6 fica três meses antes
(2026-09-01) e representa um voo já executado.

Se a apresentação ocorrer depois de 2026-12-01, há duas saídas: mudar a data e recriar o banco
(`docker compose down -v`), ou subir com `APP_RELOGIO_FIXO=2026-11-20T10:00`.

## I14. Aeronaves em tabela única

**Decisão.** A hierarquia `Aeronave` é persistida numa tabela só, discriminada pela coluna `categoria`
(`SINGLE_TABLE`).

**Alternativa.** Uma tabela por subclasse (`JOINED`), que evita colunas nulas mas exige joins em toda
leitura de aeronave.

**Motivo.** São poucas categorias e poucos atributos específicos. Uma categoria nova é só mais um valor de
discriminador e eventualmente uma coluna.

## I15. Esquema só no Flyway

**Decisão.** `spring.jpa.hibernate.ddl-auto: none`. O esquema, com chaves, `CHECK`s e `UNIQUE`s, é
definido apenas em `V1__schema.sql`.

**Motivo.** As restrições do banco fazem parte das regras (I6). Se o Hibernate gerasse o esquema, elas
poderiam ficar diferentes entre ambientes.

## I16. Erros em RFC 7807

**Decisão.** O `@RestControllerAdvice` converte exceções em `ProblemDetail` com o campo `codigo`:

| HTTP | Situação | `codigo` |
|---|---|---|
| 400 | JSON malformado ou campo obrigatório ausente (lista em `campos`) | `REQUISICAO_INVALIDA` |
| 400 | Parâmetro de consulta ausente ou mal formatado (data, situação desconhecida) | `REQUISICAO_INVALIDA` |
| 422 | Período de disponibilidade com fim igual ou anterior ao início | `PERIODO_INVALIDO` |
| 404 | Plano, piloto, aeronave, aeroporto, aerovia, vaga ou trecho inexistente | `NAO_ENCONTRADO` |
| 409 | Plano fora de edição | `PLANO_NAO_EDITAVEL` |
| 409 | Cancelar plano não aprovado / voo já executado | `PLANO_NAO_CANCELAVEL` / `VOO_JA_EXECUTADO` |
| 409 | Recurso tomado por aprovação concorrente | `CONFLITO_RESERVA` |
| 422 | Origem igual ao destino, altitude inválida, permanência ≤ 0 | `ORIGEM_IGUAL_DESTINO`, `ALTITUDE_INVALIDA`, `PERMANENCIA_INVALIDA` |
| 422 | Aprovação de plano com violações (lista em `violacoes`) | `PLANO_INVALIDO` |

## I17. Spring Boot 3.5

**Decisão.** O projeto usa Spring Boot **3.5.16** (última da linha 3.x) e Java 21, conforme a stack
definida pelo grupo. O Spring Initializr só gera projetos 4.x hoje, então ele foi usado apenas para obter
o Maven Wrapper; o `pom.xml` fixa o parent em 3.5.16.

O `pom.xml` também passa `-Dnet.bytebuddy.experimental=true` à aplicação e aos testes. Isso só tem efeito
em JDKs mais novos que o suportado oficialmente pelo Byte Buddy (usado pelos proxies do Hibernate) e é
inofensivo no Java 21.

---

## I18. Consulta de disponibilidade

**Decisão.** `GET /aerovias/{id}/altitudes-livres` e `GET /aeroportos/{codigo}/vagas-livres` recebem
`inicio` e `fim`. O período é convertido nos slots de uma hora que ele toca, com a mesma regra da
validação (`SlotHorario.cobrindo`, intervalos semiabertos, I3). Uma altitude ou vaga é **disponível**
quando está livre em todos esses slots. A resposta também lista o que está ocupado, com o slot e o plano
que reservou, para facilitar a demonstração de conflitos.

As reservas do período são lidas com **uma consulta por recurso** (`ReservaRepositoryPort.listarDaAerovia`
e `listarDoAeroporto`) e agrupadas em memória.

**Alternativa.** Reaproveitar `VerificadorDisponibilidade.altitudeOcupada` slot a slot. Daria
11 altitudes × N slots consultas ao banco (ou 115 vagas × N em GRU). A porta nova evita isso, e a
validação continua usando a interface mínima de antes.

**Motivo.** O enunciado (5.3) pede que a consulta retrate o momento e não reserve nada. Por isso os casos
de uso são `readOnly` e não passam por `salvarTodas`. A aprovação continua revalidando por conta própria
(I10), então uma consulta antiga não garante nada.

## I19. Listagem de planos em resumo

**Decisão.** `GET /planos?situacao=` devolve `PlanoResumoResponse` (situação, aeronave, origem, destino,
partida, chegada prevista e quantidades de trechos e reservas). O plano completo continua em
`GET /planos/{id}`. Sem o parâmetro, a listagem traz todos os planos. Uma situação desconhecida responde
400.

**Alternativa.** Devolver o `PlanoResponse` completo de cada plano. Ficaria pesado e repetiria os slots de
cada trecho em toda a listagem.

## I20. Abandono

**Decisão.** `POST /planos/{id}/abandono` chama `PlanoDeVoo.abandonar()`, que só aceita planos
`EM_EDICAO` (senão 409 `PLANO_NAO_EDITAVEL`). Como o plano em edição não tem reservas, nada é liberado.
O plano é carregado com bloqueio (I7), para que uma aprovação simultânea não termine depois do abandono.
O plano abandonado continua consultável e não pode mais ser alterado, validado nem aprovado.

**Alternativa.** `DELETE /planos/{id}`. Foi descartado porque o plano não é apagado: muda de situação
e permanece consultável (6.6 e 6.7). Por isso é uma transição de estado, com o mesmo formato do cancelamento.

## I21. Planos abandonado e cancelado na carga inicial

**Decisão.** O `DadosIniciaisRunner` cria também o plano 8, abandonado, e o plano 9, aprovado e depois
cancelado, sempre pelos casos de uso (I5).

**Motivo.** Assim a listagem por situação mostra as quatro situações logo na primeira execução, e o
plano 9 mostra que o cancelamento libera as reservas. Se a partida já tiver passado quando o banco é
criado, o plano 9 fica aprovado, em vez de impedir a aplicação de subir.

## I22. Aprovação sem exigir partida futura

**Decisão.** A aprovação não recusa planos cuja partida já passou. Só o **cancelamento** depende do
relógio (6.8).

**Motivo.** O enunciado restringe o cancelamento ao horário da partida, mas não a aprovação. Aprovar um
plano no passado equivale a registrar um voo já realizado: ele ocupa os recursos daquele horário e
nunca mais pode ser cancelado. É assim que a carga inicial cria o plano 6, que demonstra a regra
"aprovado com partida já ocorrida não pode ser cancelado" passando pelos próprios casos de uso (I5).

**Alternativa.** Uma violação `PARTIDA_NO_PASSADO` na validação. Ela impediria criar o plano 6 pelos casos
de uso e acrescentaria uma regra que o enunciado não pede. Se o professor a pedir, basta uma verificação
em `PlanoDeVoo.validar`, que já recebe as dependências por parâmetro, mais o `Clock`.

