package br.com.imoveis.infrastructure.persistence.jpa;

import br.com.imoveis.domain.contrato.ParteContrato;
import br.com.imoveis.domain.imovel.ContaStatus;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "contas")
public class ContaJpaEntity {

    @Id
    private UUID id;

    @Column(name = "unidade_id", nullable = false)
    private UUID unidadeId;

    @Column(name = "tipo_conta_id", nullable = false)
    private UUID tipoContaId;

    @Column(nullable = false)
    private LocalDate vencimento;

    @Column(nullable = false)
    private BigDecimal valor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ContaStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ParteContrato responsavel;

    @Column(name = "contrato_id")
    private UUID contratoId;

    @Column(name = "alerta_enviado", nullable = false)
    private boolean alertaEnviado;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getUnidadeId() { return unidadeId; }
    public void setUnidadeId(UUID unidadeId) { this.unidadeId = unidadeId; }

    public UUID getTipoContaId() { return tipoContaId; }
    public void setTipoContaId(UUID tipoContaId) { this.tipoContaId = tipoContaId; }

    public LocalDate getVencimento() { return vencimento; }
    public void setVencimento(LocalDate vencimento) { this.vencimento = vencimento; }

    public BigDecimal getValor() { return valor; }
    public void setValor(BigDecimal valor) { this.valor = valor; }

    public ContaStatus getStatus() { return status; }
    public void setStatus(ContaStatus status) { this.status = status; }

    public ParteContrato getResponsavel() { return responsavel; }
    public void setResponsavel(ParteContrato responsavel) { this.responsavel = responsavel; }

    public UUID getContratoId() { return contratoId; }
    public void setContratoId(UUID contratoId) { this.contratoId = contratoId; }

    public boolean isAlertaEnviado() { return alertaEnviado; }
    public void setAlertaEnviado(boolean alertaEnviado) { this.alertaEnviado = alertaEnviado; }
}
