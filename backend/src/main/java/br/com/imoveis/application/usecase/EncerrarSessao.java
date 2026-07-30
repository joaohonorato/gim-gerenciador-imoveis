package br.com.imoveis.application.usecase;

import br.com.imoveis.application.ports.SessaoRepository;
import io.micronaut.transaction.annotation.Transactional;
import jakarta.inject.Singleton;

@Singleton
@Transactional
public class EncerrarSessao {

    private final SessaoRepository sessaoRepository;

    public EncerrarSessao(SessaoRepository sessaoRepository) {
        this.sessaoRepository = sessaoRepository;
    }

    public void execute(String token) {
        sessaoRepository.deleteByToken(token);
    }
}
