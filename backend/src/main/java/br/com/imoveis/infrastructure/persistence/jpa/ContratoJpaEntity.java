package br.com.imoveis.infrastructure.persistence.jpa;

import br.com.imoveis.domain.contrato.ContratoAssinaturaStatus;
import br.com.imoveis.domain.contrato.GarantiaTipo;
import br.com.imoveis.domain.contrato.TipoContrato;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "contratos")
public class ContratoJpaEntity {

    @Id
    private UUID id;

    @Column(name = "unidade_id", nullable = false)
    private UUID unidadeId;

    @Column(name = "inquilino_id", nullable = false)
    private UUID inquilinoId;

    @Column(name = "proprietario_id", nullable = false)
    private UUID proprietarioId;

    @Column(name = "convite_id")
    private UUID conviteId;

    @Column(name = "data_inicio", nullable = false)
    private LocalDate dataInicio;

    @Column(name = "data_fim", nullable = false)
    private LocalDate dataFim;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoContrato tipo;

    @Column(name = "valor_aluguel", nullable = false)
    private BigDecimal valorAluguel;

    @Column(name = "indice_reajuste", nullable = false)
    private String indiceReajuste;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_assinatura", nullable = false)
    private ContratoAssinaturaStatus statusAssinatura;

    @Column(name = "assinou_proprietario", nullable = false)
    private boolean assinouProprietario;

    @Column(name = "assinou_inquilino", nullable = false)
    private boolean assinouInquilino;

    @Column(name = "garantia_id")
    private UUID garantiaId;

    @Enumerated(EnumType.STRING)
    @Column(name = "garantia_tipo")
    private GarantiaTipo garantiaTipo;

    @Column(name = "garantia_vencimento")
    private LocalDate garantiaVencimento;

    @Column(name = "garantia_dados", columnDefinition = "TEXT")
    private String garantiaDados;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getUnidadeId() { return unidadeId; }
    public void setUnidadeId(UUID unidadeId) { this.unidadeId = unidadeId; }

    public UUID getInquilinoId() { return inquilinoId; }
    public void setInquilinoId(UUID inquilinoId) { this.inquilinoId = inquilinoId; }

    public UUID getProprietarioId() { return proprietarioId; }
    public void setProprietarioId(UUID proprietarioId) { this.proprietarioId = proprietarioId; }

    public UUID getConviteId() { return conviteId; }
    public void setConviteId(UUID conviteId) { this.conviteId = conviteId; }

    public LocalDate getDataInicio() { return dataInicio; }
    public void setDataInicio(LocalDate dataInicio) { this.dataInicio = dataInicio; }

    public LocalDate getDataFim() { return dataFim; }
    public void setDataFim(LocalDate dataFim) { this.dataFim = dataFim; }

    public TipoContrato getTipo() { return tipo; }
    public void setTipo(TipoContrato tipo) { this.tipo = tipo; }

    public BigDecimal getValorAluguel() { return valorAluguel; }
    public void setValorAluguel(BigDecimal valorAluguel) { this.valorAluguel = valorAluguel; }

    public String getIndiceReajuste() { return indiceReajuste; }
    public void setIndiceReajuste(String indiceReajuste) { this.indiceReajuste = indiceReajuste; }

    public ContratoAssinaturaStatus getStatusAssinatura() { return statusAssinatura; }
    public void setStatusAssinatura(ContratoAssinaturaStatus statusAssinatura) { this.statusAssinatura = statusAssinatura; }

    public boolean isAssinouProprietario() { return assinouProprietario; }
    public void setAssinouProprietario(boolean assinouProprietario) { this.assinouProprietario = assinouProprietario; }

    public boolean isAssinouInquilino() { return assinouInquilino; }
    public void setAssinouInquilino(boolean assinouInquilino) { this.assinouInquilino = assinouInquilino; }

    public UUID getGarantiaId() { return garantiaId; }
    public void setGarantiaId(UUID garantiaId) { this.garantiaId = garantiaId; }

    public GarantiaTipo getGarantiaTipo() { return garantiaTipo; }
    public void setGarantiaTipo(GarantiaTipo garantiaTipo) { this.garantiaTipo = garantiaTipo; }

    public LocalDate getGarantiaVencimento() { return garantiaVencimento; }
    public void setGarantiaVencimento(LocalDate garantiaVencimento) { this.garantiaVencimento = garantiaVencimento; }

    public String getGarantiaDados() { return garantiaDados; }
    public void setGarantiaDados(String garantiaDados) { this.garantiaDados = garantiaDados; }
}
