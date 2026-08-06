package br.com.imoveis.application.usecase;

import br.com.imoveis.application.exception.NaoEncontradoException;
import br.com.imoveis.application.ports.ImovelRepository;
import br.com.imoveis.domain.imovel.Imovel;
import jakarta.inject.Singleton;

import java.util.List;
import java.util.UUID;

// Cadastro de unidade individual e em lote usam o mesmo caminho: "individual"
// é só uma lista de 1 nome. Ver Imovel.adicionarUnidades — cobre o caso de
// subdividir um imóvel (ex.: terreno com várias casas, cada uma com vários
// apartamentos) sem exigir uma migração de dados nas entidades já
// existentes, já que contrato/convite/conta já são referenciados por
// unidade_id, não por imovel_id.
@Singleton
public class AdicionarUnidades {

    private final ImovelRepository imovelRepository;

    public AdicionarUnidades(ImovelRepository imovelRepository) {
        this.imovelRepository = imovelRepository;
    }

    public Imovel execute(UUID imovelId, UUID proprietarioId, List<String> nomes) {
        Imovel imovel = imovelRepository.findById(imovelId)
            .filter(i -> i.proprietarioId().equals(proprietarioId))
            .orElseThrow(() -> new NaoEncontradoException("imóvel"));
        imovel.adicionarUnidades(nomes);
        return imovelRepository.save(imovel);
    }
}
