package br.pucrs.projarc.gestaoplanosvoo.domain.repository;

import br.pucrs.projarc.gestaoplanosvoo.domain.model.Altitude;
import br.pucrs.projarc.gestaoplanosvoo.domain.model.SlotHorario;
import br.pucrs.projarc.gestaoplanosvoo.domain.model.Vaga;

/** O que a validação precisa saber sobre as reservas atuais. Implementado pela persistência. */
public interface VerificadorDisponibilidade {

    boolean altitudeOcupada(String aeroviaId, Altitude altitude, SlotHorario slot);

    boolean vagaOcupada(Vaga vaga, SlotHorario slot);
}
