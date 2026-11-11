package br.pucrs.projarc.gestaoplanosvoo.application.usecases;

import br.pucrs.projarc.gestaoplanosvoo.application.dto.response.PlanoResumoResponse;
import br.pucrs.projarc.gestaoplanosvoo.domain.model.PlanoDeVoo;
import br.pucrs.projarc.gestaoplanosvoo.domain.model.SituacaoPlano;
import br.pucrs.projarc.gestaoplanosvoo.domain.repository.PlanoDeVooRepositoryPort;
import br.pucrs.projarc.gestaoplanosvoo.domain.service.ReservaService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** UC09: lista os planos de uma situação, ou todos quando a situação não é informada. */
@Service
public class ListarPlanosUseCase {

    private final PlanoDeVooRepositoryPort planos;
    private final ReservaService reservaService;

    public ListarPlanosUseCase(PlanoDeVooRepositoryPort planos, ReservaService reservaService) {
        this.planos = planos;
        this.reservaService = reservaService;
    }

    @Transactional(readOnly = true)
    public List<PlanoResumoResponse> executar(SituacaoPlano situacao) {
        List<PlanoDeVoo> encontrados = situacao == null ? planos.listarTodos() : planos.listarPorSituacao(situacao);
        return encontrados.stream()
                .map(p -> PlanoResumoResponse.de(p, reservaService.doPlano(p.getId()).size()))
                .toList();
    }
}
