package br.pucrs.projarc.gestaoplanosvoo.application;

import br.pucrs.projarc.gestaoplanosvoo.application.dto.response.DisponibilidadeAeroviaResponse;
import br.pucrs.projarc.gestaoplanosvoo.application.dto.response.DisponibilidadeVagasResponse;
import br.pucrs.projarc.gestaoplanosvoo.application.dto.response.SlotOcupadoResponse;
import br.pucrs.projarc.gestaoplanosvoo.application.usecases.ConsultarAltitudesLivresUseCase;
import br.pucrs.projarc.gestaoplanosvoo.application.usecases.ConsultarVagasLivresUseCase;
import br.pucrs.projarc.gestaoplanosvoo.domain.exception.RecursoNaoEncontradoException;
import br.pucrs.projarc.gestaoplanosvoo.domain.exception.RegraDeNegocioException;
import br.pucrs.projarc.gestaoplanosvoo.domain.model.Aeroporto;
import br.pucrs.projarc.gestaoplanosvoo.domain.model.Aerovia;
import br.pucrs.projarc.gestaoplanosvoo.domain.model.Altitude;
import br.pucrs.projarc.gestaoplanosvoo.domain.model.Piloto;
import br.pucrs.projarc.gestaoplanosvoo.domain.model.SlotHorario;
import br.pucrs.projarc.gestaoplanosvoo.domain.model.Vaga;
import br.pucrs.projarc.gestaoplanosvoo.domain.model.aeronave.Aeronave;
import br.pucrs.projarc.gestaoplanosvoo.domain.model.reserva.Reserva;
import br.pucrs.projarc.gestaoplanosvoo.domain.model.reserva.ReservaAerovia;
import br.pucrs.projarc.gestaoplanosvoo.domain.model.reserva.ReservaVaga;
import br.pucrs.projarc.gestaoplanosvoo.domain.repository.MalhaRepositoryPort;
import br.pucrs.projarc.gestaoplanosvoo.domain.repository.ReservaRepositoryPort;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static br.pucrs.projarc.gestaoplanosvoo.domain.Malha.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConsultaDisponibilidadeTest {

    private final MalhaEmMemoria malha = new MalhaEmMemoria();
    private final ReservasEmMemoria reservas = new ReservasEmMemoria();
    private final ConsultarAltitudesLivresUseCase altitudes = new ConsultarAltitudesLivresUseCase(malha, reservas);
    private final ConsultarVagasLivresUseCase vagas = new ConsultarVagasLivresUseCase(malha, reservas);

    @Test
    void altitudeOcupadaEmUmSlotDoPeriodoNaoEstaDisponivel() {
        reservas.aerovia(1L, R2, 30_000, as(9, 0));
        reservas.aerovia(2L, R2, 31_000, as(12, 0)); // fora do período
        reservas.aerovia(3L, R1, 32_000, as(9, 0));  // outra aerovia

        DisponibilidadeAeroviaResponse r = altitudes.executar("R2", as(8, 40), as(9, 43));

        assertThat(r.slots()).containsExactly(as(8, 0), as(9, 0));
        assertThat(r.altitudesDisponiveis()).hasSize(10).doesNotContain(30_000).contains(31_000, 32_000);
        assertThat(r.altitudesOcupadas()).singleElement().satisfies(o -> {
            assertThat(o.altitudePes()).isEqualTo(30_000);
            assertThat(o.slotsOcupados()).containsExactly(new SlotOcupadoResponse(as(9, 0), 1L));
        });
    }

    @Test
    void semReservasTodasAsOnzeAltitudesEstaoDisponiveis() {
        DisponibilidadeAeroviaResponse r = altitudes.executar("R6", as(0, 0), as(23, 59));

        assertThat(r.slots()).hasSize(24);
        assertThat(r.altitudesDisponiveis()).containsExactlyElementsOf(
                Altitude.todas().stream().map(Altitude::pes).toList());
        assertThat(r.altitudesOcupadas()).isEmpty();
    }

    @Test
    void periodoQueTerminaNaHoraCheiaNaoIncluiOSlotSeguinte() {
        reservas.aerovia(1L, R2, 30_000, as(10, 0));

        DisponibilidadeAeroviaResponse r = altitudes.executar("R2", as(9, 0), as(10, 0));

        assertThat(r.slots()).containsExactly(as(9, 0));
        assertThat(r.altitudesOcupadas()).isEmpty();
    }

    @Test
    void vagaOcupadaEmAlgumSlotNaoEstaDisponivel() {
        reservas.vaga(1L, vaga(GRU, 1), as(9, 0));
        reservas.vaga(1L, vaga(GRU, 1), as(10, 0));
        reservas.vaga(5L, vaga(POA, 2), as(10, 0)); // outro aeroporto

        DisponibilidadeVagasResponse r = vagas.executar("GRU", as(10, 5), as(10, 35));

        assertThat(r.slots()).containsExactly(as(10, 0));
        assertThat(r.totalVagas()).isEqualTo(5);
        assertThat(r.quantidadeDisponivel()).isEqualTo(4);
        assertThat(r.vagasDisponiveis()).containsExactly(2, 3, 4, 5);
        assertThat(r.vagasOcupadas()).singleElement().satisfies(v -> {
            assertThat(v.vaga()).isEqualTo(1);
            assertThat(v.slotsOcupados()).containsExactly(new SlotOcupadoResponse(as(10, 0), 1L));
        });
    }

    @Test
    void periodoComFimAntesDoInicioEhRejeitado() {
        assertThatThrownBy(() -> altitudes.executar("R2", as(10, 0), as(9, 0)))
                .isInstanceOf(RegraDeNegocioException.class).hasMessageContaining("posterior");
        assertThatThrownBy(() -> vagas.executar("GRU", as(10, 0), as(10, 0)))
                .isInstanceOf(RegraDeNegocioException.class);
    }

    @Test
    void aeroviaOuAeroportoInexistenteDa404() {
        assertThatThrownBy(() -> altitudes.executar("R99", as(8, 0), as(9, 0)))
                .isInstanceOf(RecursoNaoEncontradoException.class);
        assertThatThrownBy(() -> vagas.executar("XXX", as(8, 0), as(9, 0)))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void consultaNaoGravaReservas() {
        reservas.aerovia(1L, R2, 30_000, as(9, 0));
        altitudes.executar("R2", as(8, 0), as(10, 0));
        vagas.executar("GRU", as(8, 0), as(10, 0));
        assertThat(reservas.gravacoes).isZero();
    }

    private static final class MalhaEmMemoria implements MalhaRepositoryPort {
        private final List<Aeroporto> aeroportos = List.of(POA, FLN, CWB, GRU);
        private final List<Aerovia> aerovias = List.of(R1, R2, R3, R4, R5, R6, R7, R8);

        @Override
        public Optional<Aeroporto> buscarAeroporto(String codigo) {
            return aeroportos.stream().filter(a -> a.getCodigo().equals(codigo)).findFirst();
        }

        @Override
        public Optional<Aerovia> buscarAerovia(String id) {
            return aerovias.stream().filter(a -> a.getId().equals(id)).findFirst();
        }

        @Override
        public Optional<Piloto> buscarPiloto(Long id) {
            return Optional.of(PILOTO);
        }

        @Override
        public Optional<Aeronave> buscarAeronave(String prefixo) {
            return Optional.empty();
        }

        @Override
        public List<Aeroporto> listarAeroportos() {
            return aeroportos;
        }

        @Override
        public List<Aerovia> listarAerovias() {
            return aerovias;
        }

        @Override
        public List<Piloto> listarPilotos() {
            return List.of(PILOTO);
        }

        @Override
        public List<Aeronave> listarAeronaves() {
            return List.of();
        }
    }

    private static final class ReservasEmMemoria implements ReservaRepositoryPort {
        private final List<ReservaAerovia> deAerovia = new ArrayList<>();
        private final List<ReservaVaga> deVaga = new ArrayList<>();
        int gravacoes;

        void aerovia(Long plano, Aerovia aerovia, int pes, LocalDateTime slot) {
            deAerovia.add(new ReservaAerovia(null, plano, new SlotHorario(slot), aerovia, new Altitude(pes)));
        }

        void vaga(Long plano, Vaga vaga, LocalDateTime slot) {
            deVaga.add(new ReservaVaga(null, plano, new SlotHorario(slot), vaga));
        }

        private static boolean em(SlotHorario s, LocalDateTime inicio, LocalDateTime fim) {
            return !s.inicio().isBefore(inicio) && s.inicio().isBefore(fim);
        }

        @Override
        public List<ReservaAerovia> listarDaAerovia(String aeroviaId, LocalDateTime inicio, LocalDateTime fim) {
            return deAerovia.stream().filter(r -> r.getAerovia().getId().equals(aeroviaId) && em(r.getSlot(), inicio, fim))
                    .toList();
        }

        @Override
        public List<ReservaVaga> listarDoAeroporto(String codigo, LocalDateTime inicio, LocalDateTime fim) {
            return deVaga.stream().filter(r -> r.getVaga().getAeroportoCodigo().equals(codigo) && em(r.getSlot(), inicio, fim))
                    .toList();
        }

        @Override
        public boolean altitudeOcupada(String aeroviaId, Altitude altitude, SlotHorario slot) {
            return deAerovia.stream().anyMatch(r -> r.getAerovia().getId().equals(aeroviaId)
                    && r.getAltitude().equals(altitude) && r.getSlot().equals(slot));
        }

        @Override
        public boolean vagaOcupada(Vaga vaga, SlotHorario slot) {
            return deVaga.stream().anyMatch(r -> r.getVaga().equals(vaga) && r.getSlot().equals(slot));
        }

        @Override
        public void salvarTodas(List<Reserva> reservas) {
            gravacoes++;
        }

        @Override
        public void removerDoPlano(Long planoId) {
            gravacoes++;
        }

        @Override
        public List<Reserva> listarDoPlano(Long planoId) {
            return List.of();
        }
    }
}
