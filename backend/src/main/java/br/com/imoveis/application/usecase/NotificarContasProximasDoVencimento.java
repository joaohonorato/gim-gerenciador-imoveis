package br.com.imoveis.application.usecase;

import br.com.imoveis.application.ports.AlertaVencimentoEmailSender;
import br.com.imoveis.application.ports.Clock;
import br.com.imoveis.application.ports.ContaAcessoRepository;
import br.com.imoveis.application.ports.ContratoRepository;
import br.com.imoveis.application.ports.ImovelRepository;
import br.com.imoveis.application.ports.InquilinoRepository;
import br.com.imoveis.application.ports.ProprietarioRepository;
import br.com.imoveis.application.ports.PushNotificationSender;
import br.com.imoveis.application.ports.PushTokenRepository;
import br.com.imoveis.application.ports.TipoContaRepository;
import br.com.imoveis.domain.auth.ContaAcesso;
import br.com.imoveis.domain.contrato.Contrato;
import br.com.imoveis.domain.contrato.ParteContrato;
import br.com.imoveis.domain.imovel.Conta;
import br.com.imoveis.domain.imovel.Imovel;
import br.com.imoveis.domain.imovel.TipoConta;
import br.com.imoveis.domain.imovel.Unidade;
import br.com.imoveis.domain.inquilino.Inquilino;
import br.com.imoveis.domain.proprietario.Proprietario;
import br.com.imoveis.domain.shared.Email;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

// Avisa quem é responsável pela conta (proprietário ou inquilino, conforme
// CONTA.responsavel — ver docs/especificacao-produto.md §3-4) quando ela
// está a 5 dias ou menos do vencimento.
@Singleton
public class NotificarContasProximasDoVencimento {

    private static final Logger log = LoggerFactory.getLogger(NotificarContasProximasDoVencimento.class);
    private static final int DIAS_DE_ANTECEDENCIA = 5;

    private final ImovelRepository imovelRepository;
    private final ContratoRepository contratoRepository;
    private final TipoContaRepository tipoContaRepository;
    private final ProprietarioRepository proprietarioRepository;
    private final InquilinoRepository inquilinoRepository;
    private final ContaAcessoRepository contaAcessoRepository;
    private final PushTokenRepository pushTokenRepository;
    private final AlertaVencimentoEmailSender emailSender;
    private final PushNotificationSender pushSender;
    private final Clock clock;

    public NotificarContasProximasDoVencimento(ImovelRepository imovelRepository, ContratoRepository contratoRepository,
                                                 TipoContaRepository tipoContaRepository,
                                                 ProprietarioRepository proprietarioRepository,
                                                 InquilinoRepository inquilinoRepository,
                                                 ContaAcessoRepository contaAcessoRepository,
                                                 PushTokenRepository pushTokenRepository,
                                                 AlertaVencimentoEmailSender emailSender,
                                                 PushNotificationSender pushSender, Clock clock) {
        this.imovelRepository = imovelRepository;
        this.contratoRepository = contratoRepository;
        this.tipoContaRepository = tipoContaRepository;
        this.proprietarioRepository = proprietarioRepository;
        this.inquilinoRepository = inquilinoRepository;
        this.contaAcessoRepository = contaAcessoRepository;
        this.pushTokenRepository = pushTokenRepository;
        this.emailSender = emailSender;
        this.pushSender = pushSender;
        this.clock = clock;
    }

    public void execute() {
        LocalDate hoje = clock.today();
        List<Conta> candidatas = imovelRepository.findContasParaAlerta(hoje, hoje.plusDays(DIAS_DE_ANTECEDENCIA));
        for (Conta conta : candidatas) {
            try {
                notificar(conta);
                conta.marcarAlertaEnviado();
                imovelRepository.saveConta(conta);
            } catch (Exception e) {
                log.warn("Falha ao notificar vencimento da conta {}", conta.id(), e);
            }
        }
    }

    private void notificar(Conta conta) {
        Unidade unidade = imovelRepository.findUnidadeById(conta.unidadeId()).orElse(null);
        if (unidade == null) return;
        Imovel imovel = imovelRepository.findById(unidade.imovelId()).orElse(null);
        if (imovel == null) return;
        String enderecoImovel = imovel.enderecoCompleto();
        String tipoContaNome = tipoContaRepository.findById(conta.tipoContaId()).map(TipoConta::nome).orElse("Conta");

        if (conta.responsavel() == ParteContrato.INQUILINO) {
            notificarInquilino(conta, enderecoImovel, tipoContaNome);
        } else {
            notificarProprietario(imovel.proprietarioId(), conta, enderecoImovel, tipoContaNome);
        }
    }

    private void notificarInquilino(Conta conta, String enderecoImovel, String tipoContaNome) {
        Contrato contrato = contratoRepository.findById(conta.contratoId()).orElse(null);
        if (contrato == null) return;
        Inquilino inquilino = inquilinoRepository.findById(contrato.inquilinoId()).orElse(null);
        if (inquilino == null) return;

        enviarEmail(inquilino.email(), conta, enderecoImovel, tipoContaNome);
        contaAcessoRepository.findByInquilinoId(inquilino.id()).ifPresent(contaAcesso ->
            enviarPush(contaAcesso, tipoContaNome, enderecoImovel));
    }

    private void notificarProprietario(UUID proprietarioId, Conta conta, String enderecoImovel, String tipoContaNome) {
        Proprietario proprietario = proprietarioRepository.findById(proprietarioId).orElse(null);
        if (proprietario == null) return;

        enviarEmail(proprietario.email(), conta, enderecoImovel, tipoContaNome);
        contaAcessoRepository.findByProprietarioId(proprietario.id()).ifPresent(contaAcesso ->
            enviarPush(contaAcesso, tipoContaNome, enderecoImovel));
    }

    private void enviarEmail(Email destino, Conta conta, String enderecoImovel, String tipoContaNome) {
        emailSender.enviarAlertaConta(destino.value(), enderecoImovel, tipoContaNome, conta.vencimento(), conta.valor().valor());
    }

    private void enviarPush(ContaAcesso contaAcesso, String tipoContaNome, String enderecoImovel) {
        pushTokenRepository.findByContaAcessoId(contaAcesso.id()).forEach(pushToken ->
            pushSender.enviar(pushToken.token(), "Conta próxima do vencimento",
                tipoContaNome + " do imóvel " + enderecoImovel + " vence em breve."));
    }
}
