package br.com.imoveis.infrastructure.notificacao;

import br.com.imoveis.application.ports.AlertaVencimentoEmailSender;
import io.micronaut.context.annotation.Value;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Singleton
public class ResendAlertaVencimentoEmailSender implements AlertaVencimentoEmailSender {

    private static final Logger log = LoggerFactory.getLogger(ResendAlertaVencimentoEmailSender.class);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final HttpClient httpClient;
    private final String resendApiKey;
    private final String resendFromEmail;

    public ResendAlertaVencimentoEmailSender(
        @Value("${app.convites.resend.api-key:}") String resendApiKey,
        @Value("${app.convites.resend.from-email:no-reply@imoveis.local}") String resendFromEmail
    ) {
        this.httpClient = HttpClient.newBuilder().connectTimeout(REQUEST_TIMEOUT).build();
        this.resendApiKey = resendApiKey;
        this.resendFromEmail = resendFromEmail;
    }

    @Override
    public void enviarAlertaContrato(String emailDestino, String enderecoImovel, LocalDate dataFim) {
        String data = dataFim.format(DATA);
        enviar(emailDestino, "Contrato próximo do vencimento",
            "<p>O contrato do imóvel <b>" + escapeJson(enderecoImovel) + "</b> vence em " + data + ".</p>"
                + "<p>Vale já pensar em renovação ou reajuste.</p>",
            "O contrato do imóvel " + enderecoImovel + " vence em " + data + ". Vale já pensar em renovação ou reajuste.");
    }

    @Override
    public void enviarAlertaGarantia(String emailDestino, String enderecoImovel, LocalDate vencimento) {
        String data = vencimento.format(DATA);
        enviar(emailDestino, "Garantia próxima do vencimento",
            "<p>A garantia do contrato do imóvel <b>" + escapeJson(enderecoImovel) + "</b> vence em " + data + ".</p>",
            "A garantia do contrato do imóvel " + enderecoImovel + " vence em " + data + ".");
    }

    @Override
    public void enviarAlertaConta(String emailDestino, String enderecoImovel, String tipoContaNome, LocalDate vencimento, BigDecimal valor) {
        String data = vencimento.format(DATA);
        enviar(emailDestino, "Conta próxima do vencimento",
            "<p>A conta <b>" + escapeJson(tipoContaNome) + "</b> do imóvel <b>" + escapeJson(enderecoImovel)
                + "</b> (R$ " + valor + ") vence em " + data + ".</p>",
            tipoContaNome + " do imóvel " + enderecoImovel + " (R$ " + valor + ") vence em " + data + ".");
    }

    private void enviar(String emailDestino, String subject, String html, String text) {
        if (resendApiKey == null || resendApiKey.isBlank()) {
            log.warn("Resend não configurado; envio de alerta de vencimento foi pulado para {}", emailDestino);
            return;
        }
        if (emailDestino == null || emailDestino.isBlank()) {
            log.warn("Destinatário sem e-mail; envio de alerta de vencimento foi pulado");
            return;
        }

        String payload = "{" +
            "\"from\":\"" + escapeJson(resendFromEmail) + "\"," +
            "\"to\":[\"" + escapeJson(emailDestino) + "\"]," +
            "\"subject\":\"" + escapeJson(subject) + "\"," +
            "\"html\":\"" + escapeJson(html) + "\"," +
            "\"text\":\"" + escapeJson(text) + "\"" +
            "}";

        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create("https://api.resend.com/emails"))
                .header("Authorization", "Bearer " + resendApiKey)
                .header("Content-Type", "application/json")
                .timeout(REQUEST_TIMEOUT)
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.warn("Falha no envio Resend de alerta de vencimento. status={}, body={}", response.statusCode(), response.body());
            }
        } catch (Exception e) {
            log.warn("Erro ao enviar e-mail de alerta de vencimento", e);
        }
    }

    private static String escapeJson(String value) {
        return value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r");
    }
}
