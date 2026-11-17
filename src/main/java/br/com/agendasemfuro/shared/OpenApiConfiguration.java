package br.com.agendasemfuro.shared;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfiguration {

    @Bean
    public OpenAPI agendaSemFuroOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Agenda sem Furo API")
                .description("API para cadastro de barbeiros e serviços, configuração de expediente, consulta de disponibilidade e gerenciamento de agendamentos.")
                .version("v1")
                .contact(new Contact().name("Equipe Agenda sem Furo")));
    }
}
