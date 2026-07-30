package br.com.imoveis.infrastructure.config;

import br.com.imoveis.application.ports.Clock;
import io.micronaut.context.annotation.Factory;
import jakarta.inject.Singleton;

@Factory
public class SystemClockBean {
    @Singleton
    public Clock clock() {
        return Clock.system();
    }
}
