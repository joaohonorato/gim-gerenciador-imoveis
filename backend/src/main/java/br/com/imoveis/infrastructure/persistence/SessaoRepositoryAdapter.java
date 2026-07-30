package br.com.imoveis.infrastructure.persistence;

import br.com.imoveis.application.ports.SessaoRepository;
import br.com.imoveis.infrastructure.persistence.jpa.SessaoJpaEntity;
import br.com.imoveis.infrastructure.persistence.jpa.SessaoJpaRepository;
import io.micronaut.transaction.annotation.Transactional;
import jakarta.inject.Singleton;
import jakarta.persistence.EntityManager;

import java.util.Optional;
import java.util.UUID;

@Singleton
@Transactional
public class SessaoRepositoryAdapter implements SessaoRepository {

    private final SessaoJpaRepository jpa;
    private final EntityManager em;

    public SessaoRepositoryAdapter(SessaoJpaRepository jpa, EntityManager em) {
        this.jpa = jpa;
        this.em = em;
    }

    @Override
    public Sessao save(Sessao s) {
        SessaoJpaEntity e = new SessaoJpaEntity();
        e.setId(UUID.randomUUID());
        e.setToken(s.token());
        e.setContaAcessoId(s.contaAcessoId());
        e.setTipoConta(s.tipoConta());
        e.setProprietarioId(s.proprietarioId());
        e.setInquilinoId(s.inquilinoId());
        e.setExpiraEm(s.expiraEm());
        em.merge(e);
        return s;
    }

    @Override
    public Optional<Sessao> findByToken(String token) {
        return jpa.findByToken(token).map(e ->
            new Sessao(e.getToken(), e.getContaAcessoId(), e.getTipoConta(), e.getProprietarioId(),
                e.getInquilinoId(), e.getExpiraEm()));
    }

    @Override
    public void deleteByToken(String token) {
        jpa.deleteByToken(token);
    }
}
