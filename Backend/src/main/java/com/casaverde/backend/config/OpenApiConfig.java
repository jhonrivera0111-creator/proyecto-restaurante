package com.casaverde.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI casaVerdeOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Casa Verde API")
                        .version("0.0.1")
                        .description("API REST para operacion local del asadero Casa Verde: pedidos, caja, inventario, compras, empleados y configuracion.")
                        .contact(new Contact().name("Casa Verde"))
                        .license(new License().name("Uso interno")))
                .addServersItem(new Server()
                        .url("http://localhost:5020")
                        .description("Backend local"));
    }
}
