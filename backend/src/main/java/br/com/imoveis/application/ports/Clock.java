package br.com.imoveis.application.ports;

import java.time.Instant;
import java.time.LocalDate;

public interface Clock {
    Instant now();
    LocalDate today();

    static Clock system() {
        return new Clock() {
            @Override public Instant now() { return Instant.now(); }
            @Override public LocalDate today() { return LocalDate.now(); }
        };
    }
}
