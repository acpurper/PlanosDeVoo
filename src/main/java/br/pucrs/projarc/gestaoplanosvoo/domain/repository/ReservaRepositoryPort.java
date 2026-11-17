package br.pucrs.projarc.gestaoplanosvoo.domain.repository;

import br.pucrs.projarc.gestaoplanosvoo.domain.exception.ConflitoReservaException;
import br.pucrs.projarc.gestaoplanosvoo.domain.model.Altitude;
import br.pucrs.projarc.gestaoplanosvoo.domain.model.SlotHorario;
import br.pucrs.projarc.gestaoplanosvoo.domain.model.Vaga;
import br.pucrs.projarc.gestaoplanosvoo.domain.model.reserva.Reserva;
import br.pucrs.projarc.gestaoplanosvoo.domain.model.reserva.ReservaAerovia;
import br.pucrs.projarc.gestaoplanosvoo.domain.model.reserva.ReservaVaga;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Persistência das reservas. Usada somente pelo {@code ReservaService}; os casos de uso não a acessam.
 */
public interface ReservaRepositoryPort {

    boolean altitudeOcupada(String aeroviaId, Altitude altitude, SlotHorario slot);

    boolean vagaOcupada(Vaga vaga, SlotHorario slot);

    /**
     * Grava as reservas e sincroniza com o banco imediatamente (flush), para que um conflito de
     * unicidade apareça dentro da transação da aprovação.
     *
     * @throws ConflitoReservaException se outro plano reservou algum dos recursos
     */
    void salvarTodas(List<Reserva> reservas);

    void removerDoPlano(Long planoId);

    List<Reserva> listarDoPlano(Long planoId);

    /** Reservas da aerovia, em qualquer altitude, cujo slot começa em {@code [inicio, fim)}. */
    List<ReservaAerovia> listarDaAerovia(String aeroviaId, LocalDateTime inicio, LocalDateTime fim);

    /** Reservas das vagas do aeroporto cujo slot começa em {@code [inicio, fim)}. */
    List<ReservaVaga> listarDoAeroporto(String aeroportoCodigo, LocalDateTime inicio, LocalDateTime fim);
}
