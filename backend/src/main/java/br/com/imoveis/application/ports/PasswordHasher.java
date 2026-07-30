package br.com.imoveis.application.ports;

public interface PasswordHasher {
    String hash(String senha);
    boolean matches(String senha, String senhaHash);
}