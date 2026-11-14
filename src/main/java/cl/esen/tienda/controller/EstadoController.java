package cl.esen.tienda.controller;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.Map;

@RestController
@Tag(name = "Estado", description = "Comprobación de que el servicio está arriba")
public class EstadoController {

    @GetMapping("/api/v1/health")
    @Operation(summary = "Indica si el API está disponible")
    public Map<String, Object> health() {
        return Map.of("estado", "OK", "servicio", "esen-tienda-api", "fecha", LocalDateTime.now().toString());
    }

    /** La raiz lleva directo a la documentacion. */
    @Hidden
    @GetMapping("/")
    public ResponseEntity<Void> raiz() {
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create("/swagger-ui.html")).build();
    }
}
