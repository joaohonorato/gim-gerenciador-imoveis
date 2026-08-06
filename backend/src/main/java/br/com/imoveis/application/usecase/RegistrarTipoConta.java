package br.com.imoveis.application.usecase;

import br.com.imoveis.application.exception.ConflitoException;
import br.com.imoveis.application.ports.Clock;
import br.com.imoveis.application.ports.TipoContaRepository;
import br.com.imoveis.domain.imovel.TipoConta;
import io.micronaut.transaction.annotation.Transactional;
import jakarta.inject.Singleton;

import java.util.UUID;

@Singleton
@Transactional
public class RegistrarTipoConta {

    private final TipoContaRepository tipoContaRepository;
    private final Clock clock;

    public RegistrarTipoConta(TipoContaRepository tipoContaRepository, Clock clock) {
        this.tipoContaRepository = tipoContaRepository;
        this.clock = clock;
    }

    public TipoConta execute(UUID proprietarioId, String nome) {
        String nomeNormalizado = nome == null ? null : nome.trim();
        if (nomeNormalizado != null && tipoContaRepository.findByProprietarioIdAndNome(proprietarioId, nomeNormalizado).isPresent()) {
            throw new ConflitoException("já existe um tipo de conta com esse nome");
        }
        TipoConta tipoConta = TipoConta.registrar(proprietarioId, nome, clock.now());
        return tipoContaRepository.save(tipoConta);
    }
}
