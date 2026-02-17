package edu.eci.arsw.blueprints.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;

/**
 * Configuración de OpenAPI 3.0 para documentación automática de la API REST.
 * Accesible en: http://localhost:8081/swagger-ui.html
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI api() {
        return new OpenAPI()
                .info(new Info()
                        .title("ARSW Blueprints API")
                        .version("v1")
                        .description("REST API para gestión de planos (blueprints) con persistencia en PostgreSQL. " +
                                     "Laboratorio de Arquitecturas de Software - ECI.")
                        .contact(new Contact()
                                .name("Christian Alfonoso Romero Martinez"))
                        .license(new License()
                                .name("CC BY-NC 4.0")
                                .url("https://creativecommons.org/licenses/by-nc/4.0/")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8081")
                                .description("Servidor de desarrollo local")));
    }
}
