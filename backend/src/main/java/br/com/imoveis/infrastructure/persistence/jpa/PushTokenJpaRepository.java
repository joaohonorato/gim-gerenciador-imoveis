package br.com.imoveis.infrastructure.persistence.jpa;

import io.micronaut.data.annotation.Repository;
import io.micronaut.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PushTokenJpaRepository extends CrudRepository<PushTokenJpaEntity, UUID> {
    Optional<PushTokenJpaEntity> findByToken(String token);
    List<PushTokenJpaEntity> findByContaAcessoId(UUID contaAcessoId);
}
