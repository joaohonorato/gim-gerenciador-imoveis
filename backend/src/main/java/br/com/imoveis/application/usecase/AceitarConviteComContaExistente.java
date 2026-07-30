package br.com.imoveis.application.usecase;

import br.com.imoveis.application.exception.ConflitoException;
import br.com.imoveis.application.exception.NaoEncontradoException;
import br.com.imoveis.application.ports.Clock;
import br.com.imoveis.application.ports.ConviteRepository;
import br.com.imoveis.domain.convite.Candidatura;
import br.com.imoveis.domain.convite.Convite;
import br.com.imoveis.domain.convite.ConviteStatus;
import io.micronaut.transaction.annotation.Transactional;
import jakarta.inject.Singleton;

import java.util.UUID;

@Singleton
@Transactional
public class AceitarConviteComContaExistente {

    private final ConviteRepository conviteRepository;
    private final Clock clock;

    public AceitarConviteComContaExistente(ConviteRepository conviteRepository, Clock clock) {
        this.conviteRepository = conviteRepository;
        this.clock = clock;
    }

    public Result execute(String token, UUID inquilinoId) {
        Convite convite = conviteRepository.findByToken(token)
            .orElseThrow(() -> new NaoEncontradoException("convite"));

        if (convite.expirado(clock.now())) {
            throw new ConflitoException("convite expirado");
        }

        if (convite.status() != ConviteStatus.PENDENTE) {
            throw new ConflitoException("convite já foi iniciado");
        }

        Candidatura candidatura = Candidatura.nova(convite.id(), inquilinoId, clock.now());
        candidatura = conviteRepository.saveCandidatura(candidatura);

        convite.marcarCandidaturaCriada(candidatura.id());
        conviteRepository.save(convite);

        return new Result(convite, candidatura);
    }

    public record Result(Convite convite, Candidatura candidatura) {}
}
