package br.com.imoveis.infrastructure.persistence.jpa;

import br.com.imoveis.domain.imovel.ContaStatus;
import io.micronaut.data.annotation.Repository;
import io.micronaut.data.repository.CrudRepository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface ContaJpaRepository extends CrudRepository<ContaJpaEntity, UUID> {
    List<ContaJpaEntity> findByUnidadeIdIn(Collection<UUID> unidadeIds);
    List<ContaJpaEntity> findByStatusAndAlertaEnviadoFalseAndVencimentoBetween(
        ContaStatus status, LocalDate inicio, LocalDate fim);
}
