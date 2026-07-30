package br.com.imoveis.infrastructure.rest.dto;

import br.com.imoveis.domain.imovel.Imovel;
import br.com.imoveis.domain.imovel.TipoImovel;
import br.com.imoveis.domain.imovel.Unidade;
import br.com.imoveis.domain.imovel.UnidadeStatus;
import br.com.imoveis.domain.imovel.Visibilidade;
import io.micronaut.serde.annotation.Serdeable;

import java.util.List;
import java.util.UUID;

public final class ImovelDtos {
    private ImovelDtos() {}

    @Serdeable
    public record NovoImovelRequest(String endereco, String cidade, String matricula,
                                    String numero, String bairro, String complemento,
                                    TipoImovel tipoImovel) {}

    @Serdeable
    public record FiltroImoveisRequest(String busca, String cidade,
                                       UnidadeStatus status, TipoImovel tipoImovel) {}

    @Serdeable
    public record ImovelResponse(UUID id, UUID proprietarioId, String endereco, String cidade,
                                  String matricula, String numero, String bairro,
                                  String complemento, TipoImovel tipoImovel,
                                  String enderecoCompleto,
                                  Visibilidade visibilidade, List<UnidadeResumo> unidades) {
        public static ImovelResponse from(Imovel i) {
            return new ImovelResponse(i.id(), i.proprietarioId(), i.endereco(), i.cidade(),
                i.matricula(), i.numero(), i.bairro(), i.complemento(), i.tipoImovel(),
                i.enderecoCompleto(), i.visibilidade(),
                i.unidades().stream().map(UnidadeResumo::from).toList());
        }
    }

    @Serdeable
    public record UnidadeResumo(UUID id, String nome, boolean padrao, UnidadeStatus status) {
        public static UnidadeResumo from(Unidade u) {
            return new UnidadeResumo(u.id(), u.nome(), u.padrao(), u.status());
        }
    }
}
