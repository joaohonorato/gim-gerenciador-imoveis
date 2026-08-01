package br.com.imoveis.infrastructure.persistence;

import br.com.imoveis.application.ports.ArquivoRepository;
import br.com.imoveis.domain.arquivo.Arquivo;
import br.com.imoveis.domain.arquivo.TipoArquivo;
import br.com.imoveis.infrastructure.persistence.jpa.ArquivoJpaEntity;
import br.com.imoveis.infrastructure.persistence.jpa.ArquivoJpaRepository;
import io.micronaut.transaction.annotation.Transactional;
import jakarta.inject.Singleton;
import jakarta.persistence.EntityManager;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Singleton
@Transactional
public class ArquivoRepositoryAdapter implements ArquivoRepository {

    private final ArquivoJpaRepository jpa;
    private final EntityManager em;

    public ArquivoRepositoryAdapter(ArquivoJpaRepository jpa, EntityManager em) {
        this.jpa = jpa;
        this.em = em;
    }

    @Override
    public Arquivo save(Arquivo a) {
        ArquivoJpaEntity e = jpa.findById(a.id()).orElseGet(ArquivoJpaEntity::new);
        e.setId(a.id());
        e.setTipo(a.tipo());
        e.setDonoId(a.donoId());
        e.setBlobKey(a.blobKey());
        e.setNomeOriginal(a.nomeOriginal());
        e.setContentType(a.contentType());
        e.setTamanhoBytes(a.tamanhoBytes());
        e.setCriadoEm(a.criadoEm());
        em.merge(e);
        return a;
    }

    @Override
    public Optional<Arquivo> findById(UUID id) {
        return jpa.findById(id).map(this::toDomain);
    }

    @Override
    public List<Arquivo> findByDonoIdAndTipo(UUID donoId, TipoArquivo tipo) {
        return jpa.findByDonoIdAndTipo(donoId, tipo).stream().map(this::toDomain).toList();
    }

    @Override
    public void excluir(UUID id) {
        jpa.deleteById(id);
    }

    private Arquivo toDomain(ArquivoJpaEntity e) {
        return Arquivo.reconstituir(e.getId(), e.getTipo(), e.getDonoId(), e.getBlobKey(),
            e.getNomeOriginal(), e.getContentType(), e.getTamanhoBytes(), e.getCriadoEm());
    }
}
