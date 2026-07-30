package br.com.imoveis.application.usecase;

import br.com.imoveis.application.exception.NaoEncontradoException;
import br.com.imoveis.application.ports.ConviteRepository;
import br.com.imoveis.domain.contrato.GarantiaTipo;
import br.com.imoveis.domain.convite.Candidatura;
import br.com.imoveis.domain.convite.Convite;
import io.micronaut.transaction.annotation.Transactional;
import jakarta.inject.Singleton;

@Singleton
@Transactional
public class EnviarGarantiaDaCandidatura {

    private final ConviteRepository conviteRepository;

    public EnviarGarantiaDaCandidatura(ConviteRepository conviteRepository) {
        this.conviteRepository = conviteRepository;
    }

    public Candidatura execute(String token, GarantiaTipo tipo, String dadosEspecificos) {
        Convite convite = conviteRepository.findByToken(token)
            .orElseThrow(() -> new NaoEncontradoException("convite"));
        if (convite.candidaturaId() == null) {
            throw new IllegalStateException("candidatura ainda não iniciada para este convite");
        }
        Candidatura candidatura = conviteRepository.findCandidaturaById(convite.candidaturaId())
            .orElseThrow(() -> new NaoEncontradoException("candidatura"));
        candidatura.definirGarantia(tipo, dadosEspecificos);
        return conviteRepository.saveCandidatura(candidatura);
    }
}
