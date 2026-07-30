package br.com.imoveis.infrastructure.persistence.jpa;

import io.micronaut.data.annotation.Repository;
import io.micronaut.data.repository.CrudRepository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ChamadoJpaRepository extends CrudRepository<ChamadoJpaEntity, UUID> {
    List<ChamadoJpaEntity> findByImovelId(UUID imovelId);
}
