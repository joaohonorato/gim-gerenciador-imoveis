package br.com.imoveis.domain.shared;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public record Dinheiro(BigDecimal valor) {

    public Dinheiro {
        Objects.requireNonNull(valor, "valor obrigatório");
        if (valor.signum() < 0) {
            throw new IllegalArgumentException("dinheiro não pode ser negativo");
        }
        valor = valor.setScale(2, RoundingMode.HALF_UP);
    }

    public static Dinheiro of(String v) { return new Dinheiro(new BigDecimal(v)); }
    public static Dinheiro of(long v) { return new Dinheiro(BigDecimal.valueOf(v)); }
    public static Dinheiro zero() { return new Dinheiro(BigDecimal.ZERO); }
}
