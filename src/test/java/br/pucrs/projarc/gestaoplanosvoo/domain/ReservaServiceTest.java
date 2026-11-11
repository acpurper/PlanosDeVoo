package br.pucrs.projarc.gestaoplanosvoo.domain;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.Test;

import static br.pucrs.projarc.gestaoplanosvoo.domain.Malha.GRU;
import static br.pucrs.projarc.gestaoplanosvoo.domain.Malha.PASSAGEIROS;
import static br.pucrs.projarc.gestaoplanosvoo.domain.Malha.POA;
import static br.pucrs.projarc.gestaoplanosvoo.domain.Malha.R2;
import static br.pucrs.projarc.gestaoplanosvoo.domain.Malha.alt;
import static br.pucrs.projarc.gestaoplanosvoo.domain.Malha.as;
import static br.pucrs.projarc.gestaoplanosvoo.domain.Malha.plano;
import static br.pucrs.projarc.gestaoplanosvoo.domain.Malha.vaga;
import br.pucrs.projarc.gestaoplanosvoo.domain.exception.SituacaoInvalidaException;
import br.pucrs.projarc.gestaoplanosvoo.domain.model.Altitude;
import br.pucrs.projarc.gestaoplanosvoo.domain.model.PlanoDeVoo;
import br.pucrs.projarc.gestaoplanosvoo.domain.model.Pouso;
import br.pucrs.projarc.gestaoplanosvoo.domain.model.SlotHorario;
import br.pucrs.projarc.gestaoplanosvoo.domain.model.Vaga;
import br.pucrs.projarc.gestaoplanosvoo.domain.model.reserva.Reserva;
import br.pucrs.projarc.gestaoplanosvoo.domain.model.reserva.ReservaAerovia;
import br.pucrs.projarc.gestaoplanosvoo.domain.model.reserva.ReservaVaga;
import br.pucrs.projarc.gestaoplanosvoo.domain.repository.ReservaRepositoryPort;
import br.pucrs.projarc.gestaoplanosvoo.domain.service.ReservaService;

class ReservaServiceTest {

    private final ReservasEmMemoria repositorio = new ReservasEmMemoria();
    private final ReservaService servico = new ReservaService(repositorio);

    private PlanoDeVoo planoValido() {
        PlanoDeVoo plano = plano(PASSAGEIROS, POA, GRU, as(8, 10));
        plano.adicionarTrecho(R2, alt(30_000), as(8, 10), new Pouso(vaga(GRU, 1), 60));
        return plano;
    }

    @Test
    void geraUmaReservaPorSlotDeTrechoEDePouso() {
        PlanoDeVoo plano = planoValido();
        plano.aprovar(servico);

        List<Reserva> reservas = servico.gerarReservas(plano);

        assertThat(reservas).filteredOn(r -> r instanceof ReservaAerovia).hasSize(2);
        assertThat(reservas).filteredOn(r -> r instanceof ReservaVaga).hasSize(2);
        assertThat(repositorio.gravadas).as("gerar não grava").isEmpty();
    }

    @Test
    void reservarGravaAsReservasNoRepositorio() {
        PlanoDeVoo plano = planoValido();
        plano.aprovar(servico);

        List<Reserva> reservas = servico.reservar(plano);

        assertThat(reservas).hasSize(4);
        assertThat(repositorio.gravadas).containsExactlyElementsOf(reservas);
    }

    @Test
    void naoReservaParaPlanoQueNaoEstaAprovado() {
        PlanoDeVoo plano = planoValido();
        assertThatThrownBy(() -> servico.reservar(plano)).isInstanceOf(SituacaoInvalidaException.class);
        assertThat(repositorio.gravadas).isEmpty();
    }

    @Test
    void liberarRemoveAsReservasDoPlano() {
        servico.liberar(7L);
        assertThat(repositorio.removidosDoPlano).containsExactly(7L);
    }

    @Test
    void aprovacaoConsultaAOcupacaoPeloServico() {
        repositorio.altitudeOcupada = true;
        PlanoDeVoo plano = planoValido();
        assertThat(plano.validar(servico).valido()).isFalse();
    }

    private static final class ReservasEmMemoria implements ReservaRepositoryPort {
        final List<Reserva> gravadas = new ArrayList<>();
        final List<Long> removidosDoPlano = new ArrayList<>();
        boolean altitudeOcupada;

        @Override
        public boolean altitudeOcupada(String aeroviaId, Altitude altitude, SlotHorario slot) {
            return altitudeOcupada;
        }

        @Override
        public boolean vagaOcupada(Vaga vaga, SlotHorario slot) {
            return false;
        }

        @Override
        public void salvarTodas(List<Reserva> reservas) {
            gravadas.addAll(reservas);
        }

        @Override
        public void removerDoPlano(Long planoId) {
            removidosDoPlano.add(planoId);
        }

        @Override
        public List<Reserva> listarDoPlano(Long planoId) {
            return List.copyOf(gravadas);
        }

        @Override
        public List<ReservaAerovia> listarDaAerovia(String aeroviaId, LocalDateTime inicio, LocalDateTime fim) {
            return List.of();
        }

        @Override
        public List<ReservaVaga> listarDoAeroporto(String aeroportoCodigo, LocalDateTime inicio, LocalDateTime fim) {
            return List.of();
        }
    }
}

