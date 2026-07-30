package br.com.imoveis.domain.shared;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

public record Periodo(LocalDate inicio, LocalDate fim) {

    public Periodo {
        Objects.requireNonNull(inicio, "inicio obrigatório");
        Objects.requireNonNull(fim, "fim obrigatório");
        if (fim.isBefore(inicio)) {
            throw new IllegalArgumentException("fim deve ser >= inicio");
        }
    }

    public boolean overlaps(Periodo other) {
        Objects.requireNonNull(other, "outro período obrigatório");
        return !this.fim.isBefore(other.inicio) && !other.fim.isBefore(this.inicio);
    }

    public boolean contains(LocalDate date) {
        return !date.isBefore(inicio) && !date.isAfter(fim);
    }

    public int meses() {
        long m = ChronoUnit.MONTHS.between(inicio, fim);
        return (int) Math.max(1, m);
    }
}
