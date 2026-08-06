package br.com.imoveis.infrastructure.scheduling;

import br.com.imoveis.application.usecase.NotificarContasProximasDoVencimento;
import br.com.imoveis.application.usecase.NotificarContratosProximosDoVencimento;
import br.com.imoveis.application.usecase.NotificarGarantiasProximasDoVencimento;
import io.micronaut.scheduling.annotation.Scheduled;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Único job agendado do backend hoje. Roda uma vez por dia, cedo o
// suficiente pra qualquer alerta gerado já valer pro resto do dia útil dos
// destinatários (horário do servidor). Cada notificação usa a própria flag
// de "já enviado" pra decidir o que precisa de alerta (ver
// docs/especificacao-produto.md §4 e §3), então rodar de novo no mesmo dia
// (redeploy, restart) é seguro — não duplica envio.
@Singleton
public class AlertasVencimentoScheduler {

    private static final Logger log = LoggerFactory.getLogger(AlertasVencimentoScheduler.class);

    private final NotificarContratosProximosDoVencimento notificarContratos;
    private final NotificarGarantiasProximasDoVencimento notificarGarantias;
    private final NotificarContasProximasDoVencimento notificarContas;

    public AlertasVencimentoScheduler(NotificarContratosProximosDoVencimento notificarContratos,
                                       NotificarGarantiasProximasDoVencimento notificarGarantias,
                                       NotificarContasProximasDoVencimento notificarContas) {
        this.notificarContratos = notificarContratos;
        this.notificarGarantias = notificarGarantias;
        this.notificarContas = notificarContas;
    }

    @Scheduled(cron = "0 0 8 * * *")
    void executar() {
        executarComLog("contratos", notificarContratos::execute);
        executarComLog("garantias", notificarGarantias::execute);
        executarComLog("contas", notificarContas::execute);
    }

    private void executarComLog(String nome, Runnable acao) {
        try {
            acao.run();
        } catch (Exception e) {
            // Cada use case já isola falha por item individualmente; este
            // catch é só a última rede de segurança pra um dos três tipos de
            // alerta não travar os outros dois se algo escapar (ex.: erro de
            // conexão com o banco na consulta inicial).
            log.error("Falha ao rodar alerta de vencimento de {}", nome, e);
        }
    }
}
