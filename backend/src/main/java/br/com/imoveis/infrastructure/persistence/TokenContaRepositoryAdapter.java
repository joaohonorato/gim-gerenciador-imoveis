package br.com.imoveis.infrastructure.persistence;

import br.com.imoveis.application.ports.TokenContaRepository;
import br.com.imoveis.domain.auth.TokenConta;
import br.com.imoveis.domain.auth.TokenContaFinalidade;
import br.com.imoveis.domain.shared.Email;
import br.com.imoveis.infrastructure.persistence.jpa.TokenContaJpaEntity;
import br.com.imoveis.infrastructure.persistence.jpa.TokenContaJpaRepository;
import io.micronaut.transaction.annotation.Transactional;
import jakarta.inject.Singleton;
import jakarta.persistence.EntityManager;

import java.util.List;
import java.util.Optional;

@Singleton
@Transactional
public class TokenContaRepositoryAdapter implements TokenContaRepository {

    private final TokenContaJpaRepository jpa;
    private final EntityManager em;

    public TokenContaRepositoryAdapter(TokenContaJpaRepository jpa, EntityManager em) {
        this.jpa = jpa;
        this.em = em;
    }

    @Override
    public TokenConta save(TokenConta tokenConta) {
        TokenContaJpaEntity entity = jpa.findById(tokenConta.id()).orElseGet(TokenContaJpaEntity::new);
        entity.setId(tokenConta.id());
        entity.setToken(tokenConta.token());
        entity.setEmail(tokenConta.email().value());
        entity.setFinalidade(tokenConta.finalidade());
        entity.setExpiraEm(tokenConta.expiraEm());
        entity.setConsumido(tokenConta.consumido());
        entity.setCriadoEm(tokenConta.criadoEm());
        em.merge(entity);
        return tokenConta;
    }

    @Override
    public Optional<TokenConta> findByToken(String token) {
        return jpa.findByToken(token).map(this::toDomain);
    }

    @Override
    public Optional<TokenConta> findMostRecentByEmailAndFinalidade(Email email, TokenContaFinalidade finalidade) {
        List<TokenContaJpaEntity> found = jpa.findByEmailAndFinalidadeOrderByCriadoEmDesc(email.value(), finalidade);
        return found.isEmpty() ? Optional.empty() : Optional.of(toDomain(found.get(0)));
    }

    private TokenConta toDomain(TokenContaJpaEntity entity) {
        return TokenConta.reconstituir(
            entity.getId(),
            entity.getToken(),
            new Email(entity.getEmail()),
            entity.getFinalidade(),
            entity.getExpiraEm(),
            entity.getCriadoEm(),
            entity.isConsumido());
    }
}
