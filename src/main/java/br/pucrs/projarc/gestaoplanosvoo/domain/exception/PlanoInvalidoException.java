package br.pucrs.projarc.gestaoplanosvoo.domain.exception;

import br.pucrs.projarc.gestaoplanosvoo.domain.model.ResultadoValidacao;

/** Aprovação recusada porque a validação encontrou violações. API: 422, com a lista de violações. */
public class PlanoInvalidoException extends DominioException {

    private final ResultadoValidacao resultado;

    public PlanoInvalidoException(ResultadoValidacao resultado) {
        super("PLANO_INVALIDO", "O plano possui " + resultado.violacoes().size() + " violação(ões) e não pode ser aprovado.");
        this.resultado = resultado;
    }

    public ResultadoValidacao getResultado() {
        return resultado;
    }
}
