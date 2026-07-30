package br.com.imoveis.domain.imovel;

public class TransicaoInvalidaException extends RuntimeException {
    public TransicaoInvalidaException(String message) { super(message); }
}
