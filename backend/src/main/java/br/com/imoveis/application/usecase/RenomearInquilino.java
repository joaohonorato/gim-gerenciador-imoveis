package br.com.imoveis.application.usecase;

import br.com.imoveis.application.exception.NaoEncontradoException;
import br.com.imoveis.application.ports.InquilinoRepository;
import br.com.imoveis.domain.inquilino.Inquilino;
import io.micronaut.transaction.annotation.Transactional;
import jakarta.inject.Singleton;

import java.util.UUID;

@Singleton
@Transactional
public class RenomearInquilino {

    private final InquilinoRepository inquilinoRepository;

    public RenomearInquilino(InquilinoRepository inquilinoRepository) {
        this.inquilinoRepository = inquilinoRepository;
    }

    public Inquilino execute(UUID inquilinoId, String novoNome) {
        Inquilino inquilino = inquilinoRepository.findById(inquilinoId)
            .orElseThrow(() -> new NaoEncontradoException("inquilino"));

        inquilino.renomear(novoNome);
        return inquilinoRepository.save(inquilino);
    }
}
