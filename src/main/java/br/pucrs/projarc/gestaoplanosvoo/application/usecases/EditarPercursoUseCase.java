package br.pucrs.projarc.gestaoplanosvoo.application.usecases;

import br.pucrs.projarc.gestaoplanosvoo.application.dto.request.PousoRequest;
import br.pucrs.projarc.gestaoplanosvoo.application.dto.request.TrechoRequest;
import br.pucrs.projarc.gestaoplanosvoo.application.dto.response.PlanoResponse;
import br.pucrs.projarc.gestaoplanosvoo.domain.exception.RecursoNaoEncontradoException;
import br.pucrs.projarc.gestaoplanosvoo.domain.model.Aerovia;
import br.pucrs.projarc.gestaoplanosvoo.domain.model.Altitude;
import br.pucrs.projarc.gestaoplanosvoo.domain.model.PlanoDeVoo;
import br.pucrs.projarc.gestaoplanosvoo.domain.model.Pouso;
import br.pucrs.projarc.gestaoplanosvoo.domain.model.Vaga;
import br.pucrs.projarc.gestaoplanosvoo.domain.repository.MalhaRepositoryPort;
import br.pucrs.projarc.gestaoplanosvoo.domain.repository.PlanoDeVooRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * UC03 — Editar percurso: acrescentar trecho no fim, substituir ou remover o trecho de uma ordem.
 * Só em EM_EDICAO; não reserva recursos.
 */
@Service
public class EditarPercursoUseCase {

    private final PlanoDeVooRepositoryPort planos;
    private final MalhaRepositoryPort malha;

    public EditarPercursoUseCase(PlanoDeVooRepositoryPort planos, MalhaRepositoryPort malha) {
        this.planos = planos;
        this.malha = malha;
    }

    @Transactional
    public PlanoResponse adicionar(Long planoId, TrechoRequest req) {
        PlanoDeVoo plano = carregarEditavel(planoId);
        Aerovia aerovia = aerovia(req.aerovia());
        plano.adicionarTrecho(aerovia, new Altitude(req.altitudePes()), req.horarioEntrada(), pouso(aerovia, req.pouso()));
        return PlanoResponse.de(planos.salvar(plano));
    }

    @Transactional
    public PlanoResponse alterar(Long planoId, int ordem, TrechoRequest req) {
        PlanoDeVoo plano = carregarEditavel(planoId);
        Aerovia aerovia = aerovia(req.aerovia());
        plano.alterarTrecho(ordem, aerovia, new Altitude(req.altitudePes()), req.horarioEntrada(), pouso(aerovia, req.pouso()));
        return PlanoResponse.de(planos.salvar(plano));
    }

    @Transactional
    public PlanoResponse remover(Long planoId, int ordem) {
        PlanoDeVoo plano = carregarEditavel(planoId);
        plano.removerTrecho(ordem);
        return PlanoResponse.de(planos.salvar(plano));
    }

    private PlanoDeVoo carregarEditavel(Long planoId) {
        PlanoDeVoo plano = planos.buscarPorId(planoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Plano " + planoId + " não encontrado."));
        plano.garantirEditavel();
        return plano;
    }

    private Aerovia aerovia(String id) {
        return malha.buscarAerovia(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Aerovia " + id + " não encontrada."));
    }

    /** A vaga é procurada no aeroporto de destino da aerovia do trecho. */
    private Pouso pouso(Aerovia aerovia, PousoRequest req) {
        if (req == null) {
            return null;
        }
        Vaga vaga = aerovia.getDestino().vaga(req.vaga())
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Vaga " + req.vaga() + " não existe em " + aerovia.getDestino().getCodigo() + "."));
        return new Pouso(vaga, req.permanenciaMin());
    }
}
