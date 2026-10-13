package br.pucrs.projarc.gestaoplanosvoo.domain.model;

import br.pucrs.projarc.gestaoplanosvoo.domain.exception.PlanoInvalidoException;
import br.pucrs.projarc.gestaoplanosvoo.domain.exception.RecursoNaoEncontradoException;
import br.pucrs.projarc.gestaoplanosvoo.domain.exception.RegraDeNegocioException;
import br.pucrs.projarc.gestaoplanosvoo.domain.exception.SituacaoInvalidaException;
import br.pucrs.projarc.gestaoplanosvoo.domain.model.aeronave.Aeronave;
import br.pucrs.projarc.gestaoplanosvoo.domain.model.aeronave.RegraOperacao;
import br.pucrs.projarc.gestaoplanosvoo.domain.model.reserva.Reserva;
import br.pucrs.projarc.gestaoplanosvoo.domain.model.reserva.ReservaAerovia;
import br.pucrs.projarc.gestaoplanosvoo.domain.model.reserva.ReservaVaga;
import br.pucrs.projarc.gestaoplanosvoo.domain.repository.VerificadorDisponibilidade;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Raiz do agregado. Controla o ciclo de vida, é dono do percurso (trechos e pousos) e das reservas,
 * e concentra a validação. Toda mutação passa por aqui e só é aceita em EM_EDICAO.
 */
public class PlanoDeVoo {

    private final Long id;
    private final Piloto piloto;
    private final Aeronave aeronave;
    private final Aeroporto origem;
    private final Aeroporto destino;
    private final LocalDateTime dataHoraPartida;
    private SituacaoPlano situacao;
    private final List<Trecho> trechos;
    private final List<Reserva> reservas;
    private final Long versao;

    private PlanoDeVoo(Long id, Piloto piloto, Aeronave aeronave, Aeroporto origem, Aeroporto destino,
                       LocalDateTime dataHoraPartida, SituacaoPlano situacao, List<Trecho> trechos,
                       List<Reserva> reservas, Long versao) {
        this.id = id;
        this.piloto = Objects.requireNonNull(piloto, "piloto");
        this.aeronave = Objects.requireNonNull(aeronave, "aeronave");
        this.origem = Objects.requireNonNull(origem, "origem");
        this.destino = Objects.requireNonNull(destino, "destino");
        this.dataHoraPartida = Objects.requireNonNull(dataHoraPartida, "dataHoraPartida").truncatedTo(ChronoUnit.MINUTES);
        this.situacao = Objects.requireNonNull(situacao, "situacao");
        this.trechos = new ArrayList<>(trechos);
        this.reservas = new ArrayList<>(reservas);
        this.versao = versao;
    }

    /** UC02: o plano nasce em edição e sem trechos. */
    public static PlanoDeVoo criar(Piloto piloto, Aeronave aeronave, Aeroporto origem, Aeroporto destino,
                                   LocalDateTime dataHoraPartida) {
        if (origem.equals(destino)) {
            throw new RegraDeNegocioException("ORIGEM_IGUAL_DESTINO",
                    "Origem e destino do plano não podem ser o mesmo aeroporto (" + origem + ").");
        }
        return new PlanoDeVoo(null, piloto, aeronave, origem, destino, dataHoraPartida,
                SituacaoPlano.EM_EDICAO, List.of(), List.of(), null);
    }

    /** Recria um plano já persistido, sem reaplicar regras de criação. Uso exclusivo da persistência. */
    public static PlanoDeVoo reconstituir(Long id, Piloto piloto, Aeronave aeronave, Aeroporto origem, Aeroporto destino,
                                          LocalDateTime dataHoraPartida, SituacaoPlano situacao,
                                          List<Trecho> trechos, List<Reserva> reservas, Long versao) {
        return new PlanoDeVoo(id, piloto, aeronave, origem, destino, dataHoraPartida, situacao, trechos, reservas, versao);
    }

