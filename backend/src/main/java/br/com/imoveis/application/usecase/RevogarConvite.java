package br.com.imoveis.application.usecase;

import br.com.imoveis.application.exception.NaoEncontradoException;
import br.com.imoveis.application.ports.ConviteRepository;
import br.com.imoveis.domain.convite.Convite;
import io.micronaut.transaction.annotation.Transactional;
import jakarta.inject.Singleton;

@Singleton
@Transactional
public class RevogarConvite {

    private final ConviteRepository conviteRepository;

    public RevogarConvite(ConviteRepository conviteRepository) {
        this.conviteRepository = conviteRepository;
    }

    public Convite execute(String token, java.util.UUID proprietarioId) {
        Convite convite = conviteRepository.findByToken(token)
            .orElseThrow(() -> new NaoEncontradoException("convite"));
        if (!convite.proprietarioId().equals(proprietarioId)) {
            throw new NaoEncontradoException("convite");
        }
        convite.revogar();
        return conviteRepository.save(convite);
    }
}
