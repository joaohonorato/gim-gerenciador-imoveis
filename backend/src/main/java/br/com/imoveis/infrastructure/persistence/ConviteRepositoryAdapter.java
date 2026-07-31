package br.com.imoveis.infrastructure.persistence;

import br.com.imoveis.application.ports.ConviteRepository;
import br.com.imoveis.domain.convite.Candidatura;
import br.com.imoveis.domain.convite.Convite;
import br.com.imoveis.domain.shared.Dinheiro;
import br.com.imoveis.domain.shared.Periodo;
import br.com.imoveis.infrastructure.persistence.jpa.CandidaturaJpaEntity;
import br.com.imoveis.infrastructure.persistence.jpa.CandidaturaJpaRepository;
import br.com.imoveis.infrastructure.persistence.jpa.ConviteJpaEntity;
import br.com.imoveis.infrastructure.persistence.jpa.ConviteJpaRepository;
import io.micronaut.transaction.annotation.Transactional;
import jakarta.inject.Singleton;
import jakarta.persistence.EntityManager;

import java.util.Optional;
import java.util.UUID;

@Singleton
@Transactional
public class ConviteRepositoryAdapter implements ConviteRepository {

    private final ConviteJpaRepository conviteJpa;
    private final CandidaturaJpaRepository candidaturaJpa;
    private final EntityManager em;

    public ConviteRepositoryAdapter(ConviteJpaRepository conviteJpa, CandidaturaJpaRepository candidaturaJpa, EntityManager em) {
        this.conviteJpa = conviteJpa;
        this.candidaturaJpa = candidaturaJpa;
        this.em = em;
    }

    @Override
    public Convite save(Convite c) {
        ConviteJpaEntity e = conviteJpa.findById(c.id()).orElseGet(ConviteJpaEntity::new);
        e.setId(c.id());
        e.setImovelId(c.imovelId());
        e.setUnidadeId(c.unidadeId());
        e.setProprietarioId(c.proprietarioId());
        e.setToken(c.token());
        e.setExpiraEm(c.expiraEm());
        e.setTipoContrato(c.condicoes().tipoContrato());
        e.setValorAluguel(c.condicoes().valorAluguel().valor());
        e.setPeriodoInicio(c.condicoes().periodoSugerido().inicio());
        e.setPeriodoFim(c.condicoes().periodoSugerido().fim());
        e.setGarantiaAceita(c.condicoes().garantiaAceita());
        e.setStatus(c.status());
        e.setCandidaturaId(c.candidaturaId());
        e.setUltimoCanalEnvio(c.ultimoCanalEnvio());
        e.setUltimoStatusEnvio(c.ultimoStatusEnvio());
        e.setUltimoDestinoEnvio(c.ultimoDestinoEnvio());
        e.setUltimoWhatsappShareUrl(c.ultimoWhatsappShareUrl());
        e.setUltimoDetalheEnvio(c.ultimoDetalheEnvio());
        e.setUltimoEnvioEm(c.ultimoEnvioEm());
        e.setTentativasEnvio(c.tentativasEnvio());
        em.merge(e);
        return c;
    }

    @Override public Optional<Convite> findByToken(String token) { return conviteJpa.findByToken(token).map(this::toDomain); }
    @Override public Optional<Convite> findById(UUID id) { return conviteJpa.findById(id).map(this::toDomain); }
    @Override public java.util.List<Convite> findByImovelId(UUID imovelId) {
        return conviteJpa.findByImovelId(imovelId).stream().map(this::toDomain).toList();
    }

    @Override
    public Candidatura saveCandidatura(Candidatura c) {
        CandidaturaJpaEntity e = candidaturaJpa.findById(c.id()).orElseGet(CandidaturaJpaEntity::new);
        e.setId(c.id());
        e.setConviteId(c.conviteId());
        e.setInquilinoId(c.inquilinoId());
        e.setCriadaEm(c.criadaEm());
        e.setStatus(c.status());
        e.setGarantiaEscolhida(c.garantiaEscolhida());
        e.setDadosGarantia(c.dadosGarantia());
        em.merge(e);
        return c;
    }

    @Override
    public Optional<Candidatura> findCandidaturaById(UUID id) {
        return candidaturaJpa.findById(id).map(this::toDomainCandidatura);
    }

    @Override
    public java.util.List<Candidatura> findCandidaturasByInquilinoId(UUID inquilinoId) {
        return candidaturaJpa.findByInquilinoId(inquilinoId).stream()
            .map(this::toDomainCandidatura)
            .toList();
    }

    @Override
    public java.util.List<Candidatura> findCandidaturasByProprietarioId(UUID proprietarioId) {
        java.util.List<UUID> conviteIds = conviteJpa.findByProprietarioId(proprietarioId).stream()
            .map(ConviteJpaEntity::getId)
            .toList();
        if (conviteIds.isEmpty()) return java.util.List.of();
        return candidaturaJpa.findByConviteIdIn(conviteIds).stream()
            .map(this::toDomainCandidatura)
            .toList();
    }

    private Convite toDomain(ConviteJpaEntity e) {
        Convite.CondicoesConvite condicoes = new Convite.CondicoesConvite(
            e.getTipoContrato(),
            new Dinheiro(e.getValorAluguel()),
            new Periodo(e.getPeriodoInicio(), e.getPeriodoFim()),
            e.getGarantiaAceita());
        return Convite.reconstituir(e.getId(), e.getImovelId(), e.getUnidadeId(), e.getProprietarioId(), e.getToken(),
            e.getExpiraEm(), condicoes, e.getStatus(), e.getCandidaturaId(),
            e.getUltimoCanalEnvio(), e.getUltimoStatusEnvio(), e.getUltimoDestinoEnvio(),
            e.getUltimoWhatsappShareUrl(), e.getUltimoDetalheEnvio(), e.getUltimoEnvioEm(),
            e.getTentativasEnvio());
    }

    private Candidatura toDomainCandidatura(CandidaturaJpaEntity e) {
        return Candidatura.reconstituir(e.getId(), e.getConviteId(), e.getInquilinoId(), e.getCriadaEm(),
            e.getStatus(), e.getGarantiaEscolhida(), e.getDadosGarantia());
    }
}
