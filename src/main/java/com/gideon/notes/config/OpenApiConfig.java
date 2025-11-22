package com.gideon.notes.config;


import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;

@OpenAPIDefinition(
        info = @Info(
                title = "Notes Api Docs",
                description = "Backend Api docs for Notes",
                version = "1.0"
        ),
        servers = {
                @Server(
                        description = "LOCAL ENV",
                        url = "http://localhost:8082"
                ),

                @Server(
                        description = "PROD ENV",
                        url = "https://notesbackend.onrender.com"
                )

        }
)

@SecurityScheme(
        name = "bearerAuth",
        scheme = "bearer",
        type = SecuritySchemeType.HTTP,
        bearerFormat = "JWT",
        in = SecuritySchemeIn.HEADER
)
public class OpenApiConfig {
}
