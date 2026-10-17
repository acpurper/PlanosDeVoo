package br.pucrs.projarc.gestaoplanosvoo.domain.exception;

public class SituacaoInvalidaException extends DominioException {

    public SituacaoInvalidaException(String codigo, String mensagem) {
        super(codigo, mensagem);
    }
}
