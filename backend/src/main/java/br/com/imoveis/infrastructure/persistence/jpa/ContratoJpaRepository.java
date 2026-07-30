package br.com.imoveis.infrastructure.persistence.jpa;

import br.com.imoveis.domain.contrato.ContratoAssinaturaStatus;
import io.micronaut.data.annotation.Repository;
import io.micronaut.data.repository.CrudRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ContratoJpaRepository extends CrudRepository<ContratoJpaEntity, UUID> {
    List<ContratoJpaEntity> findByInquilinoId(UUID inquilinoId);
    List<ContratoJpaEntity> findByProprietarioId(UUID proprietarioId);
    List<ContratoJpaEntity> findByUnidadeIdAndStatusAssinatura(UUID unidadeId, ContratoAssinaturaStatus status);
    Optional<ContratoJpaEntity> findByUnidadeIdAndInquilinoIdAndDataInicioAndDataFim(
        UUID unidadeId, UUID inquilinoId, LocalDate dataInicio, LocalDate dataFim);
}
