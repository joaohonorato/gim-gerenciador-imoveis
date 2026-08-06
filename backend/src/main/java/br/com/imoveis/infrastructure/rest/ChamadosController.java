package br.com.imoveis.infrastructure.rest;

import br.com.imoveis.application.exception.NaoEncontradoException;
import br.com.imoveis.application.ports.CategoriaChamadoRepository;
import br.com.imoveis.application.ports.ChamadoRepository;
import br.com.imoveis.application.ports.ContratoRepository;
import br.com.imoveis.application.ports.ImovelRepository;
import br.com.imoveis.application.usecase.AbrirChamado;
import br.com.imoveis.application.usecase.AtualizarChamado;
import br.com.imoveis.application.usecase.ListarChamados;
import br.com.imoveis.application.usecase.RegistrarCategoriaChamado;
import br.com.imoveis.domain.chamado.CategoriaChamado;
import br.com.imoveis.domain.chamado.Chamado;
import br.com.imoveis.domain.imovel.Imovel;
import br.com.imoveis.infrastructure.auth.CurrentPrincipal;
import br.com.imoveis.infrastructure.auth.Principal;
import br.com.imoveis.infrastructure.rest.dto.ChamadoDtos.*;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import java.util.List;
import java.util.UUID;

@Controller
@Tag(name = "Chamados")
public class ChamadosController {

    private final AbrirChamado abrir;
    private final AtualizarChamado atualizar;
    private final ListarChamados listarChamados;
    private final RegistrarCategoriaChamado registrarCategoriaChamado;
    private final ChamadoRepository chamadoRepository;
    private final ImovelRepository imovelRepository;
    private final CategoriaChamadoRepository categoriaChamadoRepository;
    private final ContratoRepository contratoRepository;

    public ChamadosController(AbrirChamado abrir, AtualizarChamado atualizar, ListarChamados listarChamados,
                               RegistrarCategoriaChamado registrarCategoriaChamado,
                               ChamadoRepository chamadoRepository, ImovelRepository imovelRepository,
                               CategoriaChamadoRepository categoriaChamadoRepository,
                               ContratoRepository contratoRepository) {
        this.abrir = abrir;
        this.atualizar = atualizar;
        this.listarChamados = listarChamados;
        this.registrarCategoriaChamado = registrarCategoriaChamado;
        this.chamadoRepository = chamadoRepository;
        this.imovelRepository = imovelRepository;
        this.categoriaChamadoRepository = categoriaChamadoRepository;
        this.contratoRepository = contratoRepository;
    }

    @Post(value = "/imoveis/{imovelId}/chamados", consumes = MediaType.APPLICATION_JSON, produces = MediaType.APPLICATION_JSON)
    public ChamadoResponse abrir(@PathVariable UUID imovelId, @Body NovoChamadoRequest body, HttpRequest<?> req) {
        Principal p = CurrentPrincipal.require(req);
        // inquilinoId vem sempre do principal autenticado, nunca do corpo da
        // requisição — senão qualquer inquilino logado poderia abrir chamado
        // em nome de outro.
        Chamado c = abrir.execute(imovelId, p.requireInquilinoId(), body.categoriaId(), body.descricao());
        return toResponse(c);
    }

    @Get(value = "/imoveis/{imovelId}/chamados", produces = MediaType.APPLICATION_JSON)
    public List<ChamadoResponse> listar(@PathVariable UUID imovelId, HttpRequest<?> req) {
        Principal p = CurrentPrincipal.require(req);
        Imovel imovel = imovelRepository.findById(imovelId)
            .filter(i -> i.proprietarioId().equals(p.proprietarioId()))
            .orElseThrow(() -> new NaoEncontradoException("imóvel"));
        return chamadoRepository.findByImovel(imovel.id()).stream().map(this::toResponse).toList();
    }

