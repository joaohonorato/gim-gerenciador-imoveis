package br.com.imoveis.infrastructure.rest;

import br.com.imoveis.application.exception.NaoEncontradoException;
import br.com.imoveis.application.ports.ChamadoRepository;
import br.com.imoveis.application.ports.ImovelRepository;
import br.com.imoveis.application.usecase.AbrirChamado;
import br.com.imoveis.application.usecase.AtualizarChamado;
import br.com.imoveis.domain.chamado.Chamado;
import br.com.imoveis.domain.imovel.Imovel;
import br.com.imoveis.infrastructure.auth.CurrentPrincipal;
import br.com.imoveis.infrastructure.auth.Principal;
import br.com.imoveis.infrastructure.rest.dto.ChamadoDtos.*;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.*;

import java.util.List;
import java.util.UUID;

@Controller
public class ChamadosController {

    private final AbrirChamado abrir;
    private final AtualizarChamado atualizar;
    private final ChamadoRepository chamadoRepository;
    private final ImovelRepository imovelRepository;

    public ChamadosController(AbrirChamado abrir, AtualizarChamado atualizar,
                               ChamadoRepository chamadoRepository, ImovelRepository imovelRepository) {
        this.abrir = abrir;
        this.atualizar = atualizar;
        this.chamadoRepository = chamadoRepository;
        this.imovelRepository = imovelRepository;
    }

    @Post(value = "/imoveis/{imovelId}/chamados", consumes = MediaType.APPLICATION_JSON, produces = MediaType.APPLICATION_JSON)
    public ChamadoResponse abrir(@PathVariable UUID imovelId, @Body NovoChamadoRequest body, HttpRequest<?> req) {
        CurrentPrincipal.require(req);
        Chamado c = abrir.execute(imovelId, body.inquilinoId(), body.categoria(), body.descricao());
        return ChamadoResponse.from(c);
    }

    @Get(value = "/imoveis/{imovelId}/chamados", produces = MediaType.APPLICATION_JSON)
    public List<ChamadoResponse> listar(@PathVariable UUID imovelId, HttpRequest<?> req) {
        Principal p = CurrentPrincipal.require(req);
        Imovel imovel = imovelRepository.findById(imovelId)
            .filter(i -> i.proprietarioId().equals(p.proprietarioId()))
            .orElseThrow(() -> new NaoEncontradoException("imóvel"));
        return chamadoRepository.findByImovel(imovel.id()).stream().map(ChamadoResponse::from).toList();
    }

    @Patch(value = "/chamados/{id}", consumes = MediaType.APPLICATION_JSON, produces = MediaType.APPLICATION_JSON)
    public ChamadoResponse atualizar(@PathVariable UUID id, @Body AtualizarChamadoRequest body, HttpRequest<?> req) {
        Principal p = CurrentPrincipal.require(req);
        Chamado c = atualizar.execute(id, p.proprietarioId(), body.status());
        return ChamadoResponse.from(c);
    }
}
