package br.com.imoveis.application.usecase;

import br.com.imoveis.application.ports.Clock;
import br.com.imoveis.application.ports.ImovelRepository;
import br.com.imoveis.domain.imovel.Imovel;
import br.com.imoveis.domain.imovel.TipoImovel;
import jakarta.inject.Singleton;

import java.math.BigDecimal;
import java.util.UUID;

@Singleton
public class CadastrarImovel {

    private final ImovelRepository repository;
    private final Clock clock;

    public CadastrarImovel(ImovelRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public Imovel execute(UUID proprietarioId, String endereco, String cidade, String matricula,
                          String numero, String bairro, String complemento, TipoImovel tipoImovel,
                          Integer quartos, Integer banheiros, Integer vagas, BigDecimal areaM2,
                          BigDecimal iptu, String cep) {
        Imovel imovel = Imovel.cadastrar(proprietarioId, endereco, cidade, matricula,
            numero, bairro, complemento, tipoImovel, quartos, banheiros, vagas, areaM2, iptu, cep, clock.now());
        return repository.save(imovel);
    }
}
