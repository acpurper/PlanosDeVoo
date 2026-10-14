-- Pilotos e frota de demonstração: duas aeronaves por categoria.
-- Os números foram escolhidos para gerar casos de sucesso e de falha (ver docs/demo.http).

INSERT INTO piloto (nome) VALUES
    ('Carlos Andrade'),
    ('Fernanda Lima'),
    ('Rafael Costa'),
    ('Juliana Prates');

INSERT INTO aeronave (prefixo, categoria, velocidade_cruzeiro_kmh, autonomia_km,
                      empresa_manutencao, companhia_aerea, capacidade_passageiros, capacidade_toneladas) VALUES
    -- Particular lenta e de autonomia curta: limite seguro de 595 km.
    -- POA->FLN (363 km) passa; POA->FLN->CWB em sobrevoo (608 km) e POA->GRU (865 km) estouram.
    ('PP-LEN', 'PARTICULAR',  400, 700,  'Hangar Sul Manutenção', NULL, NULL, NULL),
    ('PP-JAT', 'PARTICULAR',  600, 1500, 'Aero Manutenção Gaúcha', NULL, NULL, NULL),
    ('PR-AZA', 'PASSAGEIROS', 850, 5000, NULL, 'Aerosul Linhas Aéreas', 186, NULL),
    ('PR-GOB', 'PASSAGEIROS', 830, 4000, NULL, 'Voa Brasil', 174, NULL),
    ('PR-CGA', 'CARGA',       780, 3500, NULL, 'Cargo Sul', NULL, 20),
    -- Carga com autonomia de 1.000 km: limite seguro de 850 km, não faz POA->GRU (865 km) direto.
    ('PR-CGB', 'CARGA',       700, 1000, NULL, 'Cargo Sul', NULL, 12);
