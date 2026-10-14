package br.pucrs.projarc.gestaoplanosvoo.application.dto.response;

import br.pucrs.projarc.gestaoplanosvoo.domain.model.Violacao;

public record ViolacaoResponse(String codigo, Integer trecho, String mensagem) {

    public static ViolacaoResponse de(Violacao v) {
        return new ViolacaoResponse(v.codigo().name(), v.trecho(), v.mensagem());
    }
}
