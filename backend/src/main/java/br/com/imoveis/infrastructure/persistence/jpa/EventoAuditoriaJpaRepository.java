package br.com.imoveis.infrastructure.persistence.jpa;

import br.com.imoveis.domain.auditoria.EntidadeAuditoria;
import io.micronaut.data.annotation.Repository;
import io.micronaut.data.repository.CrudRepository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EventoAuditoriaJpaRepository extends CrudRepository<EventoAuditoriaJpaEntity, UUID> {
    List<EventoAuditoriaJpaEntity> findByEntidadeTipoAndEntidadeIdOrderByCriadoEmAsc(
        EntidadeAuditoria entidadeTipo, UUID entidadeId);
}
