package br.com.imoveis.infrastructure.persistence.jpa;

import br.com.imoveis.domain.imovel.TipoImovel;
import br.com.imoveis.domain.imovel.Visibilidade;
import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "imoveis")
public class ImovelJpaEntity {

    @Id
    private UUID id;

    @Column(name = "proprietario_id", nullable = false)
    private UUID proprietarioId;

    @Column(nullable = false)
    private String endereco;

    @Column(nullable = false)
    private String cidade;

    @Column(nullable = false)
    private String matricula;

    @Column
    private String numero;

    @Column
    private String bairro;

    @Column
    private String complemento;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_imovel")
    private TipoImovel tipoImovel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Visibilidade visibilidade;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getProprietarioId() { return proprietarioId; }
    public void setProprietarioId(UUID proprietarioId) { this.proprietarioId = proprietarioId; }

    public String getEndereco() { return endereco; }
    public void setEndereco(String endereco) { this.endereco = endereco; }

    public String getCidade() { return cidade; }
    public void setCidade(String cidade) { this.cidade = cidade; }

    public String getMatricula() { return matricula; }
    public void setMatricula(String matricula) { this.matricula = matricula; }

    public String getNumero() { return numero; }
    public void setNumero(String numero) { this.numero = numero; }

    public String getBairro() { return bairro; }
    public void setBairro(String bairro) { this.bairro = bairro; }

    public String getComplemento() { return complemento; }
    public void setComplemento(String complemento) { this.complemento = complemento; }

    public TipoImovel getTipoImovel() { return tipoImovel; }
    public void setTipoImovel(TipoImovel tipoImovel) { this.tipoImovel = tipoImovel; }

    public Visibilidade getVisibilidade() { return visibilidade; }
    public void setVisibilidade(Visibilidade visibilidade) { this.visibilidade = visibilidade; }
}
