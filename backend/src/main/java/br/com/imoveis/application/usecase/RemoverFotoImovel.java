package br.com.imoveis.application.usecase;

import br.com.imoveis.application.exception.NaoEncontradoException;
import br.com.imoveis.application.ports.ArquivoRepository;
import br.com.imoveis.application.ports.ArquivoStorage;
import br.com.imoveis.domain.arquivo.Arquivo;
import br.com.imoveis.domain.arquivo.TipoArquivo;
import jakarta.inject.Singleton;

import java.util.UUID;

@Singleton
public class RemoverFotoImovel {

    private final ArquivoRepository arquivoRepository;
    private final ArquivoStorage storage;

    public RemoverFotoImovel(ArquivoRepository arquivoRepository, ArquivoStorage storage) {
        this.arquivoRepository = arquivoRepository;
        this.storage = storage;
    }

    public void execute(UUID imovelId, UUID fotoId) {
        Arquivo arquivo = arquivoRepository.findById(fotoId)
            .filter(a -> a.tipo() == TipoArquivo.FOTO_IMOVEL && a.donoId().equals(imovelId))
            .orElseThrow(() -> new NaoEncontradoException("foto"));
        storage.excluir(arquivo.tipo().container(), arquivo.blobKey());
        arquivoRepository.excluir(arquivo.id());
    }
}
