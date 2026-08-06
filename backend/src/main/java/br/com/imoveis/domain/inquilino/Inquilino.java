package br.com.imoveis.domain.inquilino;

import br.com.imoveis.domain.shared.Cpf;
import br.com.imoveis.domain.shared.Email;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class Inquilino {

    private final UUID id;
    private String nome;
    private final Cpf cpf;
    private Email email;
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

    public void renomear(String novoNome) {
        if (novoNome == null || novoNome.isBlank()) {
            throw new IllegalArgumentException("nome obrigatório");
        }
        this.nome = novoNome.trim();
    }

    // Chamado só depois que ContaAcesso.confirmarAlteracaoEmail() já validou
    // a posse do novo e-mail — mantém o e-mail redundante aqui em sincronia
    // com o de ContaAcesso (fonte de verdade pra login).
    public void atualizarEmail(Email novoEmail) {
        this.email = Objects.requireNonNull(novoEmail, "email obrigatório");
    }

    public UUID id() { return id; }
    public String nome() { return nome; }
    public Cpf cpf() { return cpf; }
    public Email email() { return email; }
    public Instant criadoEm() { return criadoEm; }
}
