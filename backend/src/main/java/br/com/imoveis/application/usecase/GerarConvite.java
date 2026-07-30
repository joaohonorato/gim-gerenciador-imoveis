package br.com.imoveis.application.usecase;

import br.com.imoveis.application.exception.NaoEncontradoException;
import br.com.imoveis.application.ports.Clock;
import br.com.imoveis.application.ports.ContaAcessoRepository;
import br.com.imoveis.application.ports.ConviteRepository;
import br.com.imoveis.application.ports.ImovelRepository;
import br.com.imoveis.domain.auth.ContaAcesso;
import br.com.imoveis.domain.convite.Candidatura;
import br.com.imoveis.domain.convite.Convite;
import br.com.imoveis.domain.imovel.Imovel;
import br.com.imoveis.domain.shared.Email;
import jakarta.inject.Singleton;

import java.util.UUID;

@Singleton
public class GerarConvite {

    private final ImovelRepository imovelRepository;
    private final ConviteRepository conviteRepository;
    private final Clock clock;
    private final ContaAcessoRepository contaAcessoRepository;

    public GerarConvite(ImovelRepository imovelRepository,
                        ConviteRepository conviteRepository,
                        Clock clock,
                        ContaAcessoRepository contaAcessoRepository) {
        this.imovelRepository = imovelRepository;
        this.conviteRepository = conviteRepository;
        this.clock = clock;
        this.contaAcessoRepository = contaAcessoRepository;
    }

    public Convite execute(UUID imovelId,
                           UUID proprietarioId,
                           Convite.CondicoesConvite condicoes,
                           String emailInquilino) {
        Imovel imovel = imovelRepository.findById(imovelId)
            .filter(i -> i.proprietarioId().equals(proprietarioId))
            .orElseThrow(() -> new NaoEncontradoException("imóvel"));
        Convite c = Convite.gerar(imovel.id(), imovel.unidadePadrao().id(), proprietarioId, condicoes, clock.now());
        c = conviteRepository.save(c);

        if (emailInquilino == null || emailInquilino.isBlank()) {
            return c;
        }

        ContaAcesso conta = contaAcessoRepository.findByEmail(new Email(emailInquilino.trim())).orElse(null);
        if (conta == null || conta.inquilinoId() == null) {
            return c;
        }

        Candidatura candidatura = conviteRepository.saveCandidatura(Candidatura.nova(c.id(), conta.inquilinoId(), clock.now()));
        c.marcarCandidaturaCriada(candidatura.id());
        return conviteRepository.save(c);
    }
}
