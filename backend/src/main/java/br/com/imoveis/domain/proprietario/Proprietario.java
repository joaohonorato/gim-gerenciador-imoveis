package br.com.imoveis.domain.proprietario;

import br.com.imoveis.domain.shared.CpfCnpj;
import br.com.imoveis.domain.shared.Email;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class Proprietario {

    private final UUID id;
    private final String nome;
    private final CpfCnpj cpfCnpj;
    private final Email email;
    private final PerfilProprietario perfil;
    private final Instant criadoEm;

    private Proprietario(UUID id, String nome, CpfCnpj cpfCnpj, Email email,
                          PerfilProprietario perfil, Instant criadoEm) {
        this.id = id;
        this.nome = nome;
        this.cpfCnpj = cpfCnpj;
        this.email = email;
        this.perfil = perfil;
        this.criadoEm = criadoEm;
    }

    public static Proprietario cadastrar(String nome, CpfCnpj cpfCnpj, Email email, Instant agora) {
        Objects.requireNonNull(nome, "nome obrigatório");
        if (nome.isBlank()) throw new IllegalArgumentException("nome obrigatório");
        Objects.requireNonNull(cpfCnpj, "cpfCnpj obrigatório");
        Objects.requireNonNull(email, "email obrigatório");
        return new Proprietario(UUID.randomUUID(), nome.trim(), cpfCnpj, email,
            PerfilProprietario.OWNER, agora);
    }

    public static Proprietario reconstituir(UUID id, String nome, CpfCnpj cpfCnpj, Email email,
                                             PerfilProprietario perfil, Instant criadoEm) {
        return new Proprietario(id, nome, cpfCnpj, email, perfil, criadoEm);
    }

    public UUID id() { return id; }
    public String nome() { return nome; }
    public CpfCnpj cpfCnpj() { return cpfCnpj; }
    public Email email() { return email; }
    public PerfilProprietario perfil() { return perfil; }
    public Instant criadoEm() { return criadoEm; }
}
