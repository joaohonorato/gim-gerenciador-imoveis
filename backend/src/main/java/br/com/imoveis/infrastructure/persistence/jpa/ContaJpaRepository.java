package br.com.imoveis.infrastructure.persistence.jpa;

import io.micronaut.data.annotation.Repository;
import io.micronaut.data.repository.CrudRepository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ContaJpaRepository extends CrudRepository<ContaJpaEntity, UUID> {
    List<ContaJpaEntity> findByImovelId(UUID imovelId);
}
