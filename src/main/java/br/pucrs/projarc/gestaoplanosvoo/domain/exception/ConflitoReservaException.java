package br.pucrs.projarc.gestaoplanosvoo.domain.exception;

/** Outro plano reservou o mesmo recurso no mesmo slot durante a aprovação (corrida). API: 409. */
public class ConflitoReservaException extends DominioException {

    public ConflitoReservaException(String mensagem) {
        super("CONFLITO_RESERVA", mensagem);
    }
}
