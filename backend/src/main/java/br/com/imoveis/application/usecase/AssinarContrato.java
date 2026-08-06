package br.com.imoveis.application.usecase;

import br.com.imoveis.application.exception.CadastroIncompletoException;
import br.com.imoveis.application.exception.NaoEncontradoException;
import br.com.imoveis.application.ports.ContratoRepository;
import br.com.imoveis.application.ports.ConviteRepository;
import br.com.imoveis.application.ports.ImovelRepository;
import br.com.imoveis.application.ports.ProprietarioRepository;
import br.com.imoveis.domain.auditoria.EntidadeAuditoria;
import br.com.imoveis.domain.auditoria.TipoEventoAuditoria;
import br.com.imoveis.domain.contrato.Contrato;
import br.com.imoveis.domain.contrato.ContratoAssinaturaStatus;
import br.com.imoveis.domain.contrato.Pagamento;
import br.com.imoveis.domain.contrato.ParteContrato;
import br.com.imoveis.domain.convite.ConviteStatus;
import br.com.imoveis.domain.imovel.Unidade;
import br.com.imoveis.domain.proprietario.Proprietario;
import io.micronaut.transaction.annotation.Transactional;
import jakarta.inject.Singleton;

import java.util.UUID;

@Singleton
@Transactional
public class AssinarContrato {

    private final ContratoRepository contratoRepository;
    private final ImovelRepository imovelRepository;
    private final ConviteRepository conviteRepository;
    private final ProprietarioRepository proprietarioRepository;
    private final RegistrarEventoAuditoria registrarEventoAuditoria;

    public AssinarContrato(ContratoRepository contratoRepository, ImovelRepository imovelRepository,
                            ConviteRepository conviteRepository, ProprietarioRepository proprietarioRepository,
                            RegistrarEventoAuditoria registrarEventoAuditoria) {
        this.contratoRepository = contratoRepository;
        this.imovelRepository = imovelRepository;
        this.conviteRepository = conviteRepository;
        this.proprietarioRepository = proprietarioRepository;
        this.registrarEventoAuditoria = registrarEventoAuditoria;
    }

    public Contrato execute(UUID contratoId, ParteContrato parte) {
        Contrato contrato = contratoRepository.findById(contratoId)
            .orElseThrow(() -> new NaoEncontradoException("contrato"));

        if (parte == ParteContrato.PROPRIETARIO) {
            Proprietario proprietario = proprietarioRepository.findById(contrato.proprietarioId())
                .orElseThrow(() -> new NaoEncontradoException("proprietário"));
            if (!proprietario.cadastroCompleto()) {
                throw new CadastroIncompletoException("CPF/CNPJ obrigatório para assinar contrato");
            }
        }

        contrato.assinar(parte);

        Contrato salvo = contratoRepository.save(contrato);
        if (salvo.statusAssinatura() == ContratoAssinaturaStatus.ASSINADO) {
            for (Pagamento p : salvo.pagamentos()) {
                contratoRepository.savePagamento(p);
            }
            Unidade unidade = imovelRepository.findUnidadeById(salvo.unidadeId())
                .orElseThrow(() -> new NaoEncontradoException("unidade"));
            unidade.alugar();
            imovelRepository.saveUnidade(unidade);

            if (salvo.conviteId() != null) {
                conviteRepository.findById(salvo.conviteId()).ifPresent(convite -> {
                    if (convite.status() == ConviteStatus.EM_ANALISE) {
                        convite.marcarConsumido();
                        conviteRepository.save(convite);
                        registrarEventoAuditoria.execute(EntidadeAuditoria.CONVITE, convite.id(),
                            TipoEventoAuditoria.CONSUMIDO, null);
                    }
                });
            }
        }
        return salvo;
    }
}
