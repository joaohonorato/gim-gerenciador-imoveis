package br.com.imoveis.domain.shared;

import java.util.Objects;
import java.util.regex.Pattern;

public record Email(String value) {

    private static final Pattern PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    public Email {
        Objects.requireNonNull(value, "email obrigatório");
        String normalized = value.trim().toLowerCase();
        if (!PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException("email inválido: " + value);
        }
        value = normalized;
    }
}
