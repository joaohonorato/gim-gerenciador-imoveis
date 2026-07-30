package br.com.imoveis.infrastructure.persistence.jpa;

import br.com.imoveis.domain.auth.TipoContaAcesso;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "convites_acesso")
public class ConviteAcessoJpaEntity {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true, length = 64)
    private String token;

    @Column(nullable = false)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoContaAcesso tipo;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false)
    private String documento;

    @Column(name = "perfil_proprietario")
    private String perfilProprietario;

    @Column(name = "expira_em", nullable = false)
    private Instant expiraEm;

    @Column(nullable = false)
    private boolean consumido;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public TipoContaAcesso getTipo() { return tipo; }
    public void setTipo(TipoContaAcesso tipo) { this.tipo = tipo; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getDocumento() { return documento; }
    public void setDocumento(String documento) { this.documento = documento; }

    public String getPerfilProprietario() { return perfilProprietario; }
    public void setPerfilProprietario(String perfilProprietario) { this.perfilProprietario = perfilProprietario; }

    public Instant getExpiraEm() { return expiraEm; }
    public void setExpiraEm(Instant expiraEm) { this.expiraEm = expiraEm; }

    public boolean isConsumido() { return consumido; }
    public void setConsumido(boolean consumido) { this.consumido = consumido; }

    public Instant getCriadoEm() { return criadoEm; }
    public void setCriadoEm(Instant criadoEm) { this.criadoEm = criadoEm; }
}