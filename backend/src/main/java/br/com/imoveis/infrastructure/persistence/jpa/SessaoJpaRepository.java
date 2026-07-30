package br.com.imoveis.infrastructure.persistence.jpa;

import io.micronaut.data.annotation.Repository;
import io.micronaut.data.repository.CrudRepository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SessaoJpaRepository extends CrudRepository<SessaoJpaEntity, UUID> {
    Optional<SessaoJpaEntity> findByToken(String token);
    long deleteByToken(String token);
}
