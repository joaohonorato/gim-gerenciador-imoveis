package br.com.imoveis.infrastructure.persistence.jpa;

import io.micronaut.data.annotation.Repository;
import io.micronaut.data.repository.CrudRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface UnidadeJpaRepository extends CrudRepository<UnidadeJpaEntity, UUID> {
    List<UnidadeJpaEntity> findByImovelId(UUID imovelId);
    List<UnidadeJpaEntity> findByImovelIdIn(Collection<UUID> imovelIds);
}
