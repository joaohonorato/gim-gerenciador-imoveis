package br.com.imoveis;

import io.micronaut.runtime.Micronaut;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;

@OpenAPIDefinition(
    info = @Info(
        title = "Gerenciador de Imoveis API",
        version = "0.1.0",
        description = "API para onboarding, gestao de imoveis e fluxo de contratos",
        license = @License(name = "Proprietary")
    )
)
public final class Application {
    private Application() {}

    public static void main(String[] args) {
        Micronaut.run(Application.class, args);
    }
}
