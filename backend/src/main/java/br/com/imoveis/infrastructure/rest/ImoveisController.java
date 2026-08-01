package br.com.imoveis.infrastructure.rest;

import br.com.imoveis.application.exception.NaoEncontradoException;
import br.com.imoveis.application.ports.ArquivoRepository;
import br.com.imoveis.application.ports.ArquivoStorage;
import br.com.imoveis.application.ports.ImovelRepository;
import br.com.imoveis.application.usecase.AdicionarFotoImovel;
import br.com.imoveis.application.usecase.CadastrarImovel;
import br.com.imoveis.application.usecase.ListarImoveisDoProprietario;
import br.com.imoveis.application.usecase.RemoverFotoImovel;
import br.com.imoveis.domain.arquivo.Arquivo;
import br.com.imoveis.domain.arquivo.TipoArquivo;
import br.com.imoveis.domain.imovel.Imovel;
import br.com.imoveis.domain.imovel.TipoImovel;
import br.com.imoveis.domain.imovel.UnidadeStatus;
import br.com.imoveis.infrastructure.auth.CurrentPrincipal;
import br.com.imoveis.infrastructure.auth.Principal;
import br.com.imoveis.infrastructure.rest.dto.ImovelDtos.*;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.*;
import io.micronaut.http.annotation.Status;
import io.micronaut.http.multipart.CompletedFileUpload;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.UUID;

@Controller("/imoveis")
public class ImoveisController {

    private final CadastrarImovel cadastrarImovel;
    private final ListarImoveisDoProprietario listar;
    private final ImovelRepository imovelRepository;
    private final AdicionarFotoImovel adicionarFotoImovel;
    private final RemoverFotoImovel removerFotoImovel;
    private final ArquivoRepository arquivoRepository;
    private final ArquivoStorage arquivoStorage;

    public ImoveisController(CadastrarImovel cadastrarImovel, ListarImoveisDoProprietario listar,
                              ImovelRepository imovelRepository, AdicionarFotoImovel adicionarFotoImovel,
                              RemoverFotoImovel removerFotoImovel, ArquivoRepository arquivoRepository,
                              ArquivoStorage arquivoStorage) {
        this.cadastrarImovel = cadastrarImovel;
        this.listar = listar;
        this.imovelRepository = imovelRepository;
        this.adicionarFotoImovel = adicionarFotoImovel;
        this.removerFotoImovel = removerFotoImovel;
        this.arquivoRepository = arquivoRepository;
        this.arquivoStorage = arquivoStorage;
    }

    @Get(produces = MediaType.APPLICATION_JSON)
    public List<ImovelResponse> listar(HttpRequest<?> req,
                                       @Nullable @QueryValue String busca,
                                       @Nullable @QueryValue String cidade,
                                       @Nullable @QueryValue UnidadeStatus status,
                                       @Nullable @QueryValue TipoImovel tipoImovel) {
        Principal p = CurrentPrincipal.require(req);
        return listar.execute(p.proprietarioId()).stream()
            .filter(i -> filtrarBusca(i, busca))
            .filter(i -> filtrarCidade(i, cidade))
            .filter(i -> status == null || i.unidadePadrao().status() == status)
            .filter(i -> tipoImovel == null || i.tipoImovel() == tipoImovel)
            .map(this::toResponse)
            .toList();
    }

    @Status(HttpStatus.CREATED)
    @Post(consumes = MediaType.APPLICATION_JSON, produces = MediaType.APPLICATION_JSON)
    public ImovelResponse criar(@Body NovoImovelRequest body, HttpRequest<?> req) {
        Principal p = CurrentPrincipal.require(req);
        Imovel i = cadastrarImovel.execute(p.proprietarioId(), body.endereco(), body.cidade(), body.matricula(),
            body.numero(), body.bairro(), body.complemento(), body.tipoImovel());
        return toResponse(i);
    }

    @Get(value = "/{id}", produces = MediaType.APPLICATION_JSON)
    public ImovelResponse detalhe(@PathVariable UUID id, HttpRequest<?> req) {
        Principal p = CurrentPrincipal.require(req);
        Imovel imovel = imovelRepository.findById(id)
            .filter(i -> i.proprietarioId().equals(p.proprietarioId()))
            .orElseThrow(() -> new NaoEncontradoException("imóvel"));
        return toResponse(imovel);
    }

    @ExecuteOn(TaskExecutors.BLOCKING)
    @Status(HttpStatus.CREATED)
    @Post(value = "/{id}/fotos", consumes = MediaType.MULTIPART_FORM_DATA, produces = MediaType.APPLICATION_JSON)
    public ImovelResponse adicionarFoto(@PathVariable UUID id, CompletedFileUpload foto, HttpRequest<?> req) {
        Principal p = CurrentPrincipal.require(req);
        Imovel imovel = imovelRepository.findById(id)
            .filter(i -> i.proprietarioId().equals(p.proprietarioId()))
            .orElseThrow(() -> new NaoEncontradoException("imóvel"));
        String contentType = foto.getContentType().map(MediaType::toString).orElse(null);
        try {
            adicionarFotoImovel.execute(imovel.id(), foto.getFilename(), contentType, foto.getSize(), foto.getInputStream());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return toResponse(imovel);
    }

    @ExecuteOn(TaskExecutors.BLOCKING)
    @Delete(value = "/{id}/fotos/{fotoId}", produces = MediaType.APPLICATION_JSON)
    public ImovelResponse removerFoto(@PathVariable UUID id, @PathVariable UUID fotoId, HttpRequest<?> req) {
        Principal p = CurrentPrincipal.require(req);
        Imovel imovel = imovelRepository.findById(id)
            .filter(i -> i.proprietarioId().equals(p.proprietarioId()))
            .orElseThrow(() -> new NaoEncontradoException("imóvel"));
        removerFotoImovel.execute(imovel.id(), fotoId);
        return toResponse(imovel);
    }

    private ImovelResponse toResponse(Imovel imovel) {
        List<FotoResponse> fotos = arquivoRepository.findByDonoIdAndTipo(imovel.id(), TipoArquivo.FOTO_IMOVEL).stream()
            .map(this::toFotoResponse)
            .toList();
        return ImovelResponse.from(imovel, fotos);
    }

    private FotoResponse toFotoResponse(Arquivo arquivo) {
        return new FotoResponse(arquivo.id(), arquivoStorage.urlPublica(arquivo.tipo().container(), arquivo.blobKey()));
    }

    private boolean filtrarCidade(Imovel i, String cidade) {
        if (cidade == null || cidade.isBlank()) {
            return true;
        }
        return i.cidade().equalsIgnoreCase(cidade.trim());
    }

    private boolean filtrarBusca(Imovel i, String busca) {
        if (busca == null || busca.isBlank()) {
            return true;
        }
        String termo = busca.trim().toLowerCase();
        return contem(i.endereco(), termo)
            || contem(i.matricula(), termo)
            || contem(i.bairro(), termo)
            || contem(i.numero(), termo)
            || contem(i.cidade(), termo);
    }

    private boolean contem(String valor, String termo) {
        return valor != null && valor.toLowerCase().contains(termo);
    }
}
