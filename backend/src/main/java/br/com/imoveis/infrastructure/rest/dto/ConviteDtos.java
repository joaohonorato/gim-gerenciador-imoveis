package br.com.imoveis.infrastructure.rest.dto;

import br.com.imoveis.domain.contrato.GarantiaTipo;
import br.com.imoveis.domain.contrato.TipoContrato;
import br.com.imoveis.domain.contrato.ContratoAssinaturaStatus;
import br.com.imoveis.domain.convite.CanalEnvioConvite;
import br.com.imoveis.domain.convite.Candidatura;
import br.com.imoveis.domain.convite.CandidaturaStatus;
import br.com.imoveis.domain.convite.Convite;
import br.com.imoveis.domain.convite.ConviteStatus;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public final class ConviteDtos {
    private ConviteDtos() {}

    @Serdeable
    public record NovoConviteRequest(
        TipoContrato tipoContrato,
        BigDecimal valorAluguel,
        LocalDate dataInicio,
        LocalDate dataFim,
        GarantiaTipo garantiaAceita,
        @Email(message = "email do inquilino inválido")
        String emailInquilino,
        CanalEnvioConvite canalEnvio,
        String telefoneInquilino) {}

    @Serdeable
    public record ConviteResponse(UUID id, String token, UUID imovelId, UUID unidadeId,
                                   Instant expiraEm, TipoContrato tipoContrato,
                                   BigDecimal valorAluguel, LocalDate dataInicio, LocalDate dataFim,
                                   GarantiaTipo garantiaAceita, ConviteStatus status,
                                   EnvioConviteResponse envio) {
        public static ConviteResponse from(Convite c) {
            EnvioConviteResponse envioResponse = c.ultimoCanalEnvio() == null ? null : new EnvioConviteResponse(
                c.ultimoCanalEnvio(),
                c.ultimoStatusEnvio(),
                c.ultimoDestinoEnvio(),
                c.ultimoWhatsappShareUrl(),
                c.ultimoDetalheEnvio(),
                c.ultimoEnvioEm(),
                c.tentativasEnvio());
            return new ConviteResponse(c.id(), c.token(), c.imovelId(), c.unidadeId(), c.expiraEm(),
                c.condicoes().tipoContrato(), c.condicoes().valorAluguel().valor(),
                c.condicoes().periodoSugerido().inicio(), c.condicoes().periodoSugerido().fim(),
                c.condicoes().garantiaAceita(), c.status(), envioResponse);
        }
    }

    @Serdeable
    public record EnvioConviteResponse(
        CanalEnvioConvite canal,
        String status,
        String destino,
        String whatsappShareUrl,
        String detalhe,
        Instant enviadoEm,
        Integer tentativas) {}

    @Serdeable
    public record ReenviarConviteRequest(
        CanalEnvioConvite canalEnvio,
        @Email(message = "email do inquilino inválido")
        String emailInquilino,
        String telefoneInquilino) {}

    @Serdeable
    public record CadastroInquilinoRequest(
        @NotBlank(message = "username obrigatório")
        @Size(min = 3, max = 120, message = "username deve ter entre 3 e 120 caracteres")
        String username,
        @NotBlank(message = "cpf obrigatório")
        String cpf,
        @NotBlank(message = "email obrigatório")
        @Email(message = "email inválido")
        String email,
        @NotBlank(message = "senha obrigatória")
        @Size(min = 8, max = 72, message = "senha deve ter entre 8 e 72 caracteres")
        String senha) {}

    @Serdeable
    public record CandidaturaResponse(UUID id, UUID inquilinoId, CandidaturaStatus status,
                                       GarantiaTipo garantiaEscolhida) {
        public static CandidaturaResponse from(Candidatura c) {
            return new CandidaturaResponse(c.id(), c.inquilinoId(), c.status(), c.garantiaEscolhida());
        }
    }

    @Serdeable
    public record GarantiaRequest(GarantiaTipo tipo, String dadosEspecificos) {}

    @Serdeable
    public record CandidaturaPendenteResponse(
        UUID id,
        UUID imovelId,
        UUID unidadeId,
        UUID inquilinoId,
        String inquilinoNome,
        String inquilinoEmail,
        TipoContrato tipoContrato,
        BigDecimal valorAluguel,
        LocalDate dataInicio,
        LocalDate dataFim,
        GarantiaTipo garantiaAceita,
        GarantiaTipo garantiaEscolhida,
        Instant criadaEm) {}

    @Serdeable
    public record ConviteInquilinoResponse(
        UUID conviteId,
        String token,
        UUID imovelId,
        UUID unidadeId,
        UUID proprietarioId,
        ConviteStatus conviteStatus,
        UUID candidaturaId,
        CandidaturaStatus candidaturaStatus,
        TipoContrato tipoContrato,
        BigDecimal valorAluguel,
        LocalDate dataInicio,
        LocalDate dataFim,
        GarantiaTipo garantiaAceita,
        GarantiaTipo garantiaEscolhida,
        Instant criadaEm,
        UUID contratoId,
        ContratoAssinaturaStatus statusAssinaturaContrato) {}
}
