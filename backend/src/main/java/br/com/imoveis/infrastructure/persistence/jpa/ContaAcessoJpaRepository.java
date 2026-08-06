package br.com.imoveis.infrastructure.persistence.jpa;

import io.micronaut.data.annotation.Repository;
import io.micronaut.data.repository.CrudRepository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ContaAcessoJpaRepository extends CrudRepository<ContaAcessoJpaEntity, UUID> {
    Optional<ContaAcessoJpaEntity> findByEmail(String email);
    Optional<ContaAcessoJpaEntity> findByEmailPendente(String emailPendente);
    Optional<ContaAcessoJpaEntity> findByProprietarioId(UUID proprietarioId);
    Optional<ContaAcessoJpaEntity> findByInquilinoId(UUID inquilinoId);
}