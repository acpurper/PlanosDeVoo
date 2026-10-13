package br.pucrs.projarc.gestaoplanosvoo.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI openApi(@Value("${app.data-referencia}") String dataReferencia) {
        return new OpenAPI().info(new Info()
                .title("Sistema de Gestão de Planos de Voo")
                .version("0.1.0")
                .description("Backend do trabalho final de Projeto e Arquitetura de Software (PUCRS). "
                        + "Entrega parcial: UC02, UC03, UC06, UC07 e UC11. "
                        + "Data de referência dos dados iniciais: " + dataReferencia + "."));
    }
}
