package br.pucrs.projarc.gestaoplanosvoo.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

@Schema(description = "Trecho do percurso (UC03). O horário de chegada é calculado pelo sistema.")
public record TrechoRequest(
        @Schema(example = "R1") @NotBlank String aerovia,
        @Schema(description = "25000 a 35000, em passos de 1000", example = "30000") @NotNull Integer altitudePes,
        @Schema(example = "2026-12-01T16:00") @NotNull LocalDateTime horarioEntrada,
        @Schema(description = "Omitir para sobrevoo") @Valid PousoRequest pouso) {
}
