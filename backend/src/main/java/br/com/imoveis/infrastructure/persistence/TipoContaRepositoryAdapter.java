package br.com.imoveis.infrastructure.persistence;

import br.com.imoveis.application.ports.TipoContaRepository;
import br.com.imoveis.domain.imovel.TipoConta;
import br.com.imoveis.infrastructure.persistence.jpa.TipoContaJpaEntity;
import br.com.imoveis.infrastructure.persistence.jpa.TipoContaJpaRepository;
import io.micronaut.transaction.annotation.Transactional;
import jakarta.inject.Singleton;
import jakarta.persistence.EntityManager;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Singleton
@Transactional
public class TipoContaRepositoryAdapter implements TipoContaRepository {

    private final TipoContaJpaRepository jpa;
    private final EntityManager em;

    public TipoContaRepositoryAdapter(TipoContaJpaRepository jpa, EntityManager em) {
        this.jpa = jpa;
        this.em = em;
    }

    @Override
    public TipoConta save(TipoConta tipoConta) {
        TipoContaJpaEntity entity = jpa.findById(tipoConta.id()).orElseGet(TipoContaJpaEntity::new);
        entity.setId(tipoConta.id());
        entity.setProprietarioId(tipoConta.proprietarioId());
        entity.setNome(tipoConta.nome());
        entity.setCriadoEm(tipoConta.criadoEm());
        em.merge(entity);
        return tipoConta;
    }

    @Override
    public Optional<TipoConta> findById(UUID id) {
        return jpa.findById(id).map(this::toDomain);
    }

    @Override
    public List<TipoConta> findByProprietarioId(UUID proprietarioId) {
        return jpa.findByProprietarioId(proprietarioId).stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<TipoConta> findByProprietarioIdAndNome(UUID proprietarioId, String nome) {
        return jpa.findByProprietarioIdAndNome(proprietarioId, nome).map(this::toDomain);
    }

    private TipoConta toDomain(TipoContaJpaEntity entity) {
        return TipoConta.reconstituir(entity.getId(), entity.getProprietarioId(), entity.getNome(), entity.getCriadoEm());
    }
}
