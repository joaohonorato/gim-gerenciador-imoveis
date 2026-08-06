package br.com.imoveis.infrastructure.persistence.jpa;

import io.micronaut.data.annotation.Repository;
import io.micronaut.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CategoriaChamadoJpaRepository extends CrudRepository<CategoriaChamadoJpaEntity, UUID> {
    List<CategoriaChamadoJpaEntity> findByProprietarioId(UUID proprietarioId);
    Optional<CategoriaChamadoJpaEntity> findByProprietarioIdAndNome(UUID proprietarioId, String nome);
}
