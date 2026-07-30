package br.com.imoveis.application.exception;

public class ConflitoException extends RuntimeException {
    public ConflitoException(String message) {
        super(message);
    }
}
