package br.pucrs.projarc.gestaoplanosvoo.domain;

import br.pucrs.projarc.gestaoplanosvoo.domain.model.Aeroporto;
import br.pucrs.projarc.gestaoplanosvoo.domain.model.Aerovia;
import br.pucrs.projarc.gestaoplanosvoo.domain.model.Piloto;
import br.pucrs.projarc.gestaoplanosvoo.domain.model.PlanoDeVoo;
import br.pucrs.projarc.gestaoplanosvoo.domain.model.Vaga;
import br.pucrs.projarc.gestaoplanosvoo.domain.model.aeronave.Aeronave;
import br.pucrs.projarc.gestaoplanosvoo.domain.model.aeronave.AeronaveCarga;
import br.pucrs.projarc.gestaoplanosvoo.domain.model.aeronave.AeronaveParticular;
import br.pucrs.projarc.gestaoplanosvoo.domain.model.aeronave.AeronavePassageiros;
import br.pucrs.projarc.gestaoplanosvoo.domain.repository.VerificadorDisponibilidade;
import br.pucrs.projarc.gestaoplanosvoo.domain.model.Altitude;
import br.pucrs.projarc.gestaoplanosvoo.domain.model.SlotHorario;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;

/** Malha do enunciado (seção 2) e frota de teste, montadas em memória para os testes de domínio. */
public final class Malha {

    public static final Aeroporto POA = aeroporto("POA", 5);
    public static final Aeroporto FLN = aeroporto("FLN", 5);
    public static final Aeroporto CWB = aeroporto("CWB", 5);
    public static final Aeroporto GRU = aeroporto("GRU", 5);

    public static final Aerovia R1 = new Aerovia("R1", POA, FLN, 363);
    public static final Aerovia R2 = new Aerovia("R2", POA, GRU, 865);
    public static final Aerovia R3 = new Aerovia("R3", GRU, POA, 865);
    public static final Aerovia R4 = new Aerovia("R4", FLN, CWB, 245);
    public static final Aerovia R5 = new Aerovia("R5", CWB, FLN, 245);
    public static final Aerovia R6 = new Aerovia("R6", CWB, GRU, 360);
    public static final Aerovia R7 = new Aerovia("R7", GRU, CWB, 360);
    public static final Aerovia R8 = new Aerovia("R8", FLN, POA, 363);

    /** Particular lenta e de autonomia curta: limite seguro = 595 km. */
    public static final Aeronave PARTICULAR_CURTA = new AeronaveParticular("PP-LEN", 400, 700, "Hangar Sul");
    public static final Aeronave PARTICULAR = new AeronaveParticular("PP-JAT", 600, 1500, "Aero Manutenção");
    public static final Aeronave PASSAGEIROS = new AeronavePassageiros("PR-AZA", 850, 5000, "Aerosul", 186);
    public static final Aeronave CARGA = new AeronaveCarga("PR-CGA", 780, 3500, "Cargo Sul", 20);

    public static final Piloto PILOTO = new Piloto(1L, "Piloto de Teste");

    public static final LocalDateTime DIA = LocalDateTime.of(2026, 12, 1, 0, 0);

    private Malha() {
    }

    public static LocalDateTime as(int hora, int minuto) {
        return DIA.withHour(hora).withMinute(minuto);
    }

    public static Vaga vaga(Aeroporto aeroporto, int numero) {
        return aeroporto.vaga(numero).orElseThrow();
    }

    public static PlanoDeVoo plano(Aeronave aeronave, Aeroporto origem, Aeroporto destino, LocalDateTime partida) {
        return PlanoDeVoo.criar(PILOTO, aeronave, origem, destino, partida);
    }

    public static Altitude alt(int pes) {
        return new Altitude(pes);
    }

    /** Nenhum recurso reservado. */
    public static final VerificadorDisponibilidade TUDO_LIVRE = new Ocupacao();

    /** Disponibilidade de teste em memória. */
    public static final class Ocupacao implements VerificadorDisponibilidade {
        private final Set<String> ocupados = new HashSet<>();

        public Ocupacao ocuparAltitude(Aerovia aerovia, int pes, LocalDateTime slot) {
            ocupados.add(aerovia.getId() + "|" + pes + "|" + slot);
            return this;
        }

        public Ocupacao ocuparVaga(Vaga vaga, LocalDateTime slot) {
            ocupados.add(vaga + "|" + slot);
            return this;
        }

        @Override
        public boolean altitudeOcupada(String aeroviaId, Altitude altitude, SlotHorario slot) {
            return ocupados.contains(aeroviaId + "|" + altitude.pes() + "|" + slot.inicio());
        }

        @Override
        public boolean vagaOcupada(Vaga vaga, SlotHorario slot) {
            return ocupados.contains(vaga + "|" + slot.inicio());
        }
    }

    private static Aeroporto aeroporto(String codigo, int vagas) {
        List<Vaga> lista = IntStream.rangeClosed(1, vagas).mapToObj(n -> new Vaga((long) n, codigo, n)).toList();
        return new Aeroporto(codigo, codigo, lista);
    }

    static Map<String, Aerovia> aerovias() {
        return Map.of("R1", R1, "R2", R2, "R3", R3, "R4", R4, "R5", R5, "R6", R6, "R7", R7, "R8", R8);
    }
}
