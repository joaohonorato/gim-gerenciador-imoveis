package br.com.imoveis.application.usecase;

import br.com.imoveis.application.exception.ConflitoException;
import br.com.imoveis.application.ports.CategoriaChamadoRepository;
import br.com.imoveis.application.ports.Clock;
import br.com.imoveis.domain.chamado.CategoriaChamado;
import io.micronaut.transaction.annotation.Transactional;
import jakarta.inject.Singleton;

import java.util.UUID;

@Singleton
@Transactional
public class RegistrarCategoriaChamado {

    private final CategoriaChamadoRepository categoriaChamadoRepository;
    private final Clock clock;

    public RegistrarCategoriaChamado(CategoriaChamadoRepository categoriaChamadoRepository, Clock clock) {
        this.categoriaChamadoRepository = categoriaChamadoRepository;
        this.clock = clock;
    }

    public CategoriaChamado execute(UUID proprietarioId, String nome) {
        String nomeNormalizado = nome == null ? null : nome.trim();
        if (nomeNormalizado != null && categoriaChamadoRepository.findByProprietarioIdAndNome(proprietarioId, nomeNormalizado).isPresent()) {
            throw new ConflitoException("já existe uma categoria de chamado com esse nome");
        }
        CategoriaChamado categoria = CategoriaChamado.registrar(proprietarioId, nome, clock.now());
        return categoriaChamadoRepository.save(categoria);
    }
}
