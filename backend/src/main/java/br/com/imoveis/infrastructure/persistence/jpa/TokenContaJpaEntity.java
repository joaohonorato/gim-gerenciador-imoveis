package br.com.imoveis.infrastructure.persistence.jpa;

import br.com.imoveis.domain.auth.TokenContaFinalidade;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tokens_conta")
public class TokenContaJpaEntity {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true, length = 64)
    private String token;

    @Column(nullable = false)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TokenContaFinalidade finalidade;

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

    public TokenContaFinalidade getFinalidade() { return finalidade; }
    public void setFinalidade(TokenContaFinalidade finalidade) { this.finalidade = finalidade; }

    public Instant getExpiraEm() { return expiraEm; }
    public void setExpiraEm(Instant expiraEm) { this.expiraEm = expiraEm; }

    public boolean isConsumido() { return consumido; }
    public void setConsumido(boolean consumido) { this.consumido = consumido; }

    public Instant getCriadoEm() { return criadoEm; }
    public void setCriadoEm(Instant criadoEm) { this.criadoEm = criadoEm; }
}
