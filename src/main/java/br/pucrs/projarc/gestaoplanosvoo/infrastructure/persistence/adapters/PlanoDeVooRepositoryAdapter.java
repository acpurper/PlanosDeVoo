package br.pucrs.projarc.gestaoplanosvoo.infrastructure.persistence.adapters;

import br.pucrs.projarc.gestaoplanosvoo.domain.model.PlanoDeVoo;
import br.pucrs.projarc.gestaoplanosvoo.domain.model.SituacaoPlano;
import br.pucrs.projarc.gestaoplanosvoo.domain.repository.PlanoDeVooRepositoryPort;
import br.pucrs.projarc.gestaoplanosvoo.domain.repository.ReservaRepositoryPort;
import br.pucrs.projarc.gestaoplanosvoo.infrastructure.persistence.entities.PlanoVooEntity;
import br.pucrs.projarc.gestaoplanosvoo.infrastructure.persistence.mappers.PlanoMapper;
import br.pucrs.projarc.gestaoplanosvoo.infrastructure.persistence.repositories.PlanoVooJpaRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class PlanoDeVooRepositoryAdapter implements PlanoDeVooRepositoryPort {

    private final PlanoVooJpaRepository planos;
    private final ReservaRepositoryPort reservas;
    private final PlanoMapper mapper;

    public PlanoDeVooRepositoryAdapter(PlanoVooJpaRepository planos, ReservaRepositoryPort reservas, PlanoMapper mapper) {
        this.planos = planos;
        this.reservas = reservas;
        this.mapper = mapper;
    }

    @Override
    public PlanoDeVoo salvar(PlanoDeVoo plano) {
        PlanoVooEntity entidade = plano.getId() == null
                ? new PlanoVooEntity()
                : planos.findById(plano.getId()).orElseThrow();
        mapper.copiar(plano, entidade);
        PlanoVooEntity salva = planos.saveAndFlush(entidade);
        return mapper.paraDominio(salva, plano.getReservas());
    }

    @Override
    public Optional<PlanoDeVoo> buscarPorId(Long id) {
        return planos.findById(id).map(e -> mapper.paraDominio(e, reservas.listarDoPlano(id)));
    }

    @Override
    public Optional<PlanoDeVoo> buscarPorIdParaAtualizacao(Long id) {
        return planos.buscarComBloqueio(id).map(e -> mapper.paraDominio(e, reservas.listarDoPlano(id)));
    }

    @Override
    public List<PlanoDeVoo> listarTodos() {
        return planos.findAllByOrderById().stream().map(this::paraDominio).toList();
    }

    @Override
    public List<PlanoDeVoo> listarPorSituacao(SituacaoPlano situacao) {
        return planos.findBySituacaoOrderById(situacao).stream().map(this::paraDominio).toList();
    }

    private PlanoDeVoo paraDominio(PlanoVooEntity e) {
        return mapper.paraDominio(e, reservas.listarDoPlano(e.getId()));
    }

    @Override
    public boolean existeAlgum() {
        return planos.count() > 0;
    }
}
