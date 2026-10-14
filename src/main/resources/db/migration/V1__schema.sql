-- Esquema do Sistema de Gestão de Planos de Voo.
-- As restrições UNIQUE das reservas garantem que uma altitude de uma aerovia e uma vaga
-- tenham no máximo um plano por slot, inclusive com aprovações concorrentes.

CREATE TABLE aeroporto (
    codigo  VARCHAR(10)  PRIMARY KEY,
    nome    VARCHAR(200) NOT NULL
);

CREATE TABLE vaga (
    id                BIGSERIAL   PRIMARY KEY,
    aeroporto_codigo  VARCHAR(10) NOT NULL REFERENCES aeroporto (codigo),
    numero            INT         NOT NULL CHECK (numero > 0),
    CONSTRAINT uk_vaga_aeroporto_numero UNIQUE (aeroporto_codigo, numero)
);

-- Aerovia é imutável: o código não oferece alteração nem exclusão, e ela é referenciada por id.
CREATE TABLE aerovia (
    id           VARCHAR(20)   PRIMARY KEY,
    origem       VARCHAR(10)   NOT NULL REFERENCES aeroporto (codigo),
    destino      VARCHAR(10)   NOT NULL REFERENCES aeroporto (codigo),
    distancia_km NUMERIC(10,2) NOT NULL CHECK (distancia_km > 0),
    CONSTRAINT ck_aerovia_orientada CHECK (origem <> destino)
);

CREATE TABLE piloto (
    id    BIGSERIAL    PRIMARY KEY,
    nome  VARCHAR(200) NOT NULL
);

-- Uma tabela para a hierarquia de aeronaves (single table), discriminada pela categoria.
CREATE TABLE aeronave (
    prefixo                  VARCHAR(10)   PRIMARY KEY,
    categoria                VARCHAR(30)   NOT NULL,
    velocidade_cruzeiro_kmh  NUMERIC(10,2) NOT NULL CHECK (velocidade_cruzeiro_kmh > 0),
    autonomia_km             NUMERIC(10,2) NOT NULL CHECK (autonomia_km > 0),
    empresa_manutencao       VARCHAR(200),
    companhia_aerea          VARCHAR(200),
    capacidade_passageiros   INT,
    capacidade_toneladas     NUMERIC(10,2)
);

CREATE TABLE plano_voo (
    id                 BIGSERIAL   PRIMARY KEY,
    piloto_id          BIGINT      NOT NULL REFERENCES piloto (id),
    aeronave_prefixo   VARCHAR(10) NOT NULL REFERENCES aeronave (prefixo),
    origem             VARCHAR(10) NOT NULL REFERENCES aeroporto (codigo),
    destino            VARCHAR(10) NOT NULL REFERENCES aeroporto (codigo),
    data_hora_partida  TIMESTAMP   NOT NULL,
    situacao           VARCHAR(20) NOT NULL CHECK (situacao IN ('EM_EDICAO', 'APROVADO', 'CANCELADO', 'ABANDONADO')),
    versao             BIGINT      NOT NULL DEFAULT 0
);
CREATE INDEX ix_plano_situacao ON plano_voo (situacao);

CREATE TABLE trecho (
    id                     BIGSERIAL   PRIMARY KEY,
    plano_id               BIGINT      NOT NULL REFERENCES plano_voo (id) ON DELETE CASCADE,
    ordem                  INT         NOT NULL CHECK (ordem > 0),
    aerovia_id             VARCHAR(20) NOT NULL REFERENCES aerovia (id),
    altitude_pes           INT         NOT NULL CHECK (altitude_pes BETWEEN 25000 AND 35000 AND altitude_pes % 1000 = 0),
    horario_entrada        TIMESTAMP   NOT NULL,
    pouso_vaga_id          BIGINT      REFERENCES vaga (id),
    pouso_permanencia_min  INT         CHECK (pouso_permanencia_min > 0),
    -- sem pouso = sobrevoo; com pouso, vaga e permanência vêm juntas
    CONSTRAINT ck_trecho_pouso CHECK ((pouso_vaga_id IS NULL) = (pouso_permanencia_min IS NULL))
);
CREATE INDEX ix_trecho_plano ON trecho (plano_id);

CREATE TABLE reserva_aerovia (
    id            BIGSERIAL   PRIMARY KEY,
    plano_id      BIGINT      NOT NULL REFERENCES plano_voo (id),
    aerovia_id    VARCHAR(20) NOT NULL REFERENCES aerovia (id),
    altitude_pes  INT         NOT NULL,
    slot_inicio   TIMESTAMP   NOT NULL CHECK (slot_inicio = date_trunc('hour', slot_inicio)),
    CONSTRAINT uk_reserva_aerovia_slot UNIQUE (aerovia_id, altitude_pes, slot_inicio)
);
CREATE INDEX ix_reserva_aerovia_plano ON reserva_aerovia (plano_id);

CREATE TABLE reserva_vaga (
    id           BIGSERIAL PRIMARY KEY,
    plano_id     BIGINT    NOT NULL REFERENCES plano_voo (id),
    vaga_id      BIGINT    NOT NULL REFERENCES vaga (id),
    slot_inicio  TIMESTAMP NOT NULL CHECK (slot_inicio = date_trunc('hour', slot_inicio)),
    CONSTRAINT uk_reserva_vaga_slot UNIQUE (vaga_id, slot_inicio)
);
CREATE INDEX ix_reserva_vaga_plano ON reserva_vaga (plano_id);
