package br.com.imoveis.infrastructure.persistence.jpa;

import io.micronaut.data.annotation.Repository;
import io.micronaut.data.repository.CrudRepository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProprietarioJpaRepository extends CrudRepository<ProprietarioJpaEntity, UUID> {
    Optional<ProprietarioJpaEntity> findByEmail(String email);
    Optional<ProprietarioJpaEntity> findByCpfCnpj(String cpfCnpj);
}
