package br.com.imoveis.infrastructure.persistence.jpa;

import io.micronaut.data.annotation.Repository;
import io.micronaut.data.repository.CrudRepository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PagamentoJpaRepository extends CrudRepository<PagamentoJpaEntity, UUID> {
    List<PagamentoJpaEntity> findByContratoIdOrderByVencimentoAsc(UUID contratoId);
    List<PagamentoJpaEntity> findByContratoIdIn(List<UUID> contratoIds);
}
