package br.com.imoveis.infrastructure.rest;

import br.com.imoveis.application.exception.NaoEncontradoException;
import br.com.imoveis.application.ports.ImovelRepository;
import br.com.imoveis.application.usecase.CadastrarImovel;
import br.com.imoveis.application.usecase.ListarImoveisDoProprietario;
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

import java.util.List;
import java.util.UUID;

@Controller("/imoveis")
public class ImoveisController {

    private final CadastrarImovel cadastrarImovel;
    private final ListarImoveisDoProprietario listar;
    private final ImovelRepository imovelRepository;

    public ImoveisController(CadastrarImovel cadastrarImovel, ListarImoveisDoProprietario listar,
                              ImovelRepository imovelRepository) {
        this.cadastrarImovel = cadastrarImovel;
        this.listar = listar;
        this.imovelRepository = imovelRepository;
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
            .map(ImovelResponse::from)
            .toList();
    }

    @Status(HttpStatus.CREATED)
    @Post(consumes = MediaType.APPLICATION_JSON, produces = MediaType.APPLICATION_JSON)
    public ImovelResponse criar(@Body NovoImovelRequest body, HttpRequest<?> req) {
        Principal p = CurrentPrincipal.require(req);
        Imovel i = cadastrarImovel.execute(p.proprietarioId(), body.endereco(), body.cidade(), body.matricula(),
            body.numero(), body.bairro(), body.complemento(), body.tipoImovel());
        return ImovelResponse.from(i);
    }

    @Get(value = "/{id}", produces = MediaType.APPLICATION_JSON)
    public ImovelResponse detalhe(@PathVariable UUID id, HttpRequest<?> req) {
        Principal p = CurrentPrincipal.require(req);
        Imovel imovel = imovelRepository.findById(id)
            .filter(i -> i.proprietarioId().equals(p.proprietarioId()))
            .orElseThrow(() -> new NaoEncontradoException("imóvel"));
        return ImovelResponse.from(imovel);
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
