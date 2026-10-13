# Trabalho Final --- Sistema de Gestão de Planos de Voo

## 1. Objetivo e visão geral

O objetivo deste trabalho é projetar e implementar o **backend de um
sistema para criação, validação e gerenciamento de planos de voo**. O
sistema deverá manter informações sobre pilotos, aeronaves, aeroportos e
aerovias, além de controlar a utilização dos recursos necessários à
realização dos voos.

Uma **aerovia** representa uma ligação aérea orientada entre dois
aeroportos. Cada aerovia possui um aeroporto de origem, um aeroporto de
destino e uma distância. Como as ligações são orientadas, uma aerovia
que permite o deslocamento do aeroporto A para o aeroporto B não implica
a existência de uma ligação no sentido contrário.

Um voo pode utilizar uma única aerovia ou uma **sequência de aerovias**.
Nesse último caso, o destino de uma aerovia deve corresponder à origem
da aerovia seguinte. Ao chegar a um aeroporto intermediário, a aeronave
poderá apenas sobrevoá-lo ou realizar um pouso antes de prosseguir
viagem.

Durante a utilização de uma aerovia, a aeronave deverá ocupar uma
determinada **altitude de voo**. Uma mesma altitude de uma mesma aerovia
não poderá ser utilizada simultaneamente por mais de uma aeronave.
Quando houver pouso, também deverá ser reservada uma **vaga de
estacionamento** no aeroporto durante o período em que a aeronave
permanecer em solo.

A possibilidade de realizar um voo depende ainda das características da
aeronave utilizada. Entre outros aspectos, devem ser considerados sua
velocidade de cruzeiro, sua autonomia e as restrições de operação
associadas à sua categoria. Quando ocorrer um pouso durante o trajeto,
considera-se que a aeronave poderá ser reabastecida antes de prosseguir
viagem.

A partir dessas informações, o usuário deverá poder construir
progressivamente um plano de voo, indicando as aerovias que serão
percorridas, as altitudes utilizadas e o que ocorrerá nos aeroportos
encontrados ao longo do percurso. O sistema deverá verificar a
consistência do percurso e dos horários, as restrições da aeronave, sua
autonomia e a disponibilidade dos recursos necessários antes de aprovar
o plano.

Como parte do trabalho, os grupos deverão **modelar o domínio do
problema e definir uma arquitetura de software adequada para o
sistema**, identificando os elementos relevantes, suas responsabilidades
e relacionamentos. A solução arquitetural adotada deverá ser
posteriormente refletida na implementação do backend e justificada pelo
grupo.

O escopo deste trabalho está restrito ao **backend do sistema**. Não faz
parte do trabalho o desenvolvimento de uma interface gráfica ou
aplicação frontend. As funcionalidades do sistema deverão ser
disponibilizadas por meio de uma **API REST**, permitindo que aplicações
clientes utilizem os serviços oferecidos pelo backend.

------------------------------------------------------------------------

## 2. Aeroportos, aerovias e rede aérea

O espaço aéreo considerado pelo sistema é representado por um conjunto
de **aeroportos** interligados por **aerovias**.

Cada aeroporto possui um **código que o identifica de forma única**, um
nome e uma determinada quantidade de vagas para estacionamento de
aeronaves. Essas vagas serão utilizadas quando um plano de voo
determinar que uma aeronave deve pousar e permanecer no aeroporto.

Para fins deste trabalho, serão adotadas quantidades simplificadas de
vagas de estacionamento. Não é necessário considerar diferenças entre
posições decorrentes do porte ou do tipo da aeronave: **qualquer vaga
cadastrada no sistema poderá receber qualquer uma das aeronaves
consideradas neste trabalho**.

  Código   Aeroporto                                                  Vagas
  -------- -------------------------------------------------------- -------
  POA      Aeroporto Internacional Salgado Filho --- Porto Alegre        41
  FLN      Aeroporto Internacional de Florianópolis/Hercílio Luz         16
  CWB      Aeroporto Internacional Afonso Pena --- Curitiba              26
  GRU      Aeroporto Internacional de São Paulo/Guarulhos               115

