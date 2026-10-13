package br.pucrs.projarc.gestaoplanosvoo.domain.repository;

import br.pucrs.projarc.gestaoplanosvoo.domain.exception.ConflitoReservaException;
import br.pucrs.projarc.gestaoplanosvoo.domain.model.reserva.Reserva;

import java.util.List;

public interface ReservaRepositoryPort extends VerificadorDisponibilidade {

    /**
     * Grava as reservas e sincroniza com o banco imediatamente (flush), para que um conflito de
     * unicidade apareça dentro da transação da aprovação.
     *
     * @throws ConflitoReservaException se outro plano reservou algum dos recursos
     */
    void salvarTodas(List<Reserva> reservas);

    void removerDoPlano(Long planoId);

    List<Reserva> listarDoPlano(Long planoId);
}