    // ------------------------------------------------------------------ UC03: edição do percurso

    /** Acrescenta um trecho ao final do percurso. */
    public Trecho adicionarTrecho(Aerovia aerovia, Altitude altitude, LocalDateTime horarioEntrada, Pouso pouso) {
        exigirEmEdicao("alterado");
        Trecho trecho = new Trecho(trechos.size() + 1, aerovia, altitude, horarioEntrada, pouso);
        trechos.add(trecho);
        return trecho;
    }

    /** Substitui o trecho da ordem indicada, mantendo a ordem. */
    public Trecho alterarTrecho(int ordem, Aerovia aerovia, Altitude altitude, LocalDateTime horarioEntrada, Pouso pouso) {
        exigirEmEdicao("alterado");
        int indice = indiceDoTrecho(ordem);
        Trecho trecho = new Trecho(ordem, aerovia, altitude, horarioEntrada, pouso);
        trechos.set(indice, trecho);
        return trecho;
    }

    /** Remove o trecho da ordem indicada e renumera os seguintes. */
    public void removerTrecho(int ordem) {
        exigirEmEdicao("alterado");
        trechos.remove(indiceDoTrecho(ordem));
        for (int i = 0; i < trechos.size(); i++) {
            trechos.set(i, trechos.get(i).comOrdem(i + 1));
        }
    }

    // ------------------------------------------------------------------ UC06: validação

    /**
     * Verifica percurso, horários, regras da categoria, autonomia e disponibilidade atual dos recursos.
     * Não altera o plano nem reserva nada.
     */
    public ResultadoValidacao validar(VerificadorDisponibilidade disponibilidade) {
        exigirEmEdicao("validado");
        List<Violacao> violacoes = new ArrayList<>();
        if (trechos.isEmpty()) {
            violacoes.add(new Violacao(CodigoViolacao.PERCURSO_VAZIO, null, "O plano não possui trechos."));
            return new ResultadoValidacao(violacoes);
        }
        validarPercurso(violacoes);
        validarHorarios(violacoes);
        validarPousos(violacoes);
        validarOperacaoDaAeronave(violacoes);
        validarAutonomia(violacoes);
        validarDisponibilidade(disponibilidade, violacoes);
        return new ResultadoValidacao(violacoes);
    }

    private void validarPercurso(List<Violacao> violacoes) {
        Trecho primeiro = trechos.getFirst();
        Trecho ultimo = trechos.getLast();
        if (!primeiro.getAerovia().getOrigem().equals(origem)) {
            violacoes.add(new Violacao(CodigoViolacao.ORIGEM_INCOERENTE, primeiro.getOrdem(),
                    "A aerovia " + primeiro.getAerovia() + " não parte da origem do plano (" + origem + ")."));
        }
        for (int i = 1; i < trechos.size(); i++) {
            Trecho anterior = trechos.get(i - 1);
            Trecho atual = trechos.get(i);
            if (!atual.getAerovia().getOrigem().equals(anterior.getAerovia().getDestino())) {
                violacoes.add(new Violacao(CodigoViolacao.PERCURSO_DESCONTINUO, atual.getOrdem(),
                        "A aerovia " + atual.getAerovia() + " não começa onde a anterior termina ("
                                + anterior.getAerovia().getDestino() + ")."));
            }
        }
        if (!ultimo.getAerovia().getDestino().equals(destino)) {
            violacoes.add(new Violacao(CodigoViolacao.DESTINO_INCOERENTE, ultimo.getOrdem(),
                    "A aerovia " + ultimo.getAerovia() + " não chega ao destino do plano (" + destino + ")."));
        }
        if (!ultimo.temPouso()) {
            violacoes.add(new Violacao(CodigoViolacao.SEM_POUSO_FINAL, ultimo.getOrdem(),
                    "É obrigatório pousar no destino final."));
        }
    }

