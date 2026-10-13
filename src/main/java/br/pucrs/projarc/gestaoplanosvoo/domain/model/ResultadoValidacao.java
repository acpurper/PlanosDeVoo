package br.pucrs.projarc.gestaoplanosvoo.domain.model;

import java.util.List;

/** Laudo da validação: reúne todas as violações encontradas, e não só a primeira. */
public record ResultadoValidacao(List<Violacao> violacoes) {

    public ResultadoValidacao {
        violacoes = List.copyOf(violacoes);
    }

    public boolean valido() {
        return violacoes.isEmpty();
    }

    public boolean contem(CodigoViolacao codigo) {
        return violacoes.stream().anyMatch(v -> v.codigo() == codigo);
    }
}
