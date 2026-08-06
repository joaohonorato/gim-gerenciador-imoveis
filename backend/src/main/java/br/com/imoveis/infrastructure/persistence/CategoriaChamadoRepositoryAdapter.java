package br.com.imoveis.infrastructure.persistence;

import br.com.imoveis.application.ports.CategoriaChamadoRepository;
import br.com.imoveis.domain.chamado.CategoriaChamado;
import br.com.imoveis.infrastructure.persistence.jpa.CategoriaChamadoJpaEntity;
import br.com.imoveis.infrastructure.persistence.jpa.CategoriaChamadoJpaRepository;
import io.micronaut.transaction.annotation.Transactional;
import jakarta.inject.Singleton;
import jakarta.persistence.EntityManager;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Singleton
@Transactional
public class CategoriaChamadoRepositoryAdapter implements CategoriaChamadoRepository {

    private final CategoriaChamadoJpaRepository jpa;
    private final EntityManager em;

    public CategoriaChamadoRepositoryAdapter(CategoriaChamadoJpaRepository jpa, EntityManager em) {
        this.jpa = jpa;
        this.em = em;
    }

    @Override
    public CategoriaChamado save(CategoriaChamado categoriaChamado) {
        CategoriaChamadoJpaEntity entity = jpa.findById(categoriaChamado.id()).orElseGet(CategoriaChamadoJpaEntity::new);
        entity.setId(categoriaChamado.id());
        entity.setProprietarioId(categoriaChamado.proprietarioId());
        entity.setNome(categoriaChamado.nome());
        entity.setCriadoEm(categoriaChamado.criadoEm());
        em.merge(entity);
        return categoriaChamado;
    }

    @Override
    public Optional<CategoriaChamado> findById(UUID id) {
        return jpa.findById(id).map(this::toDomain);
    }

    @Override
    public List<CategoriaChamado> findByProprietarioId(UUID proprietarioId) {
        return jpa.findByProprietarioId(proprietarioId).stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<CategoriaChamado> findByProprietarioIdAndNome(UUID proprietarioId, String nome) {
        return jpa.findByProprietarioIdAndNome(proprietarioId, nome).map(this::toDomain);
    }

    private CategoriaChamado toDomain(CategoriaChamadoJpaEntity entity) {
        return CategoriaChamado.reconstituir(entity.getId(), entity.getProprietarioId(), entity.getNome(), entity.getCriadoEm());
    }
}
