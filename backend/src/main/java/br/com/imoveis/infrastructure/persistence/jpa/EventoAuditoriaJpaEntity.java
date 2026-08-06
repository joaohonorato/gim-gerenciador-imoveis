package br.com.imoveis.infrastructure.persistence.jpa;

import br.com.imoveis.domain.auditoria.EntidadeAuditoria;
import br.com.imoveis.domain.auditoria.TipoEventoAuditoria;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "eventos_auditoria")
public class EventoAuditoriaJpaEntity {

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "entidade_tipo", nullable = false)
    private EntidadeAuditoria entidadeTipo;

    @Column(name = "entidade_id", nullable = false)
    private UUID entidadeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_evento", nullable = false)
    private TipoEventoAuditoria tipoEvento;

    @Column(columnDefinition = "TEXT")
    private String detalhe;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public EntidadeAuditoria getEntidadeTipo() { return entidadeTipo; }
    public void setEntidadeTipo(EntidadeAuditoria entidadeTipo) { this.entidadeTipo = entidadeTipo; }

    public UUID getEntidadeId() { return entidadeId; }
    public void setEntidadeId(UUID entidadeId) { this.entidadeId = entidadeId; }

    public TipoEventoAuditoria getTipoEvento() { return tipoEvento; }
    public void setTipoEvento(TipoEventoAuditoria tipoEvento) { this.tipoEvento = tipoEvento; }

    public String getDetalhe() { return detalhe; }
    public void setDetalhe(String detalhe) { this.detalhe = detalhe; }

    public Instant getCriadoEm() { return criadoEm; }
    public void setCriadoEm(Instant criadoEm) { this.criadoEm = criadoEm; }
}
