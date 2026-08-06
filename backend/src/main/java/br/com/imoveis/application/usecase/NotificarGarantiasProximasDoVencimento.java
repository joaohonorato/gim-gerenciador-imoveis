package br.com.imoveis.application.usecase;

import br.com.imoveis.application.ports.AlertaVencimentoEmailSender;
import br.com.imoveis.application.ports.Clock;
import br.com.imoveis.application.ports.ContaAcessoRepository;
import br.com.imoveis.application.ports.ContratoRepository;
import br.com.imoveis.application.ports.ImovelRepository;
import br.com.imoveis.application.ports.ProprietarioRepository;
import br.com.imoveis.application.ports.PushNotificationSender;
import br.com.imoveis.application.ports.PushTokenRepository;
import br.com.imoveis.domain.auth.ContaAcesso;
import br.com.imoveis.domain.contrato.Contrato;
import br.com.imoveis.domain.imovel.Imovel;
import br.com.imoveis.domain.proprietario.Proprietario;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.List;

// Avisa o proprietário quando a garantia de um contrato está a 30 dias ou
// menos do vencimento (docs/especificacao-produto.md §4: "agenda alerta de
// vencimento da GARANTIA (30 dias antes)"). A garantia protege o
// proprietário contra inadimplência, por isso o destinatário é sempre ele,
// nunca o inquilino — diferente do alerta de CONTA (ver
// NotificarContasProximasDoVencimento), cujo destinatário depende de quem é
// o responsável pela despesa.
@Singleton
public class NotificarGarantiasProximasDoVencimento {

    private static final Logger log = LoggerFactory.getLogger(NotificarGarantiasProximasDoVencimento.class);
    private static final int DIAS_DE_ANTECEDENCIA = 30;

    private final ContratoRepository contratoRepository;
    private final ImovelRepository imovelRepository;
    private final ProprietarioRepository proprietarioRepository;
    private final ContaAcessoRepository contaAcessoRepository;
    private final PushTokenRepository pushTokenRepository;
    private final AlertaVencimentoEmailSender emailSender;
    private final PushNotificationSender pushSender;
    private final Clock clock;

    public NotificarGarantiasProximasDoVencimento(ContratoRepository contratoRepository, ImovelRepository imovelRepository,
                                                    ProprietarioRepository proprietarioRepository,
                                                    ContaAcessoRepository contaAcessoRepository,
                                                    PushTokenRepository pushTokenRepository,
                                                    AlertaVencimentoEmailSender emailSender,
                                                    PushNotificationSender pushSender, Clock clock) {
        this.contratoRepository = contratoRepository;
        this.imovelRepository = imovelRepository;
        this.proprietarioRepository = proprietarioRepository;
        this.contaAcessoRepository = contaAcessoRepository;
        this.pushTokenRepository = pushTokenRepository;
        this.emailSender = emailSender;
        this.pushSender = pushSender;
        this.clock = clock;
    }

    public void execute() {
        LocalDate hoje = clock.today();
        List<Contrato> candidatos = contratoRepository.findParaAlertaGarantia(hoje, hoje.plusDays(DIAS_DE_ANTECEDENCIA));
        for (Contrato contrato : candidatos) {
            try {
                notificar(contrato);
                contrato.marcarAlertaGarantiaEnviado();
                contratoRepository.save(contrato);
            } catch (Exception e) {
                log.warn("Falha ao notificar vencimento da garantia do contrato {}", contrato.id(), e);
            }
        }
    }

    private void notificar(Contrato contrato) {
        Proprietario proprietario = proprietarioRepository.findById(contrato.proprietarioId()).orElse(null);
        if (proprietario == null || contrato.garantia() == null) return;
        String enderecoImovel = enderecoDaUnidade(contrato.unidadeId());

        emailSender.enviarAlertaGarantia(proprietario.email().value(), enderecoImovel, contrato.garantia().vencimento());

        contaAcessoRepository.findByProprietarioId(proprietario.id()).ifPresent(conta ->
            enviarPush(conta, "Garantia próxima do vencimento",
                "A garantia do contrato do imóvel " + enderecoImovel + " vence em breve."));
    }

    private String enderecoDaUnidade(java.util.UUID unidadeId) {
        return imovelRepository.findUnidadeById(unidadeId)
            .flatMap(unidade -> imovelRepository.findById(unidade.imovelId()))
            .map(Imovel::enderecoCompleto)
            .orElse("seu imóvel");
    }

    private void enviarPush(ContaAcesso contaAcesso, String titulo, String corpo) {
        pushTokenRepository.findByContaAcessoId(contaAcesso.id())
            .forEach(pushToken -> pushSender.enviar(pushToken.token(), titulo, corpo));
    }
}
