package br.pucrs.projarc.gestaoplanosvoo.infrastructure.persistence.mappers;

import br.pucrs.projarc.gestaoplanosvoo.domain.model.Aeroporto;
import br.pucrs.projarc.gestaoplanosvoo.domain.model.Aerovia;
import br.pucrs.projarc.gestaoplanosvoo.domain.model.Piloto;
import br.pucrs.projarc.gestaoplanosvoo.domain.model.Vaga;
import br.pucrs.projarc.gestaoplanosvoo.domain.model.aeronave.Aeronave;
import br.pucrs.projarc.gestaoplanosvoo.domain.model.aeronave.AeronaveCarga;
import br.pucrs.projarc.gestaoplanosvoo.domain.model.aeronave.AeronaveParticular;
import br.pucrs.projarc.gestaoplanosvoo.domain.model.aeronave.AeronavePassageiros;
import br.pucrs.projarc.gestaoplanosvoo.infrastructure.persistence.entities.AeronaveCargaEntity;
import br.pucrs.projarc.gestaoplanosvoo.infrastructure.persistence.entities.AeronaveEntity;
import br.pucrs.projarc.gestaoplanosvoo.infrastructure.persistence.entities.AeronaveParticularEntity;
import br.pucrs.projarc.gestaoplanosvoo.infrastructure.persistence.entities.AeronavePassageirosEntity;
import br.pucrs.projarc.gestaoplanosvoo.infrastructure.persistence.entities.AeroportoEntity;
import br.pucrs.projarc.gestaoplanosvoo.infrastructure.persistence.entities.AeroviaEntity;
import br.pucrs.projarc.gestaoplanosvoo.infrastructure.persistence.entities.PilotoEntity;
import br.pucrs.projarc.gestaoplanosvoo.infrastructure.persistence.entities.VagaEntity;
import org.springframework.stereotype.Component;

/** Converte as entidades JPA da malha e da frota em objetos de domínio. */
@Component
public class MalhaMapper {

    public Aeroporto paraDominio(AeroportoEntity e) {
        return new Aeroporto(e.getCodigo(), e.getNome(), e.getVagas().stream().map(this::paraDominio).toList());
    }

    public Vaga paraDominio(VagaEntity e) {
        return new Vaga(e.getId(), e.getAeroportoCodigo(), e.getNumero());
    }

    public Aerovia paraDominio(AeroviaEntity e) {
        return new Aerovia(e.getId(), paraDominio(e.getOrigem()), paraDominio(e.getDestino()), e.getDistanciaKm());
    }

    public Piloto paraDominio(PilotoEntity e) {
        return new Piloto(e.getId(), e.getNome());
    }

    /** Uma categoria nova exige um ramo aqui, além da subclasse de domínio e da entidade. */
    public Aeronave paraDominio(AeronaveEntity e) {
        return switch (e) {
            case AeronaveParticularEntity p -> new AeronaveParticular(
                    p.getPrefixo(), p.getVelocidadeCruzeiroKmh(), p.getAutonomiaKm(), p.getEmpresaManutencao());
            case AeronavePassageirosEntity p -> new AeronavePassageiros(
                    p.getPrefixo(), p.getVelocidadeCruzeiroKmh(), p.getAutonomiaKm(),
                    p.getCompanhiaAerea(), p.getCapacidadePassageiros());
            case AeronaveCargaEntity c -> new AeronaveCarga(
                    c.getPrefixo(), c.getVelocidadeCruzeiroKmh(), c.getAutonomiaKm(),
                    c.getCompanhiaAerea(), c.getCapacidadeToneladas());
            default -> throw new IllegalStateException("Categoria de aeronave sem mapeamento: " + e.getClass());
        };
    }
}