Uma aerovia representa uma ligação **orientada** entre dois aeroportos e
possui um identificador único, um aeroporto de origem, um aeroporto de
destino e a distância entre os dois aeroportos, expressa em quilômetros.

Por serem orientadas, as aerovias que ligam dois aeroportos em sentidos
opostos são consideradas aerovias distintas.

Para este trabalho, considere inicialmente a rede aérea apresentada na
tabela a seguir.

  Aerovia   Origem   Destino     Distância (km)
  --------- -------- --------- ----------------
  R1        POA      FLN                    363
  R2        POA      GRU                    865
  R3        GRU      POA                    865
  R4        FLN      CWB                    245
  R5        CWB      FLN                    245
  R6        CWB      GRU                    360
  R7        GRU      CWB                    360
  R8        FLN      POA                    363

Os aeroportos e aerovias apresentados acima constituem a **malha aérea
inicial**. Durante o desenvolvimento do trabalho, **novos aeroportos e
novas aerovias poderão ser informados pelo professor**, devendo ser
possível incorporá-los ao sistema sem alterações na lógica da aplicação.

O sistema deverá permitir consultar os aeroportos e aerovias cadastrados
e cadastrar novos aeroportos e aerovias.

ATENÇÃO: após o cadastro, uma aerovia não poderá ser alterada ou excluída. 
Somente novas aerovias poderão ser criadas, com identificadores únicos que 
não substituam os registros existentes. Isso preserva os dados das aerovias
referenciadas pelos planos e pelas reservas. Uma aerovia que entrar em
desuso permanecerá cadastrada e apenas deixará de ser solicitada pelos
usuários; a gestão desse desuso está fora do escopo do sistema.

A determinação do percurso **não faz parte das responsabilidades do
sistema**. O usuário deverá informar a sequência de aerovias e o sistema
deverá verificar se ela constitui um percurso válido.

------------------------------------------------------------------------

## 3. Aeronaves e regras de operação

As aeronaves possuem um **prefixo único**, velocidade de cruzeiro e
autonomia. Diferentes categorias possuem informações próprias e regras
de operação.

-   **Particulares:** identificação da empresa responsável pela
    manutenção.
-   **Comerciais de passageiros:** companhia aérea e capacidade máxima
    de passageiros.
-   **Comerciais de carga:** companhia aérea e capacidade máxima de
    carga em toneladas.

Regras:

-   aeronave **particular**: somente 25.000, 26.000 e 27.000 pés;
-   aeronave **comercial de passageiros**: altitudes iguais ou
    superiores a 28.000 pés, dentro da faixa permitida;
-   aeronave **comercial de carga**: qualquer altitude prevista, mas
    somente entre **00:00, inclusive, e 06:00, exclusive**; todo o
    período de utilização da aerovia deve estar nesse intervalo.

A velocidade de cruzeiro, em km/h, determina o tempo para percorrer cada
aerovia, considerando velocidade constante.

A autonomia, em quilômetros, determina a distância máxima desde o último
abastecimento. Deve ser considerada margem de segurança de 15%, de modo
que a distância sem reabastecimento não ultrapasse **85% da autonomia
nominal**.

Após um pouso, considera-se que a aeronave é reabastecida antes de
prosseguir. Sobrevoo não reinicia o controle de autonomia.

Durante o desenvolvimento, **novas categorias de aeronaves ou novas
regras de operação poderão ser informadas pelo professor**. A solução
deverá acomodá-las sem modificações extensas nas funcionalidades
existentes.

------------------------------------------------------------------------

## 4. Planos de voo e definição do percurso

Na criação de um **plano de voo** deverão ser informados piloto,
aeronave, aeroporto de origem, aeroporto de destino e data e hora
previstas para a partida. Cada plano terá identificador único.

Os pilotos terão **identificador único e nome**. Não será necessário
considerar habilitações, licenças, jornadas de trabalho ou outras
restrições relacionadas ao piloto.

Um plano nasce **em edição**. Depois de validado e aprovado passa a
**aprovado**. Um plano aprovado poderá ser **cancelado** somente se seu
horário previsto de partida ainda não tiver ocorrido. Um plano em edição
poderá ser **abandonado**.

