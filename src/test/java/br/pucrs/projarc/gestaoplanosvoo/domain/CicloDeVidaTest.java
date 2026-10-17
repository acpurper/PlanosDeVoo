package br.pucrs.projarc.gestaoplanosvoo.domain;

import br.pucrs.projarc.gestaoplanosvoo.domain.exception.PlanoInvalidoException;
import br.pucrs.projarc.gestaoplanosvoo.domain.exception.RecursoNaoEncontradoException;
import br.pucrs.projarc.gestaoplanosvoo.domain.exception.RegraDeNegocioException;
import br.pucrs.projarc.gestaoplanosvoo.domain.exception.SituacaoInvalidaException;
import br.pucrs.projarc.gestaoplanosvoo.domain.model.PlanoDeVoo;
import br.pucrs.projarc.gestaoplanosvoo.domain.model.Pouso;
import br.pucrs.projarc.gestaoplanosvoo.domain.model.SituacaoPlano;
import br.pucrs.projarc.gestaoplanosvoo.domain.model.reserva.Reserva;
import br.pucrs.projarc.gestaoplanosvoo.domain.model.reserva.ReservaAerovia;
import br.pucrs.projarc.gestaoplanosvoo.domain.model.reserva.ReservaVaga;
import org.junit.jupiter.api.Test;

import java.util.List;

import static br.pucrs.projarc.gestaoplanosvoo.domain.Malha.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CicloDeVidaTest {

    private PlanoDeVoo planoValido() {
        PlanoDeVoo plano = plano(PASSAGEIROS, POA, GRU, as(8, 10));
        plano.adicionarTrecho(R2, alt(30_000), as(8, 10), new Pouso(vaga(GRU, 1), 60));
        return plano;
    }

    @Test
    void planoNasceEmEdicaoESemTrechos() {
        PlanoDeVoo plano = plano(PASSAGEIROS, POA, GRU, as(8, 0));
        assertThat(plano.getSituacao()).isEqualTo(SituacaoPlano.EM_EDICAO);
        assertThat(plano.getTrechos()).isEmpty();
    }

    @Test
    void origemIgualAoDestinoEhRejeitada() {
        assertThatThrownBy(() -> plano(PASSAGEIROS, POA, POA, as(8, 0)))
                .isInstanceOf(RegraDeNegocioException.class);
    }

    @Test
    void aprovarGeraUmaReservaPorSlotDeTrechoEDePouso() {
        PlanoDeVoo plano = planoValido();
        List<Reserva> reservas = plano.aprovar(TUDO_LIVRE);
        // trecho 08:10 -> 09:12 = slots 08h e 09h; vaga 09:12 -> 10:12 = slots 09h e 10h
        assertThat(reservas).filteredOn(r -> r instanceof ReservaAerovia).hasSize(2);
        assertThat(reservas).filteredOn(r -> r instanceof ReservaVaga).hasSize(2);
        assertThat(plano.getSituacao()).isEqualTo(SituacaoPlano.APROVADO);
    }

    @Test
    void aprovacaoInvalidaMantemEmEdicaoESemReservas() {
        PlanoDeVoo plano = planoValido();
        Ocupacao ocupada = new Ocupacao().ocuparAltitude(R2, 30_000, as(9, 0));
        assertThatThrownBy(() -> plano.aprovar(ocupada)).isInstanceOf(PlanoInvalidoException.class);
        assertThat(plano.getSituacao()).isEqualTo(SituacaoPlano.EM_EDICAO);
        assertThat(plano.getReservas()).isEmpty();
    }

    @Test
    void somentePlanoEmEdicaoPodeSerModificadoOuValidado() {
        PlanoDeVoo plano = planoValido();
        plano.aprovar(TUDO_LIVRE);
        assertThatThrownBy(() -> plano.adicionarTrecho(R3, alt(30_000), as(12, 0), null))
                .isInstanceOf(SituacaoInvalidaException.class);
        assertThatThrownBy(() -> plano.removerTrecho(1)).isInstanceOf(SituacaoInvalidaException.class);
        assertThatThrownBy(() -> plano.validar(TUDO_LIVRE)).isInstanceOf(SituacaoInvalidaException.class);
        assertThatThrownBy(() -> plano.aprovar(TUDO_LIVRE)).isInstanceOf(SituacaoInvalidaException.class);
    }

    @Test
    void cancelarPlanoAprovadoFuturoLiberaReservas() {
        PlanoDeVoo plano = planoValido();
        plano.aprovar(TUDO_LIVRE);
        plano.cancelar(as(7, 0));
        assertThat(plano.getSituacao()).isEqualTo(SituacaoPlano.CANCELADO);
        assertThat(plano.getReservas()).isEmpty();
        assertThat(plano.getTrechos()).as("os dados do plano são preservados").hasSize(1);
    }

    @Test
    void naoCancelaVooJaExecutado() {
        PlanoDeVoo plano = planoValido();
        plano.aprovar(TUDO_LIVRE);
        assertThatThrownBy(() -> plano.cancelar(as(8, 10))).isInstanceOf(SituacaoInvalidaException.class);
        assertThatThrownBy(() -> plano.cancelar(as(12, 0))).isInstanceOf(SituacaoInvalidaException.class);
        assertThat(plano.getSituacao()).isEqualTo(SituacaoPlano.APROVADO);
    }

    @Test
    void naoCancelaPlanoNaoAprovado() {
        assertThatThrownBy(() -> planoValido().cancelar(as(7, 0))).isInstanceOf(SituacaoInvalidaException.class);
    }

    @Test
    void removerTrechoRenumeraOsSeguintes() {
        PlanoDeVoo plano = plano(PARTICULAR, POA, CWB, as(14, 0));
        plano.adicionarTrecho(R1, alt(26_000), as(14, 0), null);
        plano.adicionarTrecho(R4, alt(26_000), as(14, 37), null);
        plano.adicionarTrecho(R6, alt(26_000), as(15, 2), new Pouso(vaga(GRU, 1), 30));
        plano.removerTrecho(2);
        assertThat(plano.getTrechos()).extracting(t -> t.getOrdem()).containsExactly(1, 2);
        assertThat(plano.getTrechos()).extracting(t -> t.getAerovia().getId()).containsExactly("R1", "R6");
    }

    @Test
    void alterarTrechoMantemAOrdem() {
        PlanoDeVoo plano = plano(PARTICULAR, POA, FLN, as(14, 0));
        plano.adicionarTrecho(R1, alt(26_000), as(14, 0), null);
        plano.alterarTrecho(1, R1, alt(25_000), as(14, 0), new Pouso(vaga(FLN, 2), 20));
        assertThat(plano.getTrechos()).singleElement().satisfies(t -> {
            assertThat(t.getOrdem()).isEqualTo(1);
            assertThat(t.getAltitude().pes()).isEqualTo(25_000);
            assertThat(t.temPouso()).isTrue();
        });
    }

    @Test
    void trechoInexistenteGeraNaoEncontrado() {
        PlanoDeVoo plano = plano(PARTICULAR, POA, FLN, as(14, 0));
        assertThatThrownBy(() -> plano.removerTrecho(1)).isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void abandonarSomenteEmEdicao() {
        PlanoDeVoo plano = planoValido();
        plano.abandonar();
        assertThat(plano.getSituacao()).isEqualTo(SituacaoPlano.ABANDONADO);
        assertThatThrownBy(plano::abandonar).isInstanceOf(SituacaoInvalidaException.class);
    }
}
