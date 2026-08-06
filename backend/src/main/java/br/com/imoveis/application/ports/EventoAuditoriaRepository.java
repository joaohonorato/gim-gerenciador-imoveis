package br.com.imoveis.application.ports;

import br.com.imoveis.domain.auditoria.EntidadeAuditoria;
import br.com.imoveis.domain.auditoria.EventoAuditoria;

import java.util.List;
import java.util.UUID;

public interface EventoAuditoriaRepository {
    EventoAuditoria save(EventoAuditoria evento);
    List<EventoAuditoria> findByEntidade(EntidadeAuditoria entidadeTipo, UUID entidadeId);
}
