package br.com.imoveis.infrastructure.persistence.jpa;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "proprietarios")
public class ProprietarioJpaEntity {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String nome;

    @Column(name = "cpf_cnpj", unique = true)
    private String cpfCnpj;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String perfil;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;

    @Column
    private String telefone;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getCpfCnpj() { return cpfCnpj; }
    public void setCpfCnpj(String cpfCnpj) { this.cpfCnpj = cpfCnpj; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPerfil() { return perfil; }
    public void setPerfil(String perfil) { this.perfil = perfil; }

    public Instant getCriadoEm() { return criadoEm; }
    public void setCriadoEm(Instant criadoEm) { this.criadoEm = criadoEm; }

    public String getTelefone() { return telefone; }
    public void setTelefone(String telefone) { this.telefone = telefone; }
}
