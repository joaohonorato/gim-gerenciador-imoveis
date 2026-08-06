package br.com.imoveis.application.usecase;

import br.com.imoveis.application.exception.NaoEncontradoException;
import br.com.imoveis.application.ports.Clock;
import br.com.imoveis.application.ports.ContaAcessoRepository;
import br.com.imoveis.application.ports.ConviteRepository;
import br.com.imoveis.application.ports.ImovelRepository;
import br.com.imoveis.domain.auditoria.EntidadeAuditoria;
import br.com.imoveis.domain.auditoria.TipoEventoAuditoria;
import br.com.imoveis.domain.auth.ContaAcesso;
import br.com.imoveis.domain.convite.Candidatura;
import br.com.imoveis.domain.convite.Convite;
import br.com.imoveis.domain.imovel.Imovel;
import br.com.imoveis.domain.imovel.Unidade;
import br.com.imoveis.domain.shared.Email;
import jakarta.inject.Singleton;

import java.util.UUID;

@Singleton
public class GerarConvite {

    private final ImovelRepository imovelRepository;
    private final ConviteRepository conviteRepository;
    private final Clock clock;
    private final ContaAcessoRepository contaAcessoRepository;
    private final RegistrarEventoAuditoria registrarEventoAuditoria;

    public GerarConvite(ImovelRepository imovelRepository,
                        ConviteRepository conviteRepository,
                        Clock clock,
                        ContaAcessoRepository contaAcessoRepository,
                        RegistrarEventoAuditoria registrarEventoAuditoria) {
        this.imovelRepository = imovelRepository;
        this.conviteRepository = conviteRepository;
        this.clock = clock;
        this.contaAcessoRepository = contaAcessoRepository;
        this.registrarEventoAuditoria = registrarEventoAuditoria;
    }

    public Convite execute(UUID imovelId,
                           UUID unidadeId,
                           UUID proprietarioId,
                           Convite.CondicoesConvite condicoes,
                           String emailInquilino) {
        Imovel imovel = imovelRepository.findById(imovelId)
            .filter(i -> i.proprietarioId().equals(proprietarioId))
            .orElseThrow(() -> new NaoEncontradoException("imóvel"));
        // unidadeId é opcional: imóveis com uma unidade só (o caso comum,
        // ainda a esmagadora maioria) continuam funcionando sem o
        // proprietário precisar escolher nada — só quem subdividiu o imóvel
        // (ver AdicionarUnidades) precisa informar qual unidade está sendo
        // ofertada nesse convite específico.
        Unidade unidade = unidadeId == null
            ? imovel.unidadePadrao()
            : imovel.unidades().stream().filter(u -> u.id().equals(unidadeId)).findFirst()
                .orElseThrow(() -> new NaoEncontradoException("unidade"));
        Convite c = Convite.gerar(imovel.id(), unidade.id(), proprietarioId, condicoes, clock.now());
        c = conviteRepository.save(c);
        registrarEventoAuditoria.execute(EntidadeAuditoria.CONVITE, c.id(), TipoEventoAuditoria.CRIADO, null);

        if (emailInquilino == null || emailInquilino.isBlank()) {
            return c;
        }

        ContaAcesso conta = contaAcessoRepository.findByEmail(new Email(emailInquilino.trim())).orElse(null);
        if (conta == null || conta.inquilinoId() == null) {
            return c;
        }

        Candidatura candidatura = conviteRepository.saveCandidatura(Candidatura.nova(c.id(), conta.inquilinoId(), clock.now()));
        c.marcarCandidaturaCriada(candidatura.id());
        registrarEventoAuditoria.execute(EntidadeAuditoria.CONVITE, c.id(), TipoEventoAuditoria.CANDIDATURA_CRIADA,
            "vínculo automático com inquilino já cadastrado");
        return conviteRepository.save(c);
    }
}