### 4.1 Construção do percurso

O percurso é uma **sequência ordenada de trechos**. Cada trecho deverá
informar:

-   aerovia;
-   altitude;
-   horário previsto de entrada na aerovia;
-   o que ocorrerá ao chegar ao aeroporto de destino da aerovia.

A sequência deverá ser contínua: origem da primeira aerovia igual à
origem do plano; destino de cada aerovia igual à origem da seguinte;
destino da última igual ao destino final.

Exemplo:

`POA ──R1──> FLN ──R4──> CWB`

O sistema **não deverá determinar automaticamente o percurso**.

O horário de chegada ao destino de cada trecho deverá ser **determinado
pelo sistema**:

**duração do trecho = distância da aerovia / velocidade de cruzeiro da
aeronave**

**horário de chegada = horário de entrada + duração do trecho**

O horário de entrada na primeira aerovia deverá corresponder à partida
do plano. Após **sobrevoo**, a entrada na próxima aerovia deverá
corresponder à chegada calculada. Após **pouso intermediário**, deverá
corresponder à chegada calculada acrescida do tempo de permanência.

### 4.2 Passagem pelos aeroportos

Ao concluir um trecho:

-   **Sobrevoo:** não utiliza vaga e não há reabastecimento; a distância
    continua acumulada.
-   **Pouso:** utiliza vaga durante a permanência e considera-se
    reabastecimento antes de prosseguir.

Nos aeroportos intermediários poderá ocorrer sobrevoo ou pouso. No
**destino final deverá obrigatoriamente ocorrer pouso**.

### 4.3 Pousos e permanência nos aeroportos

Para cada pouso deverão ser informados a vaga pretendida e o tempo de
permanência, em minutos. Isso também se aplica ao **destino final**.

Indicar uma vaga durante a elaboração **não constitui reserva**.

------------------------------------------------------------------------

## 5. Controle de ocupação de aerovias e aeroportos

A aprovação depende da disponibilidade das **altitudes nas aerovias** e
das **vagas nos aeroportos**.

### 5.1 Ocupação de aerovias

As altitudes disponíveis vão de **25.000 a 35.000 pés**, inclusive, em
incrementos de 1.000 pés.

**tempo de voo = distância da aerovia / velocidade de cruzeiro**

A ocupação será controlada em **slots de uma hora alinhados à hora
cheia**. Qualquer utilização parcial ocupa o slot inteiro.

Exemplo: entrada às **15:52** e saída às **17:07** ocupa `15:00–16:00`,
`16:00–17:00` e `17:00–18:00`.

A mesma altitude da mesma aerovia não poderá ser utilizada por duas
aeronaves quando houver coincidência em pelo menos um slot.

### 5.2 Ocupação de aeroportos

Uma vaga é ocupada durante todo o período de permanência. Aplica-se a
mesma regra de slots.

Exemplo: chegada às **15:50** e permanência de **20 minutos** ocupa
`15:00–16:00` e `16:00–17:00`.

A vaga deve estar disponível em **todos os slots necessários**.

### 5.3 Consulta da disponibilidade

Durante a elaboração deverá ser possível consultar, para determinado
período:

-   altitudes disponíveis em uma aerovia;
-   vagas disponíveis em um aeroporto.

A consulta representa a situação **naquele momento** e não reserva
recursos. A aprovação sempre deverá considerar a disponibilidade atual.

### 5.4 Reserva e liberação

Planos **em edição** não reservam recursos.

As reservas são efetivadas somente na aprovação, que deverá ser
integral: ou todos os recursos são reservados e o plano é aprovado, ou
nenhuma reserva da tentativa permanece e o plano continua em edição.

Quando um **plano aprovado futuro** for cancelado, suas reservas deverão
ser liberadas. Abandonar plano em edição não exige liberação.

### 5.5 Limite diário

Todos os voos deverão **iniciar e terminar no mesmo dia**, isto é, a
chegada ao destino final deverá ocorrer na data da partida.

A permanência no destino final poderá **estender-se para o dia
seguinte**, e a reserva da vaga deverá considerar todos os slots
necessários.

