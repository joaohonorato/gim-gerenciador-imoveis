package br.com.imoveis.application.usecase;

import br.com.imoveis.application.ports.ConviteRepository;
import br.com.imoveis.domain.convite.Candidatura;
import br.com.imoveis.domain.convite.CandidaturaStatus;
import jakarta.inject.Singleton;

import java.util.List;
import java.util.UUID;

@Singleton
public class ListarCandidaturasPendentes {

    private final ConviteRepository conviteRepository;

    public ListarCandidaturasPendentes(ConviteRepository conviteRepository) {
        this.conviteRepository = conviteRepository;
    }

    public List<Candidatura> execute(UUID proprietarioId) {
        return conviteRepository.findCandidaturasByProprietarioId(proprietarioId).stream()
            .filter(c -> c.status() == CandidaturaStatus.PENDENTE)
            .toList();
    }
}
