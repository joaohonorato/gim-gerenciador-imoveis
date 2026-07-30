package br.com.imoveis.application.exception;

public class AutenticacaoInvalidaException extends RuntimeException {
    public AutenticacaoInvalidaException(String message) {
        super(message);
    }
}
