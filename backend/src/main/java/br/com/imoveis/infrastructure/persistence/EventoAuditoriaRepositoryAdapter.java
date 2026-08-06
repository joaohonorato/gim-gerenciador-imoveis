package br.com.imoveis.infrastructure.persistence;

import br.com.imoveis.application.ports.EventoAuditoriaRepository;
import br.com.imoveis.domain.auditoria.EntidadeAuditoria;
import br.com.imoveis.domain.auditoria.EventoAuditoria;
import br.com.imoveis.infrastructure.persistence.jpa.EventoAuditoriaJpaEntity;
import br.com.imoveis.infrastructure.persistence.jpa.EventoAuditoriaJpaRepository;
import io.micronaut.transaction.annotation.Transactional;
import jakarta.inject.Singleton;
import jakarta.persistence.EntityManager;

import java.util.List;
import java.util.UUID;

@Singleton
@Transactional
public class EventoAuditoriaRepositoryAdapter implements EventoAuditoriaRepository {

    private final EventoAuditoriaJpaRepository jpa;
    private final EntityManager em;

    public EventoAuditoriaRepositoryAdapter(EventoAuditoriaJpaRepository jpa, EntityManager em) {
        this.jpa = jpa;
        this.em = em;
    }

    @Override
    public EventoAuditoria save(EventoAuditoria evento) {
        EventoAuditoriaJpaEntity e = new EventoAuditoriaJpaEntity();
        e.setId(evento.id());
        e.setEntidadeTipo(evento.entidadeTipo());
        e.setEntidadeId(evento.entidadeId());
        e.setTipoEvento(evento.tipoEvento());
        e.setDetalhe(evento.detalhe());
        e.setCriadoEm(evento.criadoEm());
        em.persist(e);
        return evento;
    }

    @Override
    public List<EventoAuditoria> findByEntidade(EntidadeAuditoria entidadeTipo, UUID entidadeId) {
        return jpa.findByEntidadeTipoAndEntidadeIdOrderByCriadoEmAsc(entidadeTipo, entidadeId).stream()
            .map(this::toDomain)
            .toList();
    }

    private EventoAuditoria toDomain(EventoAuditoriaJpaEntity e) {
        return EventoAuditoria.reconstituir(e.getId(), e.getEntidadeTipo(), e.getEntidadeId(),
            e.getTipoEvento(), e.getDetalhe(), e.getCriadoEm());
    }
}
