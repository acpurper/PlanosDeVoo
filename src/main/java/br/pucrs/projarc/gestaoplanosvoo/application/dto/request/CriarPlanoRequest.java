package br.pucrs.projarc.gestaoplanosvoo.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

@Schema(description = "Dados básicos de um novo plano de voo (UC02)")
public record CriarPlanoRequest(
        @Schema(example = "1") @NotNull Long pilotoId,
        @Schema(example = "PR-AZA") @NotBlank String aeronave,
        @Schema(example = "POA") @NotBlank String origem,
        @Schema(example = "FLN") @NotBlank String destino,
        @Schema(example = "2026-12-01T16:00") @NotNull LocalDateTime dataHoraPartida) {
}
