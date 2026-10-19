package br.pucrs.projarc.gestaoplanosvoo.application.dto.response;

import br.pucrs.projarc.gestaoplanosvoo.domain.model.Aeroporto;

public record AeroportoResponse(String codigo, String nome, int totalVagas) {

    public static AeroportoResponse de(Aeroporto a) {
        return new AeroportoResponse(a.getCodigo(), a.getNome(), a.getVagas().size());
    }
}
