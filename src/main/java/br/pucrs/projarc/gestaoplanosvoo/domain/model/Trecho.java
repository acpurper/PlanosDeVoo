package br.pucrs.projarc.gestaoplanosvoo.domain.model;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Uso de uma aerovia dentro do percurso. O usuário informa a entrada; a chegada é sempre derivada
 * (entrada + distância / velocidade de cruzeiro) e nunca vem do cliente.
 */
public final class Trecho {

    private final int ordem;
    private final Aerovia aerovia;
    private final Altitude altitude;
    private final LocalDateTime horarioEntrada;
    private final Pouso pouso;

    public Trecho(int ordem, Aerovia aerovia, Altitude altitude, LocalDateTime horarioEntrada, Pouso pouso) {
        this.ordem = ordem;
        this.aerovia = Objects.requireNonNull(aerovia, "aerovia");
        this.altitude = Objects.requireNonNull(altitude, "altitude");
        this.horarioEntrada = Objects.requireNonNull(horarioEntrada, "horarioEntrada").truncatedTo(ChronoUnit.MINUTES);
        this.pouso = pouso;
    }

    public int getOrdem() { return ordem; }
    public Aerovia getAerovia() { return aerovia; }
    public Altitude getAltitude() { return altitude; }
    public LocalDateTime getHorarioEntrada() { return horarioEntrada; }
    public Optional<Pouso> getPouso() { return Optional.ofNullable(pouso); }

    public boolean temPouso() {
        return pouso != null;
    }

    Trecho comOrdem(int novaOrdem) {
        return new Trecho(novaOrdem, aerovia, altitude, horarioEntrada, pouso);
    }

    public Duration duracao(double velocidadeKmh) {
        return aerovia.duracao(velocidadeKmh);
    }

    public LocalDateTime horarioChegada(double velocidadeKmh) {
        return horarioEntrada.plus(duracao(velocidadeKmh));
    }

    /** Quando a aeronave pode entrar na próxima aerovia: a chegada (sobrevoo) ou a chegada + permanência (pouso). */
    public LocalDateTime horarioLiberacao(double velocidadeKmh) {
        LocalDateTime chegada = horarioChegada(velocidadeKmh);
        return pouso == null ? chegada : pouso.fimPermanencia(chegada);
    }

    /** Slots de uso da aerovia: [entrada, chegada). */
    public List<SlotHorario> slots(double velocidadeKmh) {
        return SlotHorario.cobrindo(horarioEntrada, horarioChegada(velocidadeKmh));
    }

    /** Slots de ocupação da vaga do pouso; vazio em caso de sobrevoo. */
    public List<SlotHorario> slotsPouso(double velocidadeKmh) {
        return pouso == null ? List.of() : pouso.slots(horarioChegada(velocidadeKmh));
    }
}
