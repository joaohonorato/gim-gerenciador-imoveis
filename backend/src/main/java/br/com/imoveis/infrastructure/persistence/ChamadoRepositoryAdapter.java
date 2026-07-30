package br.com.imoveis.infrastructure.persistence;

import br.com.imoveis.application.ports.ChamadoRepository;
import br.com.imoveis.domain.chamado.Chamado;
import br.com.imoveis.infrastructure.persistence.jpa.ChamadoJpaEntity;
import br.com.imoveis.infrastructure.persistence.jpa.ChamadoJpaRepository;
import io.micronaut.transaction.annotation.Transactional;
import jakarta.inject.Singleton;
import jakarta.persistence.EntityManager;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Singleton
@Transactional
public class ChamadoRepositoryAdapter implements ChamadoRepository {

    private final ChamadoJpaRepository jpa;
    private final EntityManager em;

    public ChamadoRepositoryAdapter(ChamadoJpaRepository jpa, EntityManager em) {
        this.jpa = jpa;
        this.em = em;
    }

    @Override
    public Chamado save(Chamado c) {
        ChamadoJpaEntity e = jpa.findById(c.id()).orElseGet(ChamadoJpaEntity::new);
        e.setId(c.id());
        e.setImovelId(c.imovelId());
        e.setAbertoPor(c.abertoPor());
        e.setCategoria(c.categoria());
        e.setDescricao(c.descricao());
        e.setStatus(c.status());
        e.setAbertoEm(c.abertoEm());
        e.setResolvidoEm(c.resolvidoEm());
        em.merge(e);
        return c;
    }

    @Override public Optional<Chamado> findById(UUID id) { return jpa.findById(id).map(this::toDomain); }
    @Override public List<Chamado> findByImovel(UUID imovelId) {
        return jpa.findByImovelId(imovelId).stream().map(this::toDomain).toList();
    }

    private Chamado toDomain(ChamadoJpaEntity e) {
        return Chamado.reconstituir(e.getId(), e.getImovelId(), e.getAbertoPor(),
            e.getCategoria(), e.getDescricao(), e.getStatus(), e.getAbertoEm(), e.getResolvidoEm());
    }
}
