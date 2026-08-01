package br.com.imoveis.infrastructure.persistence.jpa;

import br.com.imoveis.domain.arquivo.TipoArquivo;
import io.micronaut.data.annotation.Repository;
import io.micronaut.data.repository.CrudRepository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ArquivoJpaRepository extends CrudRepository<ArquivoJpaEntity, UUID> {
    List<ArquivoJpaEntity> findByDonoIdAndTipo(UUID donoId, TipoArquivo tipo);
}