------------------------------------------------------------------------

## 6. Validação, aprovação e gerenciamento dos planos de voo

O sistema deverá permitir criar, editar, validar, aprovar, consultar,
abandonar e cancelar planos, respeitando seu estado e as regras do
enunciado.

### 6.1 Criação e edição

Todo plano recebe identificador único e nasce **em edição**. Enquanto
estiver em edição poderá ter percurso, altitudes, pousos, sobrevoos e
permanências alterados. Alterações não reservam recursos.

### 6.2 Validação do percurso e dos horários

A validação deverá verificar, pelo menos:

-   existência de um ou mais trechos;
-   coerência entre origem do plano e primeira aerovia;
-   coerência entre destino do plano e última aerovia;
-   continuidade entre aerovias consecutivas;
-   correspondência entre entrada na primeira aerovia e partida do
    plano;
-   duração e chegada de cada trecho calculadas a partir da distância e
    velocidade;
-   após sobrevoo, correspondência entre chegada e entrada na próxima
    aerovia;
-   após pouso intermediário, correspondência entre chegada +
    permanência e entrada na próxima aerovia;
-   pouso obrigatório no destino final;
-   início e término do voo na mesma data.

O sistema não deverá procurar ou propor rota alternativa.

### 6.3 Validação da aeronave e da autonomia

Para cada trecho deverá ser verificada a possibilidade de operação da
aeronave na altitude e durante todo o intervalo previsto, conforme sua
categoria.

A distância desde o último abastecimento não poderá ultrapassar **85% da
autonomia nominal**. Sobrevoos acumulam distância; pousos reiniciam esse
controle após o reabastecimento.

### 6.4 Validação da disponibilidade

Para cada trecho deverão ser determinados os slots de aerovia
necessários, e a altitude deverá estar disponível em todos eles.

Para cada pouso, a vaga deverá estar disponível durante todos os slots
da permanência.

A validação considera a disponibilidade **no momento em que é
realizada**.

### 6.5 Validação e aprovação

**Validar** e **aprovar** são operações distintas.

Validar não altera o estado nem reserva recursos. Um plano em edição
pode ser validado, alterado e novamente validado.

A aprovação deverá verificar novamente as condições necessárias ou
garantir que continuem válidas no momento das reservas.

Um plano somente será aprovado se **todas as regras forem satisfeitas e
todos os recursos puderem ser reservados**.

A aprovação será integral: ou todas as reservas são efetivadas e o plano
passa a **aprovado**, ou nenhuma reserva da tentativa permanece e o
plano continua **em edição**.

### 6.6 Consulta dos planos

Deverá ser possível consultar um plano por seu identificador e consultar
planos por situação: em edição, aprovados, cancelados ou abandonados.

Planos cancelados e abandonados permanecem disponíveis para consulta.

### 6.7 Abandono

Um plano **em edição** poderá ser abandonado. Passará a **abandonado** e
não poderá mais ser alterado, validado ou aprovado. Como não possuía
reservas, não há recursos a liberar.

### 6.8 Cancelamento

Somente um plano **aprovado cujo horário previsto de partida ainda não
tenha ocorrido** poderá ser cancelado.

Para este trabalho, um plano aprovado cuja data e hora de partida já
tenham passado corresponde a um voo que **já foi executado**. Ele
permanece disponível para consulta, mas não pode mais ser cancelado.

O cancelamento de um plano futuro muda sua situação para **cancelado** e
libera todas as reservas realizadas na aprovação.

Uma vez **aprovado, abandonado ou cancelado**, o plano deverá preservar
as informações que possuía ao atingir essa situação. **Somente planos em
edição poderão ser modificados.**

------------------------------------------------------------------------

## 7. Projeto e Arquitetura da Solução

O trabalho deverá contemplar o **projeto da solução e a definição de sua
arquitetura de software**. A equipe deverá identificar os elementos
relevantes do domínio e definir a organização das responsabilidades de
forma coerente com os requisitos.

### 7.1 Modelo de domínio

A equipe deverá elaborar um **modelo de domínio** com os principais
conceitos, características, relacionamentos e regras relevantes.

