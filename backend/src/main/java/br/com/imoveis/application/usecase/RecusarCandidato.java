package br.com.imoveis.application.usecase;

import br.com.imoveis.application.exception.NaoEncontradoException;
import br.com.imoveis.application.ports.ConviteRepository;
import br.com.imoveis.domain.convite.Candidatura;
import br.com.imoveis.domain.convite.Convite;
import io.micronaut.transaction.annotation.Transactional;
import jakarta.inject.Singleton;

import java.util.UUID;

@Singleton
@Transactional
public class RecusarCandidato {

    private final ConviteRepository conviteRepository;

    public RecusarCandidato(ConviteRepository conviteRepository) {
        this.conviteRepository = conviteRepository;
    }

    public void execute(UUID candidaturaId, UUID proprietarioId) {
        Candidatura candidatura = conviteRepository.findCandidaturaById(candidaturaId)
            .orElseThrow(() -> new NaoEncontradoException("candidatura"));
        Convite convite = conviteRepository.findById(candidatura.conviteId())
            .orElseThrow(() -> new NaoEncontradoException("candidatura"));
        if (!convite.proprietarioId().equals(proprietarioId)) {
            throw new NaoEncontradoException("candidatura");
        }
        candidatura.recusar();
        conviteRepository.saveCandidatura(candidatura);
    }
}
