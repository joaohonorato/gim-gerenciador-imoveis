package br.com.imoveis.application.usecase;

import br.com.imoveis.application.exception.NaoEncontradoException;
import br.com.imoveis.application.ports.ProprietarioRepository;
import br.com.imoveis.domain.proprietario.Proprietario;
import io.micronaut.transaction.annotation.Transactional;
import jakarta.inject.Singleton;

import java.util.UUID;

// Mesmo formato de CompletarCadastroProprietario, sem a checagem de
// duplicidade (telefone não é único como cpf/cnpj).
@Singleton
@Transactional
public class AtualizarTelefoneProprietario {

    private final ProprietarioRepository proprietarioRepository;

    public AtualizarTelefoneProprietario(ProprietarioRepository proprietarioRepository) {
        this.proprietarioRepository = proprietarioRepository;
    }

    public Proprietario execute(UUID proprietarioId, String telefone) {
        Proprietario proprietario = proprietarioRepository.findById(proprietarioId)
            .orElseThrow(() -> new NaoEncontradoException("proprietário"));

        proprietario.definirTelefone(telefone);
        return proprietarioRepository.save(proprietario);
    }
}
