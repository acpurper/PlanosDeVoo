-- Malha aérea inicial (enunciado, seção 2). Novos aeroportos e aerovias entram como novas migrações.

INSERT INTO aeroporto (codigo, nome) VALUES
    ('POA', 'Aeroporto Internacional Salgado Filho — Porto Alegre'),
    ('FLN', 'Aeroporto Internacional de Florianópolis/Hercílio Luz'),
    ('CWB', 'Aeroporto Internacional Afonso Pena — Curitiba'),
    ('GRU', 'Aeroporto Internacional de São Paulo/Guarulhos');

-- Vagas numeradas de 1 até a quantidade de cada aeroporto.
INSERT INTO vaga (aeroporto_codigo, numero) SELECT 'POA', n FROM generate_series(1, 41) AS n;
INSERT INTO vaga (aeroporto_codigo, numero) SELECT 'FLN', n FROM generate_series(1, 16) AS n;
INSERT INTO vaga (aeroporto_codigo, numero) SELECT 'CWB', n FROM generate_series(1, 26) AS n;
INSERT INTO vaga (aeroporto_codigo, numero) SELECT 'GRU', n FROM generate_series(1, 115) AS n;

INSERT INTO aerovia (id, origem, destino, distancia_km) VALUES
    ('R1', 'POA', 'FLN', 363),
    ('R2', 'POA', 'GRU', 865),
    ('R3', 'GRU', 'POA', 865),
    ('R4', 'FLN', 'CWB', 245),
    ('R5', 'CWB', 'FLN', 245),
    ('R6', 'CWB', 'GRU', 360),
    ('R7', 'GRU', 'CWB', 360),
    ('R8', 'FLN', 'POA', 363);
