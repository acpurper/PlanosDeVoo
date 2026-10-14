package br.pucrs.projarc.gestaoplanosvoo.domain.exception;

/** Operação incompatível com a situação atual do plano (ex.: editar plano aprovado). API: 409. */
public class SituacaoInvalidaException extends DominioException {

    public SituacaoInvalidaException(String codigo, String mensagem) {
        super(codigo, mensagem);
    }
}