    // Catálogo de categorias do proprietário dono do imóvel — usado tanto
    // pelo inquilino (pra escolher categoria ao abrir um chamado nesse
    // imóvel específico) quanto pelo próprio proprietário. Resolve o
    // proprietarioId a partir do imóvel, não do principal chamando, porque
    // um inquilino não tem proprietarioId no seu principal — mas ainda exige
    // que o chamador tenha alguma relação com o imóvel (dono, ou inquilino
    // com contrato na unidade), senão qualquer autenticado poderia enumerar
    // o catálogo de categorias de qualquer imovelId.
    @Get(value = "/imoveis/{imovelId}/categorias-chamado", produces = MediaType.APPLICATION_JSON)
    public List<CategoriaChamadoResponse> listarCategoriasDoImovel(@PathVariable UUID imovelId, HttpRequest<?> req) {
        Principal p = CurrentPrincipal.require(req);
        Imovel imovel = imovelRepository.findById(imovelId)
            .orElseThrow(() -> new NaoEncontradoException("imóvel"));
        java.util.Set<UUID> unidadeIds = imovel.unidades().stream()
            .map(br.com.imoveis.domain.imovel.Unidade::id).collect(java.util.stream.Collectors.toSet());
        boolean autorizado = (p.proprietarioId() != null && imovel.proprietarioId().equals(p.proprietarioId()))
            || (p.inquilinoId() != null && contratoRepository.findByInquilinoId(p.inquilinoId()).stream()
                    .anyMatch(c -> unidadeIds.contains(c.unidadeId())));
        if (!autorizado) {
            throw new NaoEncontradoException("imóvel");
        }
        return categoriaChamadoRepository.findByProprietarioId(imovel.proprietarioId()).stream()
            .map(cc -> new CategoriaChamadoResponse(cc.id(), cc.nome()))
            .toList();
    }

    // Chamados do usuário autenticado, independente de ser proprietário ou
    // inquilino — cada lado enxerga só os próprios (imóveis que possui, ou
    // chamados que abriu), com filtro opcional pelo outro lado da relação.
    @Get(value = "/chamados", produces = MediaType.APPLICATION_JSON)
    public List<ChamadoResponse> listarMeusChamados(
            @Nullable @QueryValue UUID inquilinoId,
            @Nullable @QueryValue UUID proprietarioId,
            HttpRequest<?> req) {
        Principal p = CurrentPrincipal.require(req);
        List<Chamado> chamados = p.inquilinoId() != null
            ? listarChamados.paraInquilino(p.inquilinoId(), proprietarioId)
            : listarChamados.paraProprietario(p.requireProprietarioId(), inquilinoId);
        return chamados.stream().map(this::toResponse).toList();
    }

    @Patch(value = "/chamados/{id}", consumes = MediaType.APPLICATION_JSON, produces = MediaType.APPLICATION_JSON)
    public ChamadoResponse atualizar(@PathVariable UUID id, @Body AtualizarChamadoRequest body, HttpRequest<?> req) {
        Principal p = CurrentPrincipal.require(req);
        Chamado c = atualizar.execute(id, p.proprietarioId(), body.status());
        return toResponse(c);
    }

    @Get(value = "/categorias-chamado", produces = MediaType.APPLICATION_JSON)
    public List<CategoriaChamadoResponse> listarCategorias(HttpRequest<?> req) {
        Principal p = CurrentPrincipal.require(req);
        return categoriaChamadoRepository.findByProprietarioId(p.requireProprietarioId()).stream()
            .map(cc -> new CategoriaChamadoResponse(cc.id(), cc.nome()))
            .toList();
    }

    @Status(HttpStatus.CREATED)
    @Post(value = "/categorias-chamado", consumes = MediaType.APPLICATION_JSON, produces = MediaType.APPLICATION_JSON)
    public CategoriaChamadoResponse criarCategoria(@Body @Valid NovaCategoriaChamadoRequest body, HttpRequest<?> req) {
        Principal p = CurrentPrincipal.require(req);
        CategoriaChamado categoria = registrarCategoriaChamado.execute(p.requireProprietarioId(), body.nome());
        return new CategoriaChamadoResponse(categoria.id(), categoria.nome());
    }

    private ChamadoResponse toResponse(Chamado chamado) {
        String categoriaNome = categoriaChamadoRepository.findById(chamado.categoriaId())
            .map(CategoriaChamado::nome)
            .orElse(null);
        String unidadeNome = imovelRepository.findUnidadeById(chamado.unidadeId())
            .map(br.com.imoveis.domain.imovel.Unidade::nome)
            .orElse(null);
        return ChamadoResponse.from(chamado, categoriaNome, unidadeNome);
    }
}
