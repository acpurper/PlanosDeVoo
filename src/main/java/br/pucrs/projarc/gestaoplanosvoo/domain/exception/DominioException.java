package br.pucrs.projarc.gestaoplanosvoo.domain.exception;

/** Base das exceções de regra de negócio. O domínio não conhece HTTP; o mapeamento para status fica na API. */
public abstract class DominioException extends RuntimeException {

    private final String codigo;

    protected DominioException(String codigo, String mensagem) {
        super(mensagem);
        this.codigo = codigo;
    }

    public String getCodigo() {
        return codigo;
    }
}
