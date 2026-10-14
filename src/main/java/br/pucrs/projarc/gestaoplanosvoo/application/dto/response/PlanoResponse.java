package br.pucrs.projarc.gestaoplanosvoo.application.dto.response;

import br.pucrs.projarc.gestaoplanosvoo.domain.model.PlanoDeVoo;
import br.pucrs.projarc.gestaoplanosvoo.domain.model.SlotHorario;
import br.pucrs.projarc.gestaoplanosvoo.domain.model.Trecho;
import br.pucrs.projarc.gestaoplanosvoo.domain.model.aeronave.Aeronave;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Plano com percurso, horários calculados, situação e reservas. */
public record PlanoResponse(Long id, String situacao, PilotoResponse piloto, AeronaveResponse aeronave,
                            String origem, String destino, LocalDateTime dataHoraPartida,
                            @JsonInclude(JsonInclude.Include.NON_NULL) LocalDateTime chegadaPrevista,
                            List<TrechoResponse> trechos, List<ReservaResponse> reservas) {

    public record PilotoResponse(Long id, String nome) {
    }

    public record AeronaveResponse(String prefixo, String categoria, double velocidadeCruzeiroKmh,
                                   double autonomiaKm, double autonomiaSeguraKm) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record PousoResponse(String aeroporto, int vaga, int permanenciaMin, LocalDateTime fimPermanencia,
                                List<LocalDateTime> slotsVaga) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record TrechoResponse(int ordem, String aerovia, String de, String para, double distanciaKm,
                                 int altitudePes, LocalDateTime horarioEntrada, long duracaoMin,
                                 LocalDateTime horarioChegada, String passagem, PousoResponse pouso,
                                 double distanciaDesdeAbastecimentoKm, List<LocalDateTime> slotsAerovia) {
    }

    public static PlanoResponse de(PlanoDeVoo p) {
        Aeronave a = p.getAeronave();
        double vel = a.getVelocidadeCruzeiroKmh();
        List<Double> acumuladas = p.distanciasDesdeAbastecimento();
        List<TrechoResponse> trechos = new ArrayList<>();
        for (int i = 0; i < p.getTrechos().size(); i++) {
            Trecho t = p.getTrechos().get(i);
            LocalDateTime chegada = t.horarioChegada(vel);
            PousoResponse pouso = t.getPouso().map(po -> new PousoResponse(po.vaga().getAeroportoCodigo(),
                    po.vaga().getNumero(), po.permanenciaMin(), po.fimPermanencia(chegada),
                    inicios(po.slots(chegada)))).orElse(null);
            trechos.add(new TrechoResponse(t.getOrdem(), t.getAerovia().getId(),
                    t.getAerovia().getOrigem().getCodigo(), t.getAerovia().getDestino().getCodigo(),
                    t.getAerovia().getDistanciaKm(), t.getAltitude().pes(), t.getHorarioEntrada(),
                    t.duracao(vel).toMinutes(), chegada, t.temPouso() ? "POUSO" : "SOBREVOO", pouso,
                    acumuladas.get(i), inicios(t.slots(vel))));
        }
        LocalDateTime chegadaPrevista = p.getTrechos().isEmpty() ? null : p.getTrechos().getLast().horarioChegada(vel);
        return new PlanoResponse(p.getId(), p.getSituacao().name(),
                new PilotoResponse(p.getPiloto().getId(), p.getPiloto().getNome()),
                new AeronaveResponse(a.getPrefixo(), a.categoria(), vel, a.getAutonomiaKm(), a.autonomiaSegura()),
                p.getOrigem().getCodigo(), p.getDestino().getCodigo(), p.getDataHoraPartida(), chegadaPrevista,
                trechos, p.getReservas().stream().map(ReservaResponse::de).toList());
    }

    private static List<LocalDateTime> inicios(List<SlotHorario> slots) {
        return slots.stream().map(SlotHorario::inicio).toList();
    }
}
