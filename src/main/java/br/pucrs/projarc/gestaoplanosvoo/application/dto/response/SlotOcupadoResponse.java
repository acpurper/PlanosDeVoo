package br.pucrs.projarc.gestaoplanosvoo.application.dto.response;

import java.time.LocalDateTime;

/** Slot de uma hora já reservado e o plano aprovado que o reservou. */
public record SlotOcupadoResponse(LocalDateTime slotInicio, Long planoId) {
}
