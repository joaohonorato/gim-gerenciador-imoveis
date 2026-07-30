package br.com.imoveis.application.usecase;

import br.com.imoveis.application.ports.ImovelRepository;
import br.com.imoveis.domain.imovel.Imovel;
import jakarta.inject.Singleton;

import java.util.List;
import java.util.UUID;

@Singleton
public class ListarImoveisDoProprietario {

    private final ImovelRepository repository;

    public ListarImoveisDoProprietario(ImovelRepository repository) {
        this.repository = repository;
    }

    public List<Imovel> execute(UUID proprietarioId) {
        return repository.findByProprietario(proprietarioId);
    }
}
