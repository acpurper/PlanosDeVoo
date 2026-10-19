package br.pucrs.projarc.gestaoplanosvoo.application.dto.response;

import br.pucrs.projarc.gestaoplanosvoo.domain.model.PlanoDeVoo;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;

/** Linha da listagem de planos; o detalhe completo fica em GET /planos/{id}. */
public record PlanoResumoResponse(Long id, String situacao, Long pilotoId, String aeronave, String origem,
                                  String destino, LocalDateTime dataHoraPartida,
                                  @JsonInclude(JsonInclude.Include.NON_NULL) LocalDateTime chegadaPrevista,
                                  int quantidadeTrechos, int quantidadeReservas) {

    public static PlanoResumoResponse de(PlanoDeVoo p) {
        LocalDateTime chegada = p.getTrechos().isEmpty() ? null
                : p.getTrechos().getLast().horarioChegada(p.getAeronave().getVelocidadeCruzeiroKmh());
        return new PlanoResumoResponse(p.getId(), p.getSituacao().name(), p.getPiloto().getId(),
                p.getAeronave().getPrefixo(), p.getOrigem().getCodigo(), p.getDestino().getCodigo(),
                p.getDataHoraPartida(), chegada, p.getTrechos().size(), p.getReservas().size());
    }
}
