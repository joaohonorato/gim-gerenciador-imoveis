package br.com.imoveis.infrastructure.rest.dto;

import io.micronaut.serde.annotation.Serdeable;

@Serdeable
public record ErrorResponse(String code, String message) {}
