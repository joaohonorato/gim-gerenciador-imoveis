package br.com.imoveis.application.ports;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface AlertaVencimentoEmailSender {
    void enviarAlertaContrato(String emailDestino, String enderecoImovel, LocalDate dataFim);
    void enviarAlertaGarantia(String emailDestino, String enderecoImovel, LocalDate vencimento);
    void enviarAlertaConta(String emailDestino, String enderecoImovel, String tipoContaNome, LocalDate vencimento, BigDecimal valor);
}
