package br.pucrs.projarc.gestaoplanosvoo.domain.exception;

public class RecursoNaoEncontradoException extends DominioException {

    public RecursoNaoEncontradoException(String mensagem) {
        super("NAO_ENCONTRADO", mensagem);
    }
}
