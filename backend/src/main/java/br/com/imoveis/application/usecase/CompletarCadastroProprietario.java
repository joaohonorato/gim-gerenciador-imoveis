package br.com.imoveis.application.usecase;

import br.com.imoveis.application.exception.ConflitoException;
import br.com.imoveis.application.exception.NaoEncontradoException;
import br.com.imoveis.application.ports.ProprietarioRepository;
import br.com.imoveis.domain.proprietario.Proprietario;
import br.com.imoveis.domain.shared.CpfCnpj;
import io.micronaut.transaction.annotation.Transactional;
import jakarta.inject.Singleton;

import java.util.UUID;

@Singleton
@Transactional
public class CompletarCadastroProprietario {

    private final ProprietarioRepository proprietarioRepository;

    public CompletarCadastroProprietario(ProprietarioRepository proprietarioRepository) {
        this.proprietarioRepository = proprietarioRepository;
    }

    public Proprietario execute(UUID proprietarioId, String cpfCnpj) {
        Proprietario proprietario = proprietarioRepository.findById(proprietarioId)
            .orElseThrow(() -> new NaoEncontradoException("proprietário"));

        CpfCnpj novoCpfCnpj = CpfCnpj.parse(cpfCnpj);
        proprietarioRepository.findByCpfCnpj(novoCpfCnpj)
            .filter(outro -> !outro.id().equals(proprietarioId))
            .ifPresent(outro -> {
                throw new ConflitoException("já existe proprietário cadastrado para este cpf/cnpj");
            });

        proprietario.completarCadastro(novoCpfCnpj);
        return proprietarioRepository.save(proprietario);
    }
}
