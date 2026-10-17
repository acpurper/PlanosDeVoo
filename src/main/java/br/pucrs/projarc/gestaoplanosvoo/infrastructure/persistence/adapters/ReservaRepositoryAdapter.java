package br.pucrs.projarc.gestaoplanosvoo.infrastructure.persistence.adapters;

import br.pucrs.projarc.gestaoplanosvoo.domain.exception.ConflitoReservaException;
import br.pucrs.projarc.gestaoplanosvoo.domain.model.Altitude;
import br.pucrs.projarc.gestaoplanosvoo.domain.model.SlotHorario;
import br.pucrs.projarc.gestaoplanosvoo.domain.model.Vaga;
import br.pucrs.projarc.gestaoplanosvoo.domain.model.reserva.Reserva;
import br.pucrs.projarc.gestaoplanosvoo.domain.model.reserva.ReservaAerovia;
import br.pucrs.projarc.gestaoplanosvoo.domain.model.reserva.ReservaVaga;
import br.pucrs.projarc.gestaoplanosvoo.domain.repository.ReservaRepositoryPort;
import br.pucrs.projarc.gestaoplanosvoo.infrastructure.persistence.entities.AeroviaEntity;
import br.pucrs.projarc.gestaoplanosvoo.infrastructure.persistence.entities.ReservaAeroviaEntity;
import br.pucrs.projarc.gestaoplanosvoo.infrastructure.persistence.entities.ReservaVagaEntity;
import br.pucrs.projarc.gestaoplanosvoo.infrastructure.persistence.entities.VagaEntity;
import br.pucrs.projarc.gestaoplanosvoo.infrastructure.persistence.mappers.MalhaMapper;
import br.pucrs.projarc.gestaoplanosvoo.infrastructure.persistence.repositories.ReservaAeroviaJpaRepository;
import br.pucrs.projarc.gestaoplanosvoo.infrastructure.persistence.repositories.ReservaVagaJpaRepository;
import jakarta.persistence.EntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Component
public class ReservaRepositoryAdapter implements ReservaRepositoryPort {

    private final ReservaAeroviaJpaRepository reservasAerovia;
    private final ReservaVagaJpaRepository reservasVaga;
    private final MalhaMapper mapper;
    private final EntityManager em;

    public ReservaRepositoryAdapter(ReservaAeroviaJpaRepository reservasAerovia, ReservaVagaJpaRepository reservasVaga,
                                    MalhaMapper mapper, EntityManager em) {
        this.reservasAerovia = reservasAerovia;
        this.reservasVaga = reservasVaga;
        this.mapper = mapper;
        this.em = em;
    }

    @Override
    public boolean altitudeOcupada(String aeroviaId, Altitude altitude, SlotHorario slot) {
        return reservasAerovia.existsByAerovia_IdAndAltitudePesAndSlotInicio(aeroviaId, altitude.pes(), slot.inicio());
    }

    @Override
    public boolean vagaOcupada(Vaga vaga, SlotHorario slot) {
        return reservasVaga.existsByVaga_IdAndSlotInicio(vaga.getId(), slot.inicio());
    }

    @Override
    public void salvarTodas(List<Reserva> reservas) {
        List<ReservaAeroviaEntity> aerovias = new ArrayList<>();
        List<ReservaVagaEntity> vagas = new ArrayList<>();
        for (Reserva r : reservas) {
            switch (r) {
                case ReservaAerovia a -> aerovias.add(new ReservaAeroviaEntity(a.getPlanoId(),
                        em.getReference(AeroviaEntity.class, a.getAerovia().getId()), a.getAltitude().pes(), a.getSlot().inicio()));
                case ReservaVaga v -> vagas.add(new ReservaVagaEntity(v.getPlanoId(),
                        em.getReference(VagaEntity.class, v.getVaga().getId()), v.getSlot().inicio()));
                default -> throw new IllegalStateException("Tipo de reserva sem mapeamento: " + r.getClass());
            }
        }
        try {
            reservasAerovia.saveAll(aerovias);
            reservasVaga.saveAll(vagas);
            em.flush();
        } catch (DataIntegrityViolationException e) {
            throw new ConflitoReservaException(
                    "Outro plano reservou um dos recursos deste plano durante a aprovação. Nenhuma reserva foi mantida.");
        }
    }

    @Override
    public void removerDoPlano(Long planoId) {
        reservasAerovia.apagarDoPlano(planoId);
        reservasVaga.apagarDoPlano(planoId);
    }

    @Override
    public List<Reserva> listarDoPlano(Long planoId) {
        List<Reserva> reservas = new ArrayList<>();
        reservasAerovia.findByPlanoIdOrderBySlotInicio(planoId).forEach(e -> reservas.add(new ReservaAerovia(
                e.getId(), e.getPlanoId(), new SlotHorario(e.getSlotInicio()),
                mapper.paraDominio(e.getAerovia()), new Altitude(e.getAltitudePes()))));
        reservasVaga.findByPlanoIdOrderBySlotInicio(planoId).forEach(e -> reservas.add(new ReservaVaga(
                e.getId(), e.getPlanoId(), new SlotHorario(e.getSlotInicio()), mapper.paraDominio(e.getVaga()))));
        reservas.sort(Comparator.comparing((Reserva r) -> r.getSlot().inicio()));
        return reservas;
    }
}
