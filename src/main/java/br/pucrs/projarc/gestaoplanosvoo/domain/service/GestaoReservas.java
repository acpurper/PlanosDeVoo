package br.pucrs.projarc.gestaoplanosvoo.domain.service;

import java.time.LocalDateTime;
import java.util.List;

import br.pucrs.projarc.gestaoplanosvoo.domain.model.PlanoDeVoo;
import br.pucrs.projarc.gestaoplanosvoo.domain.model.reserva.Reserva;
import br.pucrs.projarc.gestaoplanosvoo.domain.model.reserva.ReservaAerovia;
import br.pucrs.projarc.gestaoplanosvoo.domain.model.reserva.ReservaVaga;
import br.pucrs.projarc.gestaoplanosvoo.domain.repository.VerificadorDisponibilidade;

/**
 * Contrato do serviço de reservas. É a única porta pela qual os casos de uso "mexem" nas reservas:
 * nenhum caso de uso acessa o {@code ReservaRepositoryPort} diretamente.
 *
 * <p>Os casos de uso dependem desta interface (e não da classe concreta), que recebem por injeção de
 * dependência no construtor. A implementação de domínio é o {@link ReservaService}; outra implementação
 * (por exemplo, um proxy para um serviço remoto de reservas) pode ser trocada sem alterar os casos de uso.
 */
public interface GestaoReservas extends VerificadorDisponibilidade {

    /** Gera e grava uma reserva por slot de um plano já aprovado. */
    List<Reserva> reservar(PlanoDeVoo plano);

    /** Remove todas as reservas do plano. */
    void liberar(Long planoId);

    /** Reservas do plano, em ordem de slot. */
    List<Reserva> doPlano(Long planoId);

    /** Reservas da aerovia, em qualquer altitude, cujo slot começa em {@code [inicio, fim)}. */
    List<ReservaAerovia> daAerovia(String aeroviaId, LocalDateTime inicio, LocalDateTime fim);

    /** Reservas das vagas do aeroporto cujo slot começa em {@code [inicio, fim)}. */
    List<ReservaVaga> doAeroporto(String aeroportoCodigo, LocalDateTime inicio, LocalDateTime fim);
}
