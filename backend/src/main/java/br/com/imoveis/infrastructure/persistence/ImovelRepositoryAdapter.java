package br.com.imoveis.infrastructure.persistence;

import br.com.imoveis.application.ports.ImovelRepository;
import br.com.imoveis.domain.imovel.Conta;
import br.com.imoveis.domain.imovel.Imovel;
import br.com.imoveis.domain.imovel.Unidade;
import br.com.imoveis.domain.shared.Dinheiro;
import br.com.imoveis.infrastructure.persistence.jpa.*;
import io.micronaut.transaction.annotation.Transactional;
import jakarta.inject.Singleton;
import jakarta.persistence.EntityManager;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Singleton
@Transactional
public class ImovelRepositoryAdapter implements ImovelRepository {

    private final ImovelJpaRepository imovelJpa;
    private final UnidadeJpaRepository unidadeJpa;
    private final ContaJpaRepository contaJpa;
    private final EntityManager em;

    public ImovelRepositoryAdapter(ImovelJpaRepository imovelJpa,
                                    UnidadeJpaRepository unidadeJpa,
                                    ContaJpaRepository contaJpa,
                                    EntityManager em) {
        this.imovelJpa = imovelJpa;
        this.unidadeJpa = unidadeJpa;
        this.contaJpa = contaJpa;
        this.em = em;
    }

    @Override
    public Imovel save(Imovel imovel) {
        ImovelJpaEntity e = imovelJpa.findById(imovel.id()).orElseGet(ImovelJpaEntity::new);
        e.setId(imovel.id());
        e.setProprietarioId(imovel.proprietarioId());
        e.setEndereco(imovel.endereco());
        e.setCidade(imovel.cidade());
        e.setMatricula(imovel.matricula());
        e.setNumero(imovel.numero());
        e.setBairro(imovel.bairro());
        e.setComplemento(imovel.complemento());
        e.setTipoImovel(imovel.tipoImovel());
        e.setVisibilidade(imovel.visibilidade());
        em.merge(e);
        for (Unidade u : imovel.unidades()) {
            saveUnidade(u);
        }
        return imovel;
    }

    @Override
    public Optional<Imovel> findById(UUID id) {
        return imovelJpa.findById(id).map(this::toDomain);
    }

    @Override
    public List<Imovel> findByProprietario(UUID proprietarioId) {
        List<ImovelJpaEntity> entities = imovelJpa.findByProprietarioId(proprietarioId);
        if (entities.isEmpty()) return List.of();
        Set<UUID> ids = entities.stream().map(ImovelJpaEntity::getId).collect(Collectors.toSet());
        Map<UUID, List<UnidadeJpaEntity>> unidadesByImovel = unidadeJpa.findByImovelIdIn(ids)
            .stream().collect(Collectors.groupingBy(UnidadeJpaEntity::getImovelId));
        return entities.stream()
            .map(e -> toDomainWith(e, unidadesByImovel.getOrDefault(e.getId(), List.of())))
            .toList();
    }

    @Override
    public Unidade saveUnidade(Unidade u) {
        UnidadeJpaEntity ue = unidadeJpa.findById(u.id()).orElseGet(UnidadeJpaEntity::new);
        ue.setId(u.id());
        ue.setImovelId(u.imovelId());
        ue.setNome(u.nome());
        ue.setPadrao(u.padrao());
        ue.setStatus(u.status());
        em.merge(ue);
        return u;
    }

    @Override
    public Optional<Unidade> findUnidadeById(UUID id) {
        return unidadeJpa.findById(id).map(this::toDomainUnidade);
    }

    @Override
    public Conta saveConta(Conta c) {
        ContaJpaEntity ce = contaJpa.findById(c.id()).orElseGet(ContaJpaEntity::new);
        ce.setId(c.id());
        ce.setImovelId(c.imovelId());
        ce.setTipo(c.tipo());
        ce.setVencimento(c.vencimento());
        ce.setValor(c.valor().valor());
        ce.setStatus(c.status());
        em.merge(ce);
        return c;
    }

    @Override
    public Optional<Conta> findContaById(UUID id) {
        return contaJpa.findById(id).map(this::toDomainConta);
    }

    @Override
    public List<Conta> findContasByImovel(UUID imovelId) {
        return contaJpa.findByImovelId(imovelId).stream().map(this::toDomainConta).toList();
    }

    private Imovel toDomain(ImovelJpaEntity e) {
        List<Unidade> unidades = unidadeJpa.findByImovelId(e.getId()).stream().map(this::toDomainUnidade).toList();
        return Imovel.reconstituir(e.getId(), e.getProprietarioId(), e.getEndereco(), e.getCidade(),
            e.getMatricula(), e.getNumero(), e.getBairro(), e.getComplemento(), e.getTipoImovel(),
            e.getVisibilidade(), unidades);
    }

    private Imovel toDomainWith(ImovelJpaEntity e, List<UnidadeJpaEntity> unidades) {
        return Imovel.reconstituir(e.getId(), e.getProprietarioId(), e.getEndereco(), e.getCidade(),
            e.getMatricula(), e.getNumero(), e.getBairro(), e.getComplemento(), e.getTipoImovel(),
            e.getVisibilidade(), unidades.stream().map(this::toDomainUnidade).toList());
    }

    private Unidade toDomainUnidade(UnidadeJpaEntity ue) {
        return Unidade.reconstituir(ue.getId(), ue.getImovelId(), ue.getNome(), ue.isPadrao(), ue.getStatus());
    }

    private Conta toDomainConta(ContaJpaEntity ce) {
        return Conta.reconstituir(ce.getId(), ce.getImovelId(), ce.getTipo(),
            ce.getVencimento(), new Dinheiro(ce.getValor()), ce.getStatus());
    }
}