    private void validarHorarios(List<Violacao> violacoes) {
        double velocidade = aeronave.getVelocidadeCruzeiroKmh();
        Trecho primeiro = trechos.getFirst();
        if (!primeiro.getHorarioEntrada().equals(dataHoraPartida)) {
            violacoes.add(new Violacao(CodigoViolacao.ENTRADA_DIFERENTE_PARTIDA, primeiro.getOrdem(),
                    "A entrada no primeiro trecho (" + primeiro.getHorarioEntrada()
                            + ") deve ser igual à partida do plano (" + dataHoraPartida + ")."));
        }
        for (int i = 1; i < trechos.size(); i++) {
            Trecho anterior = trechos.get(i - 1);
            Trecho atual = trechos.get(i);
            LocalDateTime esperado = anterior.horarioLiberacao(velocidade);
            if (!atual.getHorarioEntrada().equals(esperado)) {
                String motivo = anterior.temPouso()
                        ? "chegada calculada + permanência no pouso"
                        : "chegada calculada após sobrevoo";
                violacoes.add(new Violacao(CodigoViolacao.HORARIO_INCOMPATIVEL, atual.getOrdem(),
                        "Entrada em " + atual.getAerovia().getId() + " informada às " + atual.getHorarioEntrada()
                                + "; esperado " + esperado + " (" + motivo + ")."));
            }
        }
        LocalDateTime chegadaFinal = trechos.getLast().horarioChegada(velocidade);
        if (!chegadaFinal.toLocalDate().equals(dataHoraPartida.toLocalDate())) {
            violacoes.add(new Violacao(CodigoViolacao.DIAS_DIFERENTES, trechos.getLast().getOrdem(),
                    "A chegada ao destino final (" + chegadaFinal + ") precisa ocorrer na data da partida ("
                            + dataHoraPartida.toLocalDate() + ")."));
        }
    }

    private void validarPousos(List<Violacao> violacoes) {
        for (Trecho trecho : trechos) {
            trecho.getPouso().ifPresent(pouso -> {
                if (!pouso.vaga().pertenceA(trecho.getAerovia().getDestino())) {
                    violacoes.add(new Violacao(CodigoViolacao.VAGA_FORA_DO_AEROPORTO, trecho.getOrdem(),
                            "A vaga " + pouso.vaga() + " não pertence a " + trecho.getAerovia().getDestino() + "."));
                }
            });
        }
    }

    private void validarOperacaoDaAeronave(List<Violacao> violacoes) {
        double velocidade = aeronave.getVelocidadeCruzeiroKmh();
        for (Trecho trecho : trechos) {
            LocalDateTime ini = trecho.getHorarioEntrada();
            LocalDateTime fim = trecho.horarioChegada(velocidade);
            for (RegraOperacao regra : aeronave.regrasVioladas(trecho.getAltitude(), ini, fim)) {
                violacoes.add(new Violacao(regra.codigoViolacao(), trecho.getOrdem(),
                        "Aeronave " + aeronave.getPrefixo() + " (" + aeronave.categoria() + "): " + regra.descricao()
                                + "; trecho a " + trecho.getAltitude().pes() + " pés de " + ini + " a " + fim + "."));
            }
        }
    }

    private void validarAutonomia(List<Violacao> violacoes) {
        List<Double> acumuladas = distanciasDesdeAbastecimento();
        boolean jaApontadoNoSegmento = false;
        for (int i = 0; i < trechos.size(); i++) {
            Trecho trecho = trechos.get(i);
            if (acumuladas.get(i) > aeronave.autonomiaSegura() && !jaApontadoNoSegmento) {
                violacoes.add(new Violacao(CodigoViolacao.AUTONOMIA_EXCEDIDA, trecho.getOrdem(),
                        String.format("Distância desde o último abastecimento chega a %.0f km no fim de %s; "
                                        + "limite de 85%% da autonomia de %s = %.0f km.",
                                acumuladas.get(i), trecho.getAerovia().getId(), aeronave.getPrefixo(),
                                aeronave.autonomiaSegura())));
                jaApontadoNoSegmento = true;
            }
            if (trecho.temPouso()) {
                jaApontadoNoSegmento = false;
            }
        }
    }

