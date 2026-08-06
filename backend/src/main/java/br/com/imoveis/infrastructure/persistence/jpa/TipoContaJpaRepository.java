package br.com.imoveis.infrastructure.persistence.jpa;

import io.micronaut.data.annotation.Repository;
import io.micronaut.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TipoContaJpaRepository extends CrudRepository<TipoContaJpaEntity, UUID> {
    List<TipoContaJpaEntity> findByProprietarioId(UUID proprietarioId);
    Optional<TipoContaJpaEntity> findByProprietarioIdAndNome(UUID proprietarioId, String nome);
}
