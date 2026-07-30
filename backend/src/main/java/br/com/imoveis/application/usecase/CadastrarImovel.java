package br.com.imoveis.application.usecase;

import br.com.imoveis.application.ports.ImovelRepository;
import br.com.imoveis.domain.imovel.Imovel;
import br.com.imoveis.domain.imovel.TipoImovel;
import jakarta.inject.Singleton;

import java.util.UUID;

@Singleton
public class CadastrarImovel {

    private final ImovelRepository repository;

    public CadastrarImovel(ImovelRepository repository) {
        this.repository = repository;
    }

    public Imovel execute(UUID proprietarioId, String endereco, String cidade, String matricula) {
        Imovel imovel = Imovel.cadastrar(proprietarioId, endereco, cidade, matricula);
        return repository.save(imovel);
    }

    public Imovel execute(UUID proprietarioId, String endereco, String cidade, String matricula,
                          String numero, String bairro, String complemento, TipoImovel tipoImovel) {
        Imovel imovel = Imovel.cadastrar(proprietarioId, endereco, cidade, matricula,
            numero, bairro, complemento, tipoImovel);
        return repository.save(imovel);
    }
}
