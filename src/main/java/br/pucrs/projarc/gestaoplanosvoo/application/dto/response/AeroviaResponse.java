package br.pucrs.projarc.gestaoplanosvoo.application.dto.response;

import br.pucrs.projarc.gestaoplanosvoo.domain.model.Aerovia;

public record AeroviaResponse(String id, String origem, String destino, double distanciaKm) {

    public static AeroviaResponse de(Aerovia a) {
        return new AeroviaResponse(a.getId(), a.getOrigem().getCodigo(), a.getDestino().getCodigo(), a.getDistanciaKm());
    }
}
