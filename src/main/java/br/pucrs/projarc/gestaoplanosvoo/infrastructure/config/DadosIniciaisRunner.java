package br.pucrs.projarc.gestaoplanosvoo.infrastructure.config;

import br.pucrs.projarc.gestaoplanosvoo.application.dto.request.CriarPlanoRequest;
import br.pucrs.projarc.gestaoplanosvoo.application.dto.request.PousoRequest;
import br.pucrs.projarc.gestaoplanosvoo.application.dto.request.TrechoRequest;
import br.pucrs.projarc.gestaoplanosvoo.application.dto.response.PlanoResponse;
import br.pucrs.projarc.gestaoplanosvoo.application.usecases.AprovarPlanoUseCase;
import br.pucrs.projarc.gestaoplanosvoo.application.usecases.CriarPlanoUseCase;
import br.pucrs.projarc.gestaoplanosvoo.application.usecases.EditarPercursoUseCase;
import br.pucrs.projarc.gestaoplanosvoo.domain.repository.PlanoDeVooRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Component
public class DadosIniciaisRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DadosIniciaisRunner.class);

    private final PlanoDeVooRepositoryPort planos;
    private final CriarPlanoUseCase criar;
    private final EditarPercursoUseCase editar;
    private final AprovarPlanoUseCase aprovar;
    private final LocalDate dia;

    public DadosIniciaisRunner(PlanoDeVooRepositoryPort planos, CriarPlanoUseCase criar, EditarPercursoUseCase editar,
                               AprovarPlanoUseCase aprovar, @Value("${app.data-referencia}") LocalDate dataReferencia) {
        this.planos = planos;
        this.criar = criar;
        this.editar = editar;
        this.aprovar = aprovar;
        this.dia = dataReferencia;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (planos.existeAlgum()) {
            log.info("Planos já existem no banco; carga inicial ignorada. Data de referência: {}", dia);
            return;
        }

        // 1) PR-AZA POA->GRU por R2 a 30.000: 08:10 -> 09:12; pouso GRU-1 por 60 min.
        //    Reservas: R2/30000 08h e 09h; GRU-1 09h e 10h.
        Long p1 = plano(1L, "PR-AZA", "POA", "GRU", as(8, 10));
        trecho(p1, "R2", 30_000, as(8, 10), 1, 60);
        aprovar.executar(p1);

        // 2) PP-JAT POA->CWB: R1 a 26.000 14:00 -> 14:37 (sobrevoo FLN); R4 a 26.000 14:37 -> 15:02; pouso CWB-1 45 min.
        Long p2 = plano(2L, "PP-JAT", "POA", "CWB", as(14, 0));
        trecho(p2, "R1", 26_000, as(14, 0), null, null);
        trecho(p2, "R4", 26_000, as(14, 37), 1, 45);
        aprovar.executar(p2);

        // 3) PR-CGA (carga) GRU->FLN: R7 a 32.000 01:00 -> 01:28; pouso CWB-2 30 min; R5 01:58 -> 02:17; pouso FLN-1 120 min.
        Long p3 = plano(3L, "PR-CGA", "GRU", "FLN", as(1, 0));
        trecho(p3, "R7", 32_000, as(1, 0), 2, 30);
        trecho(p3, "R5", 32_000, as(1, 58), 1, 120);
        aprovar.executar(p3);

        // 4) PR-GOB FLN->POA por R8 a 29.000: 10:30 -> 10:57; pouso POA-1 60 min.
        Long p4 = plano(4L, "PR-GOB", "FLN", "POA", as(10, 30));
        trecho(p4, "R8", 29_000, as(10, 30), 1, 60);
        aprovar.executar(p4);

        // 5) PR-AZA GRU->POA por R3 a 30.000: 18:00 -> 19:02; pouso POA-2 40 min.
        Long p5 = plano(1L, "PR-AZA", "GRU", "POA", as(18, 0));
        trecho(p5, "R3", 30_000, as(18, 0), 2, 40);
        aprovar.executar(p5);

        // 6) Voo já executado (partida 3 meses antes da data de referência): não pode ser cancelado.
        LocalDateTime passado = as(10, 30).minusMonths(3);
        Long p6 = plano(3L, "PR-GOB", "FLN", "POA", passado);
        trecho(p6, "R8", 29_000, passado, 1, 60);
        aprovar.executar(p6);

        // 7) Em edição, pronto para demonstrar autonomia excedida: POA->FLN (sobrevoo) ->CWB = 608 km > 595 km.
        Long p7 = plano(4L, "PP-LEN", "POA", "CWB", as(9, 0));
        trecho(p7, "R1", 25_000, as(9, 0), null, null);
        trecho(p7, "R4", 25_000, as(9, 55), 3, 30);

        log.info("Carga inicial concluída: planos aprovados {} a {} em {}, plano {} executado em {}, plano {} em edição.",
                p1, p5, dia, p6, passado.toLocalDate(), p7);
    }

    private Long plano(Long piloto, String aeronave, String origem, String destino, LocalDateTime partida) {
        PlanoResponse p = criar.executar(new CriarPlanoRequest(piloto, aeronave, origem, destino, partida));
        return p.id();
    }

    private void trecho(Long plano, String aerovia, int altitude, LocalDateTime entrada, Integer vaga, Integer permanencia) {
        PousoRequest pouso = vaga == null ? null : new PousoRequest(vaga, permanencia);
        editar.adicionar(plano, new TrechoRequest(aerovia, altitude, entrada, pouso));
    }

    private LocalDateTime as(int hora, int minuto) {
        return dia.atTime(hora, minuto);
    }
}
