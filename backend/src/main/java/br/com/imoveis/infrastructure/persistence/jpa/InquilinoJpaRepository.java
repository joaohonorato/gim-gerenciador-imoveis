package br.com.imoveis.infrastructure.persistence.jpa;

import io.micronaut.data.annotation.Repository;
import io.micronaut.data.repository.CrudRepository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface InquilinoJpaRepository extends CrudRepository<InquilinoJpaEntity, UUID> {
    Optional<InquilinoJpaEntity> findByCpf(String cpf);
    Optional<InquilinoJpaEntity> findByEmail(String email);
}
