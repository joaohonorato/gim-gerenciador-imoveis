package br.com.imoveis.infrastructure.persistence.jpa;

import io.micronaut.data.annotation.Repository;
import io.micronaut.data.repository.CrudRepository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CandidaturaJpaRepository extends CrudRepository<CandidaturaJpaEntity, UUID> {
	List<CandidaturaJpaEntity> findByInquilinoId(UUID inquilinoId);
	List<CandidaturaJpaEntity> findByConviteIdIn(List<UUID> conviteIds);
}
