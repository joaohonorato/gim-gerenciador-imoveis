package br.com.imoveis.infrastructure.persistence.jpa;

import io.micronaut.data.annotation.Repository;
import io.micronaut.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConviteJpaRepository extends CrudRepository<ConviteJpaEntity, UUID> {
    Optional<ConviteJpaEntity> findByToken(String token);
    List<ConviteJpaEntity> findByProprietarioId(UUID proprietarioId);
}
