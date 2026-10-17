package br.pucrs.projarc.gestaoplanosvoo.domain.model;

import br.pucrs.projarc.gestaoplanosvoo.domain.exception.RegraDeNegocioException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

public record Pouso(Vaga vaga, int permanenciaMin) {

    public Pouso {
        Objects.requireNonNull(vaga, "vaga");
        if (permanenciaMin <= 0) {
            throw new RegraDeNegocioException("PERMANENCIA_INVALIDA", "A permanência deve ser maior que zero minuto.");
        }
    }

    public LocalDateTime fimPermanencia(LocalDateTime chegada) {
        return chegada.plusMinutes(permanenciaMin);
    }

    public List<SlotHorario> slots(LocalDateTime chegada) {
        return SlotHorario.cobrindo(chegada, fimPermanencia(chegada));
    }
}