    /** Uso de uma altitude de uma aerovia num slot; a mesma chave da UNIQUE de reserva_aerovia. */
    private record UsoAltitude(String aeroviaId, Altitude altitude, SlotHorario slot) {
    }

    /** Uso de uma vaga num slot; a mesma chave da UNIQUE de reserva_vaga. */
    private record UsoVaga(Vaga vaga, SlotHorario slot) {
    }

    /**
     * Cada recurso precisa estar livre em todos os slots, tanto em relação às reservas já gravadas (outros planos)
     * quanto aos trechos anteriores deste mesmo plano. As duas checagens são independentes: um mesmo slot pode
     * gerar as duas violações.
     */
    private void validarDisponibilidade(VerificadorDisponibilidade disponibilidade, List<Violacao> violacoes) {
        double velocidade = aeronave.getVelocidadeCruzeiroKmh();
        // recurso -> ordem do primeiro trecho deste plano que o usa
        Map<UsoAltitude, Integer> altitudesUsadas = new HashMap<>();
        Map<UsoVaga, Integer> vagasUsadas = new HashMap<>();
        for (Trecho trecho : trechos) {
            String aeroviaId = trecho.getAerovia().getId();
            int pes = trecho.getAltitude().pes();
            for (SlotHorario slot : trecho.slots(velocidade)) {
                if (disponibilidade.altitudeOcupada(aeroviaId, trecho.getAltitude(), slot)) {
                    violacoes.add(new Violacao(CodigoViolacao.ALTITUDE_OCUPADA, trecho.getOrdem(),
                            "Altitude " + pes + " de " + aeroviaId + " já reservada no slot " + slot.inicio() + "."));
                }
                Integer outroTrecho = altitudesUsadas.putIfAbsent(new UsoAltitude(aeroviaId, trecho.getAltitude(), slot),
                        trecho.getOrdem());
                if (outroTrecho != null) {
                    violacoes.add(new Violacao(CodigoViolacao.ALTITUDE_OCUPADA, trecho.getOrdem(),
                            "Altitude " + pes + " de " + aeroviaId + " já usada pelo trecho " + outroTrecho
                                    + " deste plano no slot " + slot.inicio() + "."));
                }
            }
            Pouso pouso = trecho.getPouso().orElse(null);
            if (pouso != null) {
                for (SlotHorario slot : trecho.slotsPouso(velocidade)) {
                    if (disponibilidade.vagaOcupada(pouso.vaga(), slot)) {
                        violacoes.add(new Violacao(CodigoViolacao.VAGA_OCUPADA, trecho.getOrdem(),
                                "Vaga " + pouso.vaga() + " já reservada no slot " + slot.inicio() + "."));
                    }
                    Integer outroTrecho = vagasUsadas.putIfAbsent(new UsoVaga(pouso.vaga(), slot), trecho.getOrdem());
                    if (outroTrecho != null) {
                        violacoes.add(new Violacao(CodigoViolacao.VAGA_OCUPADA, trecho.getOrdem(),
                                "Vaga " + pouso.vaga() + " já usada pelo pouso do trecho " + outroTrecho
                                        + " deste plano no slot " + slot.inicio() + "."));
                    }
                }
            }
        }
    }

    /** Distância acumulada desde o último abastecimento ao fim de cada trecho. Sobrevoo acumula; pouso zera depois. */
    public List<Double> distanciasDesdeAbastecimento() {
        List<Double> acumuladas = new ArrayList<>();
        double acumulado = 0;
        for (Trecho trecho : trechos) {
            acumulado += trecho.getAerovia().getDistanciaKm();
            acumuladas.add(acumulado);
            if (trecho.temPouso()) {
                acumulado = 0;
            }
        }
        return acumuladas;
    }

