package br.pucrs.projarc.gestaoplanosvoo.domain.exception;

/** Referência a algo que não existe (plano, piloto, aerovia, vaga, trecho...). API: 404. */
public class RecursoNaoEncontradoException extends DominioException {

    public RecursoNaoEncontradoException(String mensagem) {
        super("NAO_ENCONTRADO", mensagem);
    }
}
