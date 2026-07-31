package br.com.imoveis.application.usecase;

import br.com.imoveis.application.exception.NaoEncontradoException;
import br.com.imoveis.application.ports.ContratoRepository;
import br.com.imoveis.application.ports.ConviteRepository;
import br.com.imoveis.application.ports.InquilinoRepository;
import br.com.imoveis.domain.inquilino.Inquilino;
import jakarta.inject.Singleton;

import java.util.UUID;

@Singleton
public class BuscarInquilinoDoProprietario {

    private final InquilinoRepository inquilinoRepository;
    private final ContratoRepository contratoRepository;
    private final ConviteRepository conviteRepository;

    public BuscarInquilinoDoProprietario(InquilinoRepository inquilinoRepository,
                                          ContratoRepository contratoRepository,
                                          ConviteRepository conviteRepository) {
        this.inquilinoRepository = inquilinoRepository;
        this.contratoRepository = contratoRepository;
        this.conviteRepository = conviteRepository;
    }

    public Inquilino execute(UUID inquilinoId, UUID proprietarioId) {
        boolean temRelacao = contratoRepository.findByInquilinoId(inquilinoId).stream()
                .anyMatch(c -> c.proprietarioId().equals(proprietarioId))
            || conviteRepository.findCandidaturasByProprietarioId(proprietarioId).stream()
                .anyMatch(c -> c.inquilinoId().equals(inquilinoId));

        if (!temRelacao) {
            throw new NaoEncontradoException("inquilino");
        }

        return inquilinoRepository.findById(inquilinoId)
            .orElseThrow(() -> new NaoEncontradoException("inquilino"));
    }
}
