package br.pucrs.projarc.gestaoplanosvoo.domain.exception;

/** Dado que viola uma regra do domínio (ex.: altitude fora da faixa, origem igual ao destino). API: 422. */
public class RegraDeNegocioException extends DominioException {

    public RegraDeNegocioException(String codigo, String mensagem) {
        super(codigo, mensagem);
    }
}
