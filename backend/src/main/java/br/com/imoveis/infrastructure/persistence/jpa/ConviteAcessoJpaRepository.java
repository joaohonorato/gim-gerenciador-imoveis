package br.com.imoveis.infrastructure.persistence.jpa;

import io.micronaut.data.annotation.Repository;
import io.micronaut.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConviteAcessoJpaRepository extends CrudRepository<ConviteAcessoJpaEntity, UUID> {
    Optional<ConviteAcessoJpaEntity> findByToken(String token);
    List<ConviteAcessoJpaEntity> findByEmailOrderByCriadoEmDesc(String email);
}