package br.com.imoveis.infrastructure.rest.dto;

import br.com.imoveis.domain.inquilino.Inquilino;
import io.micronaut.serde.annotation.Serdeable;

import java.time.Instant;
import java.util.UUID;

public final class InquilinoDtos {
    private InquilinoDtos() {}

    @Serdeable
    public record InquilinoResponse(UUID id, String nome, String cpf, String email, Instant criadoEm) {
        public static InquilinoResponse from(Inquilino i) {
            return new InquilinoResponse(
                i.id(),
                i.nome(),
                i.cpf() == null ? null : i.cpf().value(),
                i.email() == null ? null : i.email().value(),
                i.criadoEm());
        }
    }
}
