package br.pucrs.projarc.gestaoplanosvoo.application.usecases;

import br.pucrs.projarc.gestaoplanosvoo.application.dto.response.PlanoResumoResponse;
import br.pucrs.projarc.gestaoplanosvoo.domain.model.PlanoDeVoo;
import br.pucrs.projarc.gestaoplanosvoo.domain.model.SituacaoPlano;
import br.pucrs.projarc.gestaoplanosvoo.domain.repository.PlanoDeVooRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** UC09: lista os planos de uma situação, ou todos quando a situação não é informada. */
@Service
public class ListarPlanosUseCase {

    private final PlanoDeVooRepositoryPort planos;

    public ListarPlanosUseCase(PlanoDeVooRepositoryPort planos) {
        this.planos = planos;
    }

    @Transactional(readOnly = true)
    public List<PlanoResumoResponse> executar(SituacaoPlano situacao) {
        List<PlanoDeVoo> encontrados = situacao == null ? planos.listarTodos() : planos.listarPorSituacao(situacao);
        return encontrados.stream().map(PlanoResumoResponse::de).toList();
    }
}
