package br.pucrs.projarc.gestaoplanosvoo.infrastructure.persistence.adapters;

import br.pucrs.projarc.gestaoplanosvoo.domain.model.Aeroporto;
import br.pucrs.projarc.gestaoplanosvoo.domain.model.Aerovia;
import br.pucrs.projarc.gestaoplanosvoo.domain.model.Piloto;
import br.pucrs.projarc.gestaoplanosvoo.domain.model.aeronave.Aeronave;
import br.pucrs.projarc.gestaoplanosvoo.domain.repository.MalhaRepositoryPort;
import br.pucrs.projarc.gestaoplanosvoo.infrastructure.persistence.mappers.MalhaMapper;
import br.pucrs.projarc.gestaoplanosvoo.infrastructure.persistence.repositories.AeronaveJpaRepository;
import br.pucrs.projarc.gestaoplanosvoo.infrastructure.persistence.repositories.AeroportoJpaRepository;
import br.pucrs.projarc.gestaoplanosvoo.infrastructure.persistence.repositories.AeroviaJpaRepository;
import br.pucrs.projarc.gestaoplanosvoo.infrastructure.persistence.repositories.PilotoJpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
@Transactional(readOnly = true)
public class MalhaRepositoryAdapter implements MalhaRepositoryPort {

    private final AeroportoJpaRepository aeroportos;
    private final AeroviaJpaRepository aerovias;
    private final PilotoJpaRepository pilotos;
    private final AeronaveJpaRepository aeronaves;
    private final MalhaMapper mapper;

    public MalhaRepositoryAdapter(AeroportoJpaRepository aeroportos, AeroviaJpaRepository aerovias,
                                  PilotoJpaRepository pilotos, AeronaveJpaRepository aeronaves, MalhaMapper mapper) {
        this.aeroportos = aeroportos;
        this.aerovias = aerovias;
        this.pilotos = pilotos;
        this.aeronaves = aeronaves;
        this.mapper = mapper;
    }

    @Override
    public Optional<Aeroporto> buscarAeroporto(String codigo) {
        return aeroportos.findById(codigo).map(mapper::paraDominio);
    }

    @Override
    public Optional<Aerovia> buscarAerovia(String id) {
        return aerovias.findById(id).map(mapper::paraDominio);
    }

    @Override
    public Optional<Piloto> buscarPiloto(Long id) {
        return pilotos.findById(id).map(mapper::paraDominio);
    }

    @Override
    public Optional<Aeronave> buscarAeronave(String prefixo) {
        return aeronaves.findById(prefixo).map(mapper::paraDominio);
    }
}
