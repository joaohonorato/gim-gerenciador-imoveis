package br.com.imoveis.application.ports;

public interface TokenContaEmailSender {
    void enviarResetSenha(String emailDestino, String token);
    void enviarVerificacaoEmail(String emailDestino, String token);
    void enviarConfirmacaoAlteracaoEmail(String emailDestino, String token);
}
