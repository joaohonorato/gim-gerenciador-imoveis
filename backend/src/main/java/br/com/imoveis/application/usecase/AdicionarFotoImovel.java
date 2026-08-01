package br.com.imoveis.application.usecase;

import br.com.imoveis.application.ports.ArquivoRepository;
import br.com.imoveis.application.ports.ArquivoStorage;
import br.com.imoveis.application.ports.Clock;
import br.com.imoveis.domain.arquivo.Arquivo;
import br.com.imoveis.domain.arquivo.TipoArquivo;
import jakarta.inject.Singleton;

import java.io.InputStream;
import java.util.List;
import java.util.UUID;

@Singleton
public class AdicionarFotoImovel {

    private static final long TAMANHO_MAXIMO_BYTES = 8L * 1024 * 1024;
    private static final List<String> CONTENT_TYPES_PERMITIDOS = List.of("image/jpeg", "image/png", "image/webp");

    private final ArquivoRepository arquivoRepository;
    private final ArquivoStorage storage;
    private final Clock clock;

    public AdicionarFotoImovel(ArquivoRepository arquivoRepository, ArquivoStorage storage, Clock clock) {
        this.arquivoRepository = arquivoRepository;
        this.storage = storage;
        this.clock = clock;
    }

    public Arquivo execute(UUID imovelId, String nomeOriginal, String contentType, long tamanho, InputStream dados) {
        if (contentType == null || !CONTENT_TYPES_PERMITIDOS.contains(contentType)) {
            throw new IllegalArgumentException("tipo de arquivo não permitido para foto de imóvel: " + contentType);
        }
        if (tamanho > TAMANHO_MAXIMO_BYTES) {
            throw new IllegalArgumentException("imagem excede o tamanho máximo permitido (8MB)");
        }

        String blobKey = imovelId + "/" + UUID.randomUUID() + extensao(contentType);
        storage.upload(TipoArquivo.FOTO_IMOVEL.container(), blobKey, dados, tamanho, contentType);

        Arquivo arquivo = Arquivo.novo(TipoArquivo.FOTO_IMOVEL, imovelId, blobKey, nomeOriginal, contentType, tamanho, clock.now());
        return arquivoRepository.save(arquivo);
    }

    private String extensao(String contentType) {
        return switch (contentType) {
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            default -> ".jpg";
        };
    }
}
