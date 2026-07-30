package br.com.imoveis.application.usecase;

import br.com.imoveis.application.exception.NaoEncontradoException;
import br.com.imoveis.application.ports.ImovelRepository;
import br.com.imoveis.domain.imovel.Conta;
import br.com.imoveis.domain.imovel.Imovel;
import br.com.imoveis.domain.imovel.TipoConta;
import br.com.imoveis.domain.shared.Dinheiro;
import jakarta.inject.Singleton;

import java.time.LocalDate;
import java.util.UUID;

@Singleton
public class RegistrarConta {

    private final ImovelRepository repository;

    public RegistrarConta(ImovelRepository repository) {
        this.repository = repository;
    }

    public Conta execute(UUID imovelId, UUID proprietarioId, TipoConta tipo, LocalDate vencimento, Dinheiro valor) {
        Imovel imovel = repository.findById(imovelId)
            .filter(i -> i.proprietarioId().equals(proprietarioId))
            .orElseThrow(() -> new NaoEncontradoException("imóvel"));
        Conta conta = Conta.registrar(imovel.id(), tipo, vencimento, valor);
        return repository.saveConta(conta);
    }
}
