package br.com.imoveis.infrastructure.persistence.jpa;

import br.com.imoveis.domain.auth.TokenContaFinalidade;
import io.micronaut.data.annotation.Repository;
import io.micronaut.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TokenContaJpaRepository extends CrudRepository<TokenContaJpaEntity, UUID> {
    Optional<TokenContaJpaEntity> findByToken(String token);
    List<TokenContaJpaEntity> findByEmailAndFinalidadeOrderByCriadoEmDesc(String email, TokenContaFinalidade finalidade);
}
