package br.pucrs.projarc.gestaoplanosvoo.infrastructure.persistence.mappers;

import br.pucrs.projarc.gestaoplanosvoo.domain.model.Altitude;
import br.pucrs.projarc.gestaoplanosvoo.domain.model.PlanoDeVoo;
import br.pucrs.projarc.gestaoplanosvoo.domain.model.Pouso;
import br.pucrs.projarc.gestaoplanosvoo.domain.model.Trecho;
import br.pucrs.projarc.gestaoplanosvoo.domain.model.reserva.Reserva;
import br.pucrs.projarc.gestaoplanosvoo.infrastructure.persistence.entities.AeronaveEntity;
import br.pucrs.projarc.gestaoplanosvoo.infrastructure.persistence.entities.AeroportoEntity;
import br.pucrs.projarc.gestaoplanosvoo.infrastructure.persistence.entities.AeroviaEntity;
import br.pucrs.projarc.gestaoplanosvoo.infrastructure.persistence.entities.PilotoEntity;
import br.pucrs.projarc.gestaoplanosvoo.infrastructure.persistence.entities.PlanoVooEntity;
import br.pucrs.projarc.gestaoplanosvoo.infrastructure.persistence.entities.TrechoEntity;
import br.pucrs.projarc.gestaoplanosvoo.infrastructure.persistence.entities.VagaEntity;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

import java.util.List;

/** Converte o agregado PlanoDeVoo de e para as entidades JPA. */
@Component
public class PlanoMapper {

    private final MalhaMapper malha;
    private final EntityManager em;

    public PlanoMapper(MalhaMapper malha, EntityManager em) {
        this.malha = malha;
        this.em = em;
    }

    public PlanoDeVoo paraDominio(PlanoVooEntity e, List<Reserva> reservas) {
        List<Trecho> trechos = e.getTrechos().stream().map(this::paraDominio).toList();
        return PlanoDeVoo.reconstituir(e.getId(),
                malha.paraDominio(e.getPiloto()),
                malha.paraDominio(e.getAeronave()),
                malha.paraDominio(e.getOrigem()),
                malha.paraDominio(e.getDestino()),
                e.getDataHoraPartida(),
                e.getSituacao(),
                trechos,
                reservas,
                e.getVersao());
    }

    private Trecho paraDominio(TrechoEntity t) {
        Pouso pouso = t.getPousoVaga() == null ? null
                : new Pouso(malha.paraDominio(t.getPousoVaga()), t.getPousoPermanenciaMin());
        return new Trecho(t.getOrdem(), malha.paraDominio(t.getAerovia()), new Altitude(t.getAltitudePes()),
                t.getHorarioEntrada(), pouso);
    }

    /** Copia o estado do domínio para a entidade. O percurso é regravado por inteiro. */
    public void copiar(PlanoDeVoo plano, PlanoVooEntity e) {
        e.setPiloto(em.getReference(PilotoEntity.class, plano.getPiloto().getId()));
        e.setAeronave(em.getReference(AeronaveEntity.class, plano.getAeronave().getPrefixo()));
        e.setOrigem(em.getReference(AeroportoEntity.class, plano.getOrigem().getCodigo()));
        e.setDestino(em.getReference(AeroportoEntity.class, plano.getDestino().getCodigo()));
        e.setDataHoraPartida(plano.getDataHoraPartida());
        e.setSituacao(plano.getSituacao());
        e.getTrechos().clear();
        for (Trecho t : plano.getTrechos()) {
            VagaEntity vaga = t.getPouso().map(p -> em.getReference(VagaEntity.class, p.vaga().getId())).orElse(null);
            Integer permanencia = t.getPouso().map(Pouso::permanenciaMin).orElse(null);
            e.getTrechos().add(new TrechoEntity(e, t.getOrdem(), em.getReference(AeroviaEntity.class, t.getAerovia().getId()),
                    t.getAltitude().pes(), t.getHorarioEntrada(), vaga, permanencia));
        }
    }
}
