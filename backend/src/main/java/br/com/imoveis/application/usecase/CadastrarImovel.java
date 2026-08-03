package br.com.imoveis.application.usecase;

import br.com.imoveis.application.ports.ImovelRepository;
import br.com.imoveis.domain.imovel.Imovel;
import br.com.imoveis.domain.imovel.TipoImovel;
import jakarta.inject.Singleton;

import java.math.BigDecimal;
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
                          String numero, String bairro, String complemento, TipoImovel tipoImovel,
                          Integer quartos, Integer banheiros, Integer vagas, BigDecimal areaM2,
                          BigDecimal iptu, String cep) {
        Imovel imovel = Imovel.cadastrar(proprietarioId, endereco, cidade, matricula,
            numero, bairro, complemento, tipoImovel, quartos, banheiros, vagas, areaM2, iptu, cep);
        return repository.save(imovel);
    }
}
