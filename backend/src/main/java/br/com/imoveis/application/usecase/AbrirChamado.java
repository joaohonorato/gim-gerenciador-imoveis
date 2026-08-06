package br.com.imoveis.application.usecase;

import br.com.imoveis.application.exception.NaoEncontradoException;
import br.com.imoveis.application.ports.CategoriaChamadoRepository;
import br.com.imoveis.application.ports.ChamadoRepository;
import br.com.imoveis.application.ports.Clock;
import br.com.imoveis.application.ports.ContratoRepository;
import br.com.imoveis.application.ports.ImovelRepository;
import br.com.imoveis.domain.chamado.Chamado;
import br.com.imoveis.domain.contrato.Contrato;
import br.com.imoveis.domain.imovel.Imovel;
import br.com.imoveis.domain.imovel.Unidade;
import jakarta.inject.Singleton;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Singleton
public class AbrirChamado {

    private final ChamadoRepository chamadoRepository;
    private final ImovelRepository imovelRepository;
    private final CategoriaChamadoRepository categoriaChamadoRepository;
    private final ContratoRepository contratoRepository;
    private final Clock clock;

    public AbrirChamado(ChamadoRepository chamadoRepository, ImovelRepository imovelRepository,
                         CategoriaChamadoRepository categoriaChamadoRepository, ContratoRepository contratoRepository,
                         Clock clock) {
        this.chamadoRepository = chamadoRepository;
        this.imovelRepository = imovelRepository;
        this.categoriaChamadoRepository = categoriaChamadoRepository;
        this.contratoRepository = contratoRepository;
        this.clock = clock;
    }

    public Chamado execute(UUID imovelId, UUID inquilinoId, UUID categoriaId, String descricao) {
        Imovel imovel = imovelRepository.findById(imovelId)
            .orElseThrow(() -> new NaoEncontradoException("imóvel"));
        // Só quem tem contrato em alguma unidade desse imóvel pode abrir
        // chamado nele — sem isso, qualquer inquilino autenticado poderia
        // criar chamados em imóveis de proprietários com quem nunca teve
        // relação nenhuma. A unidade do chamado é a mesma do contrato
        // encontrado (não pedimos pro inquilino escolher — ele só tem
        // contrato numa unidade daquele imóvel, na prática).
        Set<UUID> unidadeIds = imovel.unidades().stream().map(Unidade::id).collect(Collectors.toSet());
        Contrato contratoDoInquilino = contratoRepository.findByInquilinoId(inquilinoId).stream()
            .filter(c -> unidadeIds.contains(c.unidadeId()))
            .findFirst()
            .orElseThrow(() -> new NaoEncontradoException("imóvel"));
        // categoriaId pertence ao catálogo do proprietário do imóvel — mesma
        // validação de posse já usada pra tipoContaId em RegistrarConta.
        categoriaChamadoRepository.findById(categoriaId)
            .filter(cc -> cc.proprietarioId().equals(imovel.proprietarioId()))
            .orElseThrow(() -> new NaoEncontradoException("categoria de chamado"));
        Chamado c = Chamado.abrir(imovelId, contratoDoInquilino.unidadeId(), inquilinoId, categoriaId, descricao, clock.now());
        return chamadoRepository.save(c);
    }
}
