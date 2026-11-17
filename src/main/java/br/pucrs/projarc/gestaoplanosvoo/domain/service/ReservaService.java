package br.pucrs.projarc.gestaoplanosvoo.domain.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import br.pucrs.projarc.gestaoplanosvoo.domain.exception.SituacaoInvalidaException;
import br.pucrs.projarc.gestaoplanosvoo.domain.model.Altitude;
import br.pucrs.projarc.gestaoplanosvoo.domain.model.PlanoDeVoo;
import br.pucrs.projarc.gestaoplanosvoo.domain.model.Pouso;
import br.pucrs.projarc.gestaoplanosvoo.domain.model.SituacaoPlano;
import br.pucrs.projarc.gestaoplanosvoo.domain.model.SlotHorario;
import br.pucrs.projarc.gestaoplanosvoo.domain.model.Trecho;
import br.pucrs.projarc.gestaoplanosvoo.domain.model.Vaga;
import br.pucrs.projarc.gestaoplanosvoo.domain.model.reserva.Reserva;
import br.pucrs.projarc.gestaoplanosvoo.domain.model.reserva.ReservaAerovia;
import br.pucrs.projarc.gestaoplanosvoo.domain.model.reserva.ReservaVaga;
import br.pucrs.projarc.gestaoplanosvoo.domain.repository.ReservaRepositoryPort;

/**
 * Serviço de domínio responsável pelas reservas. É a única classe que usa o {@link ReservaRepositoryPort};
 * os casos de uso o enxergam apenas pela interface {@link GestaoReservas}.
 */
public class ReservaService implements GestaoReservas {

    private final ReservaRepositoryPort reservas;

    public ReservaService(ReservaRepositoryPort reservas) {
        this.reservas = Objects.requireNonNull(reservas, "reservas");
    }

    @Override
    public boolean altitudeOcupada(String aeroviaId, Altitude altitude, SlotHorario slot) {
        return reservas.altitudeOcupada(aeroviaId, altitude, slot);
    }

    @Override
    public boolean vagaOcupada(Vaga vaga, SlotHorario slot) {
        return reservas.vagaOcupada(vaga, slot);
    }

    public List<Reserva> gerarReservas(PlanoDeVoo plano) {
        if (plano.getSituacao() != SituacaoPlano.APROVADO) {
            throw new SituacaoInvalidaException("PLANO_NAO_APROVADO",
                    "Só é possível reservar recursos para planos aprovados; o plano " + plano.getId()
                            + " está " + plano.getSituacao() + ".");
        }
        double velocidade = plano.getAeronave().getVelocidadeCruzeiroKmh();
        List<Reserva> novas = new ArrayList<>();
        for (Trecho trecho : plano.getTrechos()) {
            for (SlotHorario slot : trecho.slots(velocidade)) {
                novas.add(new ReservaAerovia(null, plano.getId(), slot, trecho.getAerovia(), trecho.getAltitude()));
            }
            Pouso pouso = trecho.getPouso().orElse(null);
            if (pouso != null) {
                for (SlotHorario slot : trecho.slotsPouso(velocidade)) {
                    novas.add(new ReservaVaga(null, plano.getId(), slot, pouso.vaga()));
                }
            }
        }
        return List.copyOf(novas);
    }

    @Override
    public List<Reserva> reservar(PlanoDeVoo plano) {
        List<Reserva> novas = gerarReservas(plano);
        reservas.salvarTodas(novas);
        return novas;
    }

    @Override
    public void liberar(Long planoId) {
        reservas.removerDoPlano(planoId);
    }

    @Override
    public List<Reserva> doPlano(Long planoId) {
        return reservas.listarDoPlano(planoId);
    }

    @Override
    public List<ReservaAerovia> daAerovia(String aeroviaId, LocalDateTime inicio, LocalDateTime fim) {
        return reservas.listarDaAerovia(aeroviaId, inicio, fim);
    }

    @Override
    public List<ReservaVaga> doAeroporto(String aeroportoCodigo, LocalDateTime inicio, LocalDateTime fim) {
        return reservas.listarDoAeroporto(aeroportoCodigo, inicio, fim);
    }
}

