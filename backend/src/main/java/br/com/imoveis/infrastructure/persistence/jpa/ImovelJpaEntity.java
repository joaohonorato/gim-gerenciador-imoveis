package br.com.imoveis.infrastructure.persistence.jpa;

import br.com.imoveis.domain.imovel.TipoImovel;
import br.com.imoveis.domain.imovel.Visibilidade;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
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

    @Column
    private Integer quartos;

    @Column
    private Integer banheiros;

    @Column
    private Integer vagas;

    @Column(name = "area_m2")
    private BigDecimal areaM2;

    @Column
    private BigDecimal iptu;

    @Column
    private String cep;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;

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

    public Integer getQuartos() { return quartos; }
    public void setQuartos(Integer quartos) { this.quartos = quartos; }

    public Integer getBanheiros() { return banheiros; }
    public void setBanheiros(Integer banheiros) { this.banheiros = banheiros; }

    public Integer getVagas() { return vagas; }
    public void setVagas(Integer vagas) { this.vagas = vagas; }

    public BigDecimal getAreaM2() { return areaM2; }
    public void setAreaM2(BigDecimal areaM2) { this.areaM2 = areaM2; }

    public BigDecimal getIptu() { return iptu; }
    public void setIptu(BigDecimal iptu) { this.iptu = iptu; }

    public String getCep() { return cep; }
    public void setCep(String cep) { this.cep = cep; }

    public Instant getCriadoEm() { return criadoEm; }
    public void setCriadoEm(Instant criadoEm) { this.criadoEm = criadoEm; }
}
