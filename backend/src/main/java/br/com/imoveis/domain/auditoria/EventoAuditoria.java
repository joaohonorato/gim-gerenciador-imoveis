package br.com.imoveis.domain.auditoria;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class EventoAuditoria {

    private final UUID id;
    private final EntidadeAuditoria entidadeTipo;
    private final UUID entidadeId;
    private final TipoEventoAuditoria tipoEvento;
    private final String detalhe;
    private final Instant criadoEm;

    private EventoAuditoria(UUID id, EntidadeAuditoria entidadeTipo, UUID entidadeId,
                            TipoEventoAuditoria tipoEvento, String detalhe, Instant criadoEm) {
        this.id = id;
        this.entidadeTipo = Objects.requireNonNull(entidadeTipo, "entidadeTipo obrigatório");
        this.entidadeId = Objects.requireNonNull(entidadeId, "entidadeId obrigatório");
        this.tipoEvento = Objects.requireNonNull(tipoEvento, "tipoEvento obrigatório");
        this.detalhe = detalhe;
        this.criadoEm = Objects.requireNonNull(criadoEm, "criadoEm obrigatório");
    }

    public static EventoAuditoria registrar(EntidadeAuditoria entidadeTipo, UUID entidadeId,
                                             TipoEventoAuditoria tipoEvento, String detalhe, Instant agora) {
        return new EventoAuditoria(UUID.randomUUID(), entidadeTipo, entidadeId, tipoEvento, detalhe, agora);
    }

    public static EventoAuditoria reconstituir(UUID id, EntidadeAuditoria entidadeTipo, UUID entidadeId,
                                                TipoEventoAuditoria tipoEvento, String detalhe, Instant criadoEm) {
        return new EventoAuditoria(id, entidadeTipo, entidadeId, tipoEvento, detalhe, criadoEm);
    }

    public UUID id() { return id; }
    public EntidadeAuditoria entidadeTipo() { return entidadeTipo; }
    public UUID entidadeId() { return entidadeId; }
    public TipoEventoAuditoria tipoEvento() { return tipoEvento; }
    public String detalhe() { return detalhe; }
    public Instant criadoEm() { return criadoEm; }
}
