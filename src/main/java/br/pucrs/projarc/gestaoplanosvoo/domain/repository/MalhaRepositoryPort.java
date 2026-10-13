package br.pucrs.projarc.gestaoplanosvoo.domain.repository;

import br.pucrs.projarc.gestaoplanosvoo.domain.model.Aeroporto;
import br.pucrs.projarc.gestaoplanosvoo.domain.model.Aerovia;
import br.pucrs.projarc.gestaoplanosvoo.domain.model.Piloto;
import br.pucrs.projarc.gestaoplanosvoo.domain.model.aeronave.Aeronave;

import java.util.Optional;

/** Consulta dos dados pré-existentes (carregados por script): malha aérea, pilotos e frota. */
public interface MalhaRepositoryPort {

    Optional<Aeroporto> buscarAeroporto(String codigo);

    Optional<Aerovia> buscarAerovia(String id);

    Optional<Piloto> buscarPiloto(Long id);

    Optional<Aeronave> buscarAeronave(String prefixo);
}
