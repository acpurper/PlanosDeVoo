package br.pucrs.projarc.gestaoplanosvoo.application.dto.response;

import br.pucrs.projarc.gestaoplanosvoo.domain.model.Piloto;

public record PilotoResponse(Long id, String nome) {

    public static PilotoResponse de(Piloto p) {
        return new PilotoResponse(p.getId(), p.getNome());
    }
}
