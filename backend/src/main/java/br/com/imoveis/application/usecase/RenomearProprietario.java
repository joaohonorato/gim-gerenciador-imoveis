package br.com.imoveis.application.usecase;

import br.com.imoveis.application.exception.NaoEncontradoException;
import br.com.imoveis.application.ports.ProprietarioRepository;
import br.com.imoveis.domain.proprietario.Proprietario;
import io.micronaut.transaction.annotation.Transactional;
import jakarta.inject.Singleton;

import java.util.UUID;

// Mesmo formato de AtualizarTelefoneProprietario — sem checagem de
// duplicidade (nome não é único).
@Singleton
@Transactional
public class RenomearProprietario {

    private final ProprietarioRepository proprietarioRepository;

    public RenomearProprietario(ProprietarioRepository proprietarioRepository) {
        this.proprietarioRepository = proprietarioRepository;
    }

    public Proprietario execute(UUID proprietarioId, String novoNome) {
        Proprietario proprietario = proprietarioRepository.findById(proprietarioId)
            .orElseThrow(() -> new NaoEncontradoException("proprietário"));

        proprietario.renomear(novoNome);
        return proprietarioRepository.save(proprietario);
    }
}