**Não será fornecido um modelo de classes de referência.**

As principais decisões de modelagem deverão ser justificadas. O modelo
deverá ser documentado em **UML**, com detalhe suficiente para
compreender a estrutura proposta.

### 7.2 Arquitetura da solução

A equipe deverá definir a arquitetura do backend. O **estilo
arquitetural** e a organização em módulos, camadas, componentes ou
outros elementos são decisões da equipe.

A arquitetura deverá considerar:

-   separação de responsabilidades;
-   organização das regras de negócio;
-   acesso e persistência;
-   facilidade de verificar o comportamento;
-   evolução do sistema;
-   incorporação de novos aeroportos, aerovias, categorias e regras;
-   consistência de operações que envolvem múltiplas alterações.

A arquitetura deverá contemplar o **backend e os elementos necessários
ao seu funcionamento**, incluindo API REST e persistência. Clientes
podem aparecer nos diagramas, mas não fazem parte do escopo de
implementação.

A implementação deverá ser coerente com a arquitetura proposta.

### 7.3 Persistência dos dados

As informações deverão ser **persistidas em banco de dados**.
Persistência exclusivamente em memória ou CSV não atende ao trabalho.

Devem ser persistidos os dados necessários à recuperação do estado após
reinicialização, incluindo rede aérea, pilotos, aeronaves, planos e
reservas.

A tecnologia de banco e a integração com a persistência são decisões de
projeto da equipe, observadas as restrições tecnológicas do enunciado.

### 7.4 Documentação da arquitetura

A arquitetura deverá ser documentada com o **modelo C4**, incluindo no
mínimo:

-   **Contexto (Nível 1):** sistema, usuários e sistemas externos;
-   **Contêineres (Nível 2):** principais elementos executáveis e/ou
    armazenadores de dados, responsabilidades, tecnologias e
    comunicação;
-   **Componentes (Nível 3):** organização interna dos contêineres
    relevantes, principais componentes, responsabilidades e
    dependências.

Não é necessário C4 de código (Nível 4), pois o modelo de domínio em UML
deverá fornecer informações sobre os principais elementos do domínio.

Os diagramas deverão representar a arquitetura efetivamente adotada e
permanecer **consistentes com a implementação**.

As principais decisões arquiteturais deverão ser documentadas e
justificadas. Quando houver alternativas relevantes, deverão ser
registradas a **decisão adotada, as principais alternativas consideradas
e a justificativa da escolha**. Não é exigido formato específico para
esse registro.

------------------------------------------------------------------------

## 8. Requisitos de implementação e entrega

O sistema deverá ser implementado como **backend em Java e Spring
Boot**, conforme o modelo e a arquitetura definidos pela equipe.

Não haverá frontend obrigatório. As funcionalidades deverão ser
disponibilizadas por **API REST**.

### 8.1 Requisitos de implementação

A solução deverá permitir:

-   consultar a rede aérea e cadastrar novos aeroportos e aerovias;
-   consultar pilotos e aeronaves e cadastrar os dados necessários;
-   criar e modificar planos em edição;
-   definir percursos;
-   consultar disponibilidade de altitudes e vagas;
-   validar planos;
-   aprovar planos e efetivar reservas;
-   consultar planos e situações;
-   abandonar planos em edição;
-   cancelar **planos aprovados futuros**, liberando as reservas.

A API REST deverá permitir demonstrar e verificar essas funcionalidades.

A utilização de Java e Spring Boot não determina a arquitetura interna.
Módulos, camadas, componentes, dependências e responsabilidades são
decisões da equipe.

### 8.2 Dados iniciais e persistência

Ao iniciar pela primeira vez, deverão existir dados suficientes para
demonstração: malha aérea inicial, pilotos e aeronaves das diferentes
categorias.

Também deverá existir um **conjunto inicial de planos aprovados e
respectivas reservas**.

Para uma **data de referência**, pelo menos **60% das aerovias da malha
inicial deverão possuir alguma ocupação**. Com oito aerovias, **pelo
menos cinco** deverão possuir alguma ocupação nessa data.

