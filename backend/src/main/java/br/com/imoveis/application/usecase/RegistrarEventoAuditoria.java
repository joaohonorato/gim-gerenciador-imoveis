package br.com.imoveis.application.usecase;

import br.com.imoveis.application.ports.Clock;
import br.com.imoveis.application.ports.EventoAuditoriaRepository;
import br.com.imoveis.domain.auditoria.EntidadeAuditoria;
import br.com.imoveis.domain.auditoria.EventoAuditoria;
import br.com.imoveis.domain.auditoria.TipoEventoAuditoria;
import jakarta.inject.Singleton;

import java.util.UUID;

// Trilha de auditoria mínima dos fluxos de convite (rank 5 do backlog
// técnico) — chamado a partir de outros use cases nos pontos de transição
// de ciclo de vida do CONVITE/CONVITE_ACESSO, não é acionado diretamente
// por nenhuma rota própria.
@Singleton
public class RegistrarEventoAuditoria {

    private final EventoAuditoriaRepository repository;
    private final Clock clock;

    public RegistrarEventoAuditoria(EventoAuditoriaRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public void execute(EntidadeAuditoria entidadeTipo, UUID entidadeId, TipoEventoAuditoria tipoEvento, String detalhe) {
        repository.save(EventoAuditoria.registrar(entidadeTipo, entidadeId, tipoEvento, detalhe, clock.now()));
    }
}
