package br.com.imoveis.infrastructure.persistence.jpa;

import br.com.imoveis.domain.contrato.GarantiaTipo;
import br.com.imoveis.domain.convite.CandidaturaStatus;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "candidaturas")
public class CandidaturaJpaEntity {

    @Id
    private UUID id;

    @Column(name = "convite_id", nullable = false)
    private UUID conviteId;

    @Column(name = "inquilino_id", nullable = false)
    private UUID inquilinoId;

    @Column(name = "criada_em", nullable = false)
    private Instant criadaEm;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CandidaturaStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "garantia_escolhida")
    private GarantiaTipo garantiaEscolhida;

    @Column(name = "dados_garantia", columnDefinition = "TEXT")
    private String dadosGarantia;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getConviteId() { return conviteId; }
    public void setConviteId(UUID conviteId) { this.conviteId = conviteId; }

    public UUID getInquilinoId() { return inquilinoId; }
    public void setInquilinoId(UUID inquilinoId) { this.inquilinoId = inquilinoId; }

    public Instant getCriadaEm() { return criadaEm; }
    public void setCriadaEm(Instant criadaEm) { this.criadaEm = criadaEm; }

    public CandidaturaStatus getStatus() { return status; }
    public void setStatus(CandidaturaStatus status) { this.status = status; }

    public GarantiaTipo getGarantiaEscolhida() { return garantiaEscolhida; }
    public void setGarantiaEscolhida(GarantiaTipo garantiaEscolhida) { this.garantiaEscolhida = garantiaEscolhida; }

    public String getDadosGarantia() { return dadosGarantia; }
    public void setDadosGarantia(String dadosGarantia) { this.dadosGarantia = dadosGarantia; }
}
