package br.com.imoveis.domain.proprietario;

import br.com.imoveis.domain.shared.CpfCnpj;
import br.com.imoveis.domain.shared.Email;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class Proprietario {

    private final UUID id;
    private String nome;
    private CpfCnpj cpfCnpj;
    private Email email;
    private final PerfilProprietario perfil;
    private final Instant criadoEm;
    private String telefone;

    private Proprietario(UUID id, String nome, CpfCnpj cpfCnpj, Email email,
                          PerfilProprietario perfil, Instant criadoEm, String telefone) {
        this.id = id;
        this.nome = nome;
        this.cpfCnpj = cpfCnpj;
        this.email = email;
        this.perfil = perfil;
        this.criadoEm = criadoEm;
        this.telefone = telefone;
    }

    /**
     * cpfCnpj e telefone são opcionais (cadastro básico) — ver
     * {@link #completarCadastro(CpfCnpj)} e {@link #definirTelefone(String)}.
     */
    public static Proprietario cadastrar(String nome, CpfCnpj cpfCnpj, Email email, Instant agora) {
        Objects.requireNonNull(nome, "nome obrigatório");
        if (nome.isBlank()) throw new IllegalArgumentException("nome obrigatório");
        Objects.requireNonNull(email, "email obrigatório");
        return new Proprietario(UUID.randomUUID(), nome.trim(), cpfCnpj, email,
            PerfilProprietario.OWNER, agora, null);
    }

    public static Proprietario reconstituir(UUID id, String nome, CpfCnpj cpfCnpj, Email email,
                                             PerfilProprietario perfil, Instant criadoEm, String telefone) {
        return new Proprietario(id, nome, cpfCnpj, email, perfil, criadoEm, telefone);
    }

    public void completarCadastro(CpfCnpj cpfCnpj) {
        this.cpfCnpj = Objects.requireNonNull(cpfCnpj, "cpfCnpj obrigatório");
    }

    public void definirTelefone(String telefone) {
        if (telefone == null || telefone.isBlank()) {
            throw new IllegalArgumentException("telefone obrigatório");
        }
        this.telefone = telefone.trim();
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

    public boolean cadastroCompleto() {
        return cpfCnpj != null;
    }

    public UUID id() { return id; }
    public String nome() { return nome; }
    public CpfCnpj cpfCnpj() { return cpfCnpj; }
    public Email email() { return email; }
    public PerfilProprietario perfil() { return perfil; }
    public Instant criadoEm() { return criadoEm; }
    public String telefone() { return telefone; }
}
