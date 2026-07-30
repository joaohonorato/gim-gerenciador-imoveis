package br.com.imoveis.application.usecase;

import br.com.imoveis.application.exception.NaoEncontradoException;
import br.com.imoveis.application.ports.ChamadoRepository;
import br.com.imoveis.application.ports.Clock;
import br.com.imoveis.application.ports.ImovelRepository;
import br.com.imoveis.domain.chamado.Chamado;
import br.com.imoveis.domain.chamado.ChamadoStatus;
import br.com.imoveis.domain.imovel.Imovel;
import io.micronaut.transaction.annotation.Transactional;
import jakarta.inject.Singleton;

import java.util.UUID;

@Singleton
@Transactional
public class AtualizarChamado {

    private final ChamadoRepository chamadoRepository;
    private final ImovelRepository imovelRepository;
    private final Clock clock;

    public AtualizarChamado(ChamadoRepository chamadoRepository, ImovelRepository imovelRepository, Clock clock) {
        this.chamadoRepository = chamadoRepository;
        this.imovelRepository = imovelRepository;
        this.clock = clock;
    }

    public Chamado execute(UUID chamadoId, UUID proprietarioId, ChamadoStatus novoStatus) {
        Chamado chamado = chamadoRepository.findById(chamadoId)
            .orElseThrow(() -> new NaoEncontradoException("chamado"));
        Imovel imovel = imovelRepository.findById(chamado.imovelId())
            .orElseThrow(() -> new NaoEncontradoException("chamado"));
        if (!imovel.proprietarioId().equals(proprietarioId)) {
            throw new NaoEncontradoException("chamado");
        }
        Runnable transicao = switch (novoStatus) {
            case EM_ANDAMENTO -> chamado::iniciarAtendimento;
            case RESOLVIDO    -> () -> chamado.resolver(clock.now());
            case ABERTO       -> () -> { throw new IllegalArgumentException("não é permitido reabrir chamado"); };
        };
        transicao.run();
        return chamadoRepository.save(chamado);
    }
}
