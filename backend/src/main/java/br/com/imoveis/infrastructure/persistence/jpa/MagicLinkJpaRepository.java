package br.com.imoveis.infrastructure.persistence.jpa;

import io.micronaut.data.annotation.Repository;
import io.micronaut.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MagicLinkJpaRepository extends CrudRepository<MagicLinkJpaEntity, UUID> {
    Optional<MagicLinkJpaEntity> findByToken(String token);
    List<MagicLinkJpaEntity> findByEmailOrderByCriadoEmDesc(String email);
}
