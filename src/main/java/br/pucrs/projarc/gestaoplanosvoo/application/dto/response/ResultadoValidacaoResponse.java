package br.pucrs.projarc.gestaoplanosvoo.application.dto.response;

import br.pucrs.projarc.gestaoplanosvoo.domain.model.ResultadoValidacao;

import java.util.List;

public record ResultadoValidacaoResponse(Long planoId, boolean valido, List<ViolacaoResponse> violacoes) {

    public static ResultadoValidacaoResponse de(Long planoId, ResultadoValidacao r) {
        return new ResultadoValidacaoResponse(planoId, r.valido(), r.violacoes().stream().map(ViolacaoResponse::de).toList());
    }
}