As ocupações deverão ser consequência dos **planos aprovados**
existentes na base.

A data de referência deverá estar claramente identificada e será
utilizada durante a apresentação para testes de validação,
disponibilidade e reserva.

Os dados deverão permitir tanto recursos disponíveis quanto conflitos;
não deverão tornar todos os recursos indisponíveis durante toda a data.

A carga inicial deverá ser **reproduzível a partir dos artefatos
entregues**, sem inclusão manual prévia à apresentação.

As alterações realizadas pela API REST deverão persistir após
reinicialização.

### 8.3 Funcionalidades e situações que deverão ser demonstradas

A solução deverá demonstrar o ciclo de um plano desde criação e percurso
até validação e eventual aprovação, além de falhas como percurso
inconsistente, **horários incompatíveis com a duração calculada**,
restrições da aeronave, autonomia insuficiente, conflitos em aerovias e
indisponibilidade de vagas.

Também deverá ser possível verificar que:

-   validação não efetiva reservas;
-   aprovação malsucedida não deixa reservas parciais;
-   aprovação considera a disponibilidade no momento da operação;
-   plano aprovado futuro pode ser cancelado, liberando reservas;
-   plano aprovado cuja partida já ocorreu não pode ser cancelado;
-   plano em edição pode ser abandonado;
-   planos aprovados, cancelados e abandonados permanecem consultáveis;
-   somente planos em edição podem ser modificados.

As demonstrações de disponibilidade e conflito deverão utilizar a **data
de referência**.

### 8.4 Critérios de avaliação

Serão considerados:

-   atendimento aos requisitos e regras de negócio;
-   qualidade e consistência do **modelo de domínio**;
-   adequação do **estilo arquitetural** escolhido;
-   separação e distribuição de responsabilidades;
-   coerência entre domínio, arquitetura, documentação e implementação;
-   qualidade e consistência dos **diagramas C4**;
-   qualidade da **API REST**, inclusive situações de sucesso e erro;
-   utilização adequada do banco e persistência;
-   consistência de operações com múltiplas alterações, especialmente
    aprovação e cancelamento;
-   capacidade de acomodar as evoluções previstas;
-   clareza, organização e manutenibilidade do código;
-   facilidade de verificar o comportamento pela API;
-   qualidade da documentação e justificativas arquiteturais.

Não será exigido estilo arquitetural específico. A equipe deverá
**explicar e justificar suas decisões** em relação aos requisitos.

### 8.5 Entrega e apresentação

A entrega será por **repositório Git**, contendo no mínimo:

-   código-fonte do backend Java/Spring Boot;
-   arquivos para configurar e executar o banco;
-   mecanismos para carregar os **dados iniciais**;
-   **modelo de domínio em UML**;
-   **diagramas C4**;
-   documentação das principais decisões de projeto e arquitetura;
-   `README.md` com instruções de preparação e execução.

A API REST deverá possuir documentação de operações, parâmetros,
requisições e respostas, por **OpenAPI ou mecanismo equivalente**.

O `README.md` deverá explicar como preparar o ambiente, inicializar o
banco, carregar dados, compilar e executar a aplicação, acessar a API e
identificar a **data de referência**.

A solução deverá ser **reproduzível a partir do repositório**, sem
depender de configurações ou dados exclusivos do ambiente da equipe.

A documentação arquitetural deverá corresponder à versão entregue.

#### Apresentação do trabalho

Durante a apresentação, a equipe deverá demonstrar o backend realizando
**requisições HTTP à API REST**.

Poderá ser utilizada **qualquer ferramenta** para essas requisições:
cliente HTTP, linha de comando, interface da documentação da API ou
equivalente. Não é necessário desenvolver interface específica.

A demonstração deverá utilizar a **data de referência e os dados
iniciais**, cobrindo validação, disponibilidade, aprovação, reserva,
conflito, abandono e cancelamento.

A equipe deverá estar preparada para realizar **requisições indicadas
pelo professor** durante a apresentação, permitindo verificar o
comportamento em situações diferentes de uma sequência previamente
preparada.

As demais regras de entrega, prazo, submissão, grupos e apresentação
serão informadas separadamente.
