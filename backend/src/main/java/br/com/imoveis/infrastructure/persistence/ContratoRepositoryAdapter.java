package br.com.imoveis.infrastructure.persistence;

import br.com.imoveis.application.ports.ContratoRepository;
import br.com.imoveis.domain.contrato.Contrato;
import br.com.imoveis.domain.contrato.ContratoAssinaturaStatus;
import br.com.imoveis.domain.contrato.Garantia;
import br.com.imoveis.domain.contrato.Pagamento;
import br.com.imoveis.domain.contrato.ParteContrato;
import br.com.imoveis.domain.shared.Dinheiro;
import br.com.imoveis.domain.shared.Periodo;
import br.com.imoveis.infrastructure.persistence.jpa.ContratoJpaEntity;
import br.com.imoveis.infrastructure.persistence.jpa.ContratoJpaRepository;
import br.com.imoveis.infrastructure.persistence.jpa.PagamentoJpaEntity;
import br.com.imoveis.infrastructure.persistence.jpa.PagamentoJpaRepository;
import io.micronaut.transaction.annotation.Transactional;
import jakarta.inject.Singleton;
import jakarta.persistence.EntityManager;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Singleton
@Transactional
public class ContratoRepositoryAdapter implements ContratoRepository {

    private final ContratoJpaRepository contratoJpa;
    private final PagamentoJpaRepository pagamentoJpa;
    private final EntityManager em;

    public ContratoRepositoryAdapter(ContratoJpaRepository contratoJpa, PagamentoJpaRepository pagamentoJpa, EntityManager em) {
        this.contratoJpa = contratoJpa;
        this.pagamentoJpa = pagamentoJpa;
        this.em = em;
    }

    @Override
    public Contrato save(Contrato c) {
        ContratoJpaEntity e = contratoJpa.findById(c.id()).orElseGet(ContratoJpaEntity::new);
        e.setId(c.id());
        e.setUnidadeId(c.unidadeId());
        e.setInquilinoId(c.inquilinoId());
        e.setProprietarioId(c.proprietarioId());
        e.setDataInicio(c.periodo().inicio());
        e.setDataFim(c.periodo().fim());
        e.setTipo(c.tipo());
        e.setValorAluguel(c.valorAluguel().valor());
        e.setIndiceReajuste(c.indiceReajuste());
        e.setStatusAssinatura(c.statusAssinatura());
        e.setAssinouProprietario(c.assinaturas().contains(ParteContrato.PROPRIETARIO));
        e.setAssinouInquilino(c.assinaturas().contains(ParteContrato.INQUILINO));
        if (c.garantia() != null) {
            e.setGarantiaId(c.garantia().id());
            e.setGarantiaTipo(c.garantia().tipo());
            e.setGarantiaVencimento(c.garantia().vencimento());
            e.setGarantiaDados(c.garantia().dadosEspecificos());
        }
        em.merge(e);
        return c;
    }

    @Override
    public Optional<Contrato> findById(UUID id) {
        return contratoJpa.findById(id).map(this::toDomain);
    }

    @Override
    public List<Contrato> findByInquilinoId(UUID inquilinoId) {
        return contratoJpa.findByInquilinoId(inquilinoId).stream()
            .map(this::toDomain)
            .toList();
    }

    @Override
    public List<Contrato> findByProprietarioId(UUID proprietarioId) {
        return contratoJpa.findByProprietarioId(proprietarioId).stream()
            .map(this::toDomain)
            .toList();
    }

    @Override
    public List<Contrato> findByUnidadeAndStatus(UUID unidadeId, ContratoAssinaturaStatus status) {
        return contratoJpa.findByUnidadeIdAndStatusAssinatura(unidadeId, status).stream()
            .map(this::toDomain).toList();
    }

    @Override
    public Optional<Contrato> findByUnidadeInquilinoEPeriodo(UUID unidadeId, UUID inquilinoId,
                                                              java.time.LocalDate dataInicio,
                                                              java.time.LocalDate dataFim) {
        return contratoJpa.findByUnidadeIdAndInquilinoIdAndDataInicioAndDataFim(unidadeId, inquilinoId, dataInicio, dataFim)
            .map(this::toDomain);
    }

    @Override
    public Pagamento savePagamento(Pagamento p) {
        PagamentoJpaEntity e = pagamentoJpa.findById(p.id()).orElseGet(PagamentoJpaEntity::new);
        e.setId(p.id());
        e.setContratoId(p.contratoId());
        e.setVencimento(p.vencimento());
        e.setValor(p.valor().valor());
        e.setPagoEm(p.pagoEm());
        e.setStatus(p.status());
        em.merge(e);
        return p;
    }

    @Override
    public Optional<Pagamento> findPagamentoById(UUID id) {
        return pagamentoJpa.findById(id).map(this::toDomainPagamento);
    }

    @Override
    public List<Pagamento> findPagamentosByContrato(UUID contratoId) {
        return pagamentoJpa.findByContratoId(contratoId).stream().map(this::toDomainPagamento).toList();
    }

    private Contrato toDomain(ContratoJpaEntity e) {
        EnumSet<ParteContrato> assinaturas = EnumSet.noneOf(ParteContrato.class);
        if (e.isAssinouProprietario()) assinaturas.add(ParteContrato.PROPRIETARIO);
        if (e.isAssinouInquilino()) assinaturas.add(ParteContrato.INQUILINO);
        List<Pagamento> pagamentos = pagamentoJpa.findByContratoId(e.getId()).stream()
            .map(this::toDomainPagamento).toList();
        Garantia garantia = e.getGarantiaTipo() == null ? null : Garantia.reconstituir(
            e.getGarantiaId(), e.getId(), e.getGarantiaTipo(),
            e.getGarantiaVencimento(), e.getGarantiaDados());
        return Contrato.reconstituir(e.getId(), e.getUnidadeId(), e.getInquilinoId(), e.getProprietarioId(),
            new Periodo(e.getDataInicio(), e.getDataFim()), e.getTipo(),
            new Dinheiro(e.getValorAluguel()), e.getIndiceReajuste(),
            e.getStatusAssinatura(), assinaturas, garantia, pagamentos);
    }

    private Pagamento toDomainPagamento(PagamentoJpaEntity e) {
        return new Pagamento(e.getId(), e.getContratoId(), e.getVencimento(), new Dinheiro(e.getValor()),
            e.getPagoEm(), e.getStatus());
    }
}
