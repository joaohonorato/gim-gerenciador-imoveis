package br.com.imoveis.infrastructure.persistence.jpa;

import br.com.imoveis.domain.contrato.PagamentoStatus;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "pagamentos")
public class PagamentoJpaEntity {

    @Id
    private UUID id;

    @Column(name = "contrato_id", nullable = false)
    private UUID contratoId;

    @Column(nullable = false)
    private LocalDate vencimento;

    @Column(nullable = false)
    private BigDecimal valor;

    @Column(name = "pago_em")
    private LocalDate pagoEm;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PagamentoStatus status;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getContratoId() { return contratoId; }
    public void setContratoId(UUID contratoId) { this.contratoId = contratoId; }

    public LocalDate getVencimento() { return vencimento; }
    public void setVencimento(LocalDate vencimento) { this.vencimento = vencimento; }

    public BigDecimal getValor() { return valor; }
    public void setValor(BigDecimal valor) { this.valor = valor; }

    public LocalDate getPagoEm() { return pagoEm; }
    public void setPagoEm(LocalDate pagoEm) { this.pagoEm = pagoEm; }

    public PagamentoStatus getStatus() { return status; }
    public void setStatus(PagamentoStatus status) { this.status = status; }
}
