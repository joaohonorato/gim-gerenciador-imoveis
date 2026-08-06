package br.com.imoveis.application.exception;

public class CadastroIncompletoException extends RuntimeException {
    public CadastroIncompletoException(String message) {
        super(message);
    }
}
