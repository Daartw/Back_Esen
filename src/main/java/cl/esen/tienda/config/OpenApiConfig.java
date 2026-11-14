package cl.esen.tienda.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Documentacion Swagger con el boton "Authorize" para pegar el token JWT. */
@Configuration
public class OpenApiConfig {

    public static final String ESQUEMA_JWT = "bearerAuth";

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("ESEN Tienda API")
                        .version("v1")
                        .description("""
                                API REST de la tienda online ESEN (proyecto académico DSY1104).
                                Para probar rutas privadas: ejecuta POST /api/v1/auth/login, copia el accessToken \
                                y pégalo en el botón Authorize."""))
                .components(new Components().addSecuritySchemes(ESQUEMA_JWT,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
