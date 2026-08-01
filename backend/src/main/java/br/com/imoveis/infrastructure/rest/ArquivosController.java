package br.com.imoveis.infrastructure.rest;

import br.com.imoveis.application.exception.NaoEncontradoException;
import br.com.imoveis.application.ports.ArquivoRepository;
import br.com.imoveis.application.ports.ArquivoStorage;
import br.com.imoveis.application.ports.ContratoRepository;
import br.com.imoveis.domain.arquivo.Arquivo;
import br.com.imoveis.domain.arquivo.TipoArquivo;
import br.com.imoveis.domain.contrato.Contrato;
import br.com.imoveis.infrastructure.auth.CurrentPrincipal;
import br.com.imoveis.infrastructure.auth.Principal;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;

import java.time.Duration;
import java.util.UUID;

@Controller("/arquivos")
public class ArquivosController {

    private static final Duration VALIDADE_SAS = Duration.ofMinutes(10);

    private final ArquivoRepository arquivoRepository;
    private final ArquivoStorage arquivoStorage;
    private final ContratoRepository contratoRepository;

    public ArquivosController(ArquivoRepository arquivoRepository, ArquivoStorage arquivoStorage,
                               ContratoRepository contratoRepository) {
        this.arquivoRepository = arquivoRepository;
        this.arquivoStorage = arquivoStorage;
        this.contratoRepository = contratoRepository;
    }

    @ExecuteOn(TaskExecutors.BLOCKING)
    @Get(value = "/{id}/url", produces = MediaType.APPLICATION_JSON)
    public UrlResponse url(@PathVariable UUID id, HttpRequest<?> req) {
        Principal p = CurrentPrincipal.require(req);
        Arquivo arquivo = arquivoRepository.findById(id).orElseThrow(() -> new NaoEncontradoException("arquivo"));

        if (!arquivo.tipo().privado()) {
            return new UrlResponse(arquivoStorage.urlPublica(arquivo.tipo().container(), arquivo.blobKey()));
        }

        if (arquivo.tipo() == TipoArquivo.DOCUMENTO_CONTRATO || arquivo.tipo() == TipoArquivo.DOCUMENTO_GARANTIA) {
            Contrato contrato = contratoRepository.findById(arquivo.donoId())
                .orElseThrow(() -> new NaoEncontradoException("arquivo"));
            if (!isOwnerOrTenant(p, contrato)) {
                throw new NaoEncontradoException("arquivo");
            }
        }

        String url = arquivoStorage.urlTemporaria(arquivo.tipo().container(), arquivo.blobKey(), VALIDADE_SAS);
        return new UrlResponse(url);
    }

    private boolean isOwnerOrTenant(Principal p, Contrato c) {
        return (p.proprietarioId() != null && c.proprietarioId().equals(p.proprietarioId()))
            || (p.inquilinoId() != null && c.inquilinoId().equals(p.inquilinoId()));
    }

    @io.micronaut.serde.annotation.Serdeable
    public record UrlResponse(String url) {}
}
