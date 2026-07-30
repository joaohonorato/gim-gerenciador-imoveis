package br.com.imoveis.infrastructure.rest.dto;

import br.com.imoveis.domain.auth.TipoContaAcesso;
import br.com.imoveis.domain.proprietario.PerfilProprietario;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public final class AuthDtos {
    private AuthDtos() {}

    @Serdeable
    public record LoginRequest(
        @NotBlank(message = "email obrigatório")
        @Email(message = "email inválido")
        String email,
        @NotBlank(message = "senha obrigatória")
        @Size(min = 8, max = 72, message = "senha deve ter entre 8 e 72 caracteres")
        String senha) {}

    @Serdeable
    public record RegistrarProprietarioRequest(
        @NotBlank(message = "email obrigatório")
        @Email(message = "email inválido")
        String email,
        @NotBlank(message = "senha obrigatória")
        @Size(min = 8, max = 72, message = "senha deve ter entre 8 e 72 caracteres")
        String senha,
        @NotBlank(message = "username obrigatório")
        @Size(min = 3, max = 120, message = "username deve ter entre 3 e 120 caracteres")
        String username,
        @NotBlank(message = "cpf/cnpj obrigatório")
        @Pattern(
            regexp = "^(?:\\d{11}|\\d{14}|\\d{3}\\.\\d{3}\\.\\d{3}-\\d{2}|\\d{2}\\.\\d{3}\\.\\d{3}/\\d{4}-\\d{2})$",
            message = "cpf/cnpj inválido")
        String cpfCnpj) {}

    @Serdeable
    public record LoginResponse(String sessionToken, UUID proprietarioId, String nome, String email,
                                TipoContaAcesso tipoConta) {}

    @Serdeable
    public record NovoConviteProprietarioRequest(
        @NotBlank(message = "email obrigatório")
        @Email(message = "email inválido")
        String email,
        @NotBlank(message = "nome obrigatório")
        @Size(min = 3, max = 120, message = "nome deve ter entre 3 e 120 caracteres")
        String nome,
        @NotBlank(message = "cpf/cnpj obrigatório")
        @Pattern(
            regexp = "^(?:\\d{11}|\\d{14}|\\d{3}\\.\\d{3}\\.\\d{3}-\\d{2}|\\d{2}\\.\\d{3}\\.\\d{3}/\\d{4}-\\d{2})$",
            message = "cpf/cnpj inválido")
        String cpfCnpj,
        PerfilProprietario perfil) {}

    @Serdeable
    public record ConviteAcessoResponse(String token, String email, TipoContaAcesso tipoConta,
                                        String nome, String documento, PerfilProprietario perfil,
                                        boolean consumido) {}

    @Serdeable
    public record AceitarConviteRequest(
        @NotBlank(message = "email obrigatório")
        @Email(message = "email inválido")
        String email,
        @NotBlank(message = "senha obrigatória")
        @Size(min = 8, max = 72, message = "senha deve ter entre 8 e 72 caracteres")
        String senha) {}

    @Serdeable
    public record MeResponse(UUID id, String nome, String email, TipoContaAcesso tipoConta) {}
}