    // ------------------------------------------------------------------ UC07, UC10, UC11: ciclo de vida

    /**
     * UC07: revalida e, se tudo estiver certo, gera uma reserva por slot de cada trecho e de cada pouso
     * e passa a APROVADO. A gravação das reservas (tudo ou nada) é responsabilidade do caso de uso.
     *
     * @return as reservas a gravar
     * @throws PlanoInvalidoException se houver qualquer violação; o plano continua em edição
     */
    public List<Reserva> aprovar(VerificadorDisponibilidade disponibilidade) {
        ResultadoValidacao resultado = validar(disponibilidade);
        if (!resultado.valido()) {
            throw new PlanoInvalidoException(resultado);
        }
        double velocidade = aeronave.getVelocidadeCruzeiroKmh();
        List<Reserva> novas = new ArrayList<>();
        for (Trecho trecho : trechos) {
            for (SlotHorario slot : trecho.slots(velocidade)) {
                novas.add(new ReservaAerovia(null, id, slot, trecho.getAerovia(), trecho.getAltitude()));
            }
            Pouso pouso = trecho.getPouso().orElse(null);
            if (pouso != null) {
                for (SlotHorario slot : trecho.slotsPouso(velocidade)) {
                    novas.add(new ReservaVaga(null, id, slot, pouso.vaga()));
                }
            }
        }
        reservas.clear();
        reservas.addAll(novas);
        situacao = SituacaoPlano.APROVADO;
        return List.copyOf(novas);
    }

    /** UC11: só plano aprovado cuja partida ainda não ocorreu. As reservas deixam de pertencer ao plano. */
    public void cancelar(LocalDateTime agora) {
        if (situacao != SituacaoPlano.APROVADO) {
            throw new SituacaoInvalidaException("PLANO_NAO_CANCELAVEL",
                    "Somente planos aprovados podem ser cancelados; o plano " + id + " está " + situacao + ".");
        }
        if (!dataHoraPartida.isAfter(agora)) {
            throw new SituacaoInvalidaException("VOO_JA_EXECUTADO",
                    "A partida do plano " + id + " (" + dataHoraPartida + ") já ocorreu; o voo foi executado e não pode ser cancelado.");
        }
        situacao = SituacaoPlano.CANCELADO;
        reservas.clear();
    }

    /** UC10: plano em edição passa a abandonado. Não há reservas a liberar. */
    public void abandonar() {
        exigirEmEdicao("abandonado");
        situacao = SituacaoPlano.ABANDONADO;
    }

    /** Falha com 409 se o plano não estiver em edição. Permite checar antes de resolver referências da edição. */
    public void garantirEditavel() {
        exigirEmEdicao("alterado");
    }

    private void exigirEmEdicao(String operacao) {
        if (situacao != SituacaoPlano.EM_EDICAO) {
            throw new SituacaoInvalidaException("PLANO_NAO_EDITAVEL",
                    "O plano " + id + " está " + situacao + " e não pode ser " + operacao + "; somente planos EM_EDICAO.");
        }
    }

    private int indiceDoTrecho(int ordem) {
        if (ordem < 1 || ordem > trechos.size()) {
            throw new RecursoNaoEncontradoException("O plano " + id + " não possui trecho de ordem " + ordem + ".");
        }
        return ordem - 1;
    }

    public Long getId() { return id; }
    public Piloto getPiloto() { return piloto; }
    public Aeronave getAeronave() { return aeronave; }
    public Aeroporto getOrigem() { return origem; }
    public Aeroporto getDestino() { return destino; }
    public LocalDateTime getDataHoraPartida() { return dataHoraPartida; }
    public SituacaoPlano getSituacao() { return situacao; }
    public List<Trecho> getTrechos() { return List.copyOf(trechos); }
    public List<Reserva> getReservas() { return List.copyOf(reservas); }
    public Long getVersao() { return versao; }
}
