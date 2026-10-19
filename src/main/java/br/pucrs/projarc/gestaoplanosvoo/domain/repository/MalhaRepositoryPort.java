package br.pucrs.projarc.gestaoplanosvoo.domain.repository;

import br.pucrs.projarc.gestaoplanosvoo.domain.model.Aeroporto;
import br.pucrs.projarc.gestaoplanosvoo.domain.model.Aerovia;
import br.pucrs.projarc.gestaoplanosvoo.domain.model.Piloto;
import br.pucrs.projarc.gestaoplanosvoo.domain.model.aeronave.Aeronave;

import java.util.List;
import java.util.Optional;

public interface MalhaRepositoryPort {

    Optional<Aeroporto> buscarAeroporto(String codigo);

    Optional<Aerovia> buscarAerovia(String id);

    Optional<Piloto> buscarPiloto(Long id);

    Optional<Aeronave> buscarAeronave(String prefixo);

    List<Aeroporto> listarAeroportos();

    List<Aerovia> listarAerovias();

    List<Piloto> listarPilotos();

    List<Aeronave> listarAeronaves();
}
