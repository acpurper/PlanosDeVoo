package br.pucrs.projarc.gestaoplanosvoo.domain.exception;

public class ConflitoReservaException extends DominioException {

    public ConflitoReservaException(String mensagem) {
        super("CONFLITO_RESERVA", mensagem);
    }
}
