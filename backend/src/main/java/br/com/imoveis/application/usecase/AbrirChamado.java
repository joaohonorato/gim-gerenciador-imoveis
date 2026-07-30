package br.com.imoveis.application.usecase;

import br.com.imoveis.application.exception.NaoEncontradoException;
import br.com.imoveis.application.ports.ChamadoRepository;
import br.com.imoveis.application.ports.Clock;
import br.com.imoveis.application.ports.ImovelRepository;
import br.com.imoveis.domain.chamado.CategoriaChamado;
import br.com.imoveis.domain.chamado.Chamado;
import jakarta.inject.Singleton;

import java.util.UUID;

@Singleton
public class AbrirChamado {

    private final ChamadoRepository chamadoRepository;
    private final ImovelRepository imovelRepository;
    private final Clock clock;

    public AbrirChamado(ChamadoRepository chamadoRepository, ImovelRepository imovelRepository, Clock clock) {
        this.chamadoRepository = chamadoRepository;
        this.imovelRepository = imovelRepository;
        this.clock = clock;
    }

    public Chamado execute(UUID imovelId, UUID inquilinoId, CategoriaChamado categoria, String descricao) {
        imovelRepository.findById(imovelId)
            .orElseThrow(() -> new NaoEncontradoException("imóvel"));
        Chamado c = Chamado.abrir(imovelId, inquilinoId, categoria, descricao, clock.now());
        return chamadoRepository.save(c);
    }
}
