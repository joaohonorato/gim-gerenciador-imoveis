package br.com.imoveis.domain.inquilino;

import br.com.imoveis.domain.shared.Cpf;
import br.com.imoveis.domain.shared.Email;

import java.time.Instant;
import java.util.UUID;

public class Inquilino {

    private final UUID id;
    private final String nome;
    private final Cpf cpf;
    private final Email email;
    private final Instant criadoEm;

    private Inquilino(UUID id, String nome, Cpf cpf, Email email, Instant criadoEm) {
        this.id = id;
        this.nome = nome;
        this.cpf = cpf;
        this.email = email;
        this.criadoEm = criadoEm;
    }

    public static Inquilino cadastrar(String nome, Cpf cpf, Email email, Instant agora) {
        if (nome == null || nome.isBlank()) throw new IllegalArgumentException("nome obrigatório");
        return new Inquilino(UUID.randomUUID(), nome.trim(), cpf, email, agora);
    }

    public static Inquilino reconstituir(UUID id, String nome, Cpf cpf, Email email, Instant criadoEm) {
        return new Inquilino(id, nome, cpf, email, criadoEm);
    }

    public UUID id() { return id; }
    public String nome() { return nome; }
    public Cpf cpf() { return cpf; }
    public Email email() { return email; }
    public Instant criadoEm() { return criadoEm; }
}
