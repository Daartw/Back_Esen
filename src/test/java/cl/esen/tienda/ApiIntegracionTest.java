package cl.esen.tienda;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.greaterThan;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas de integracion: levantan la aplicacion completa contra una base H2 en memoria
 * y recorren el API como lo haria el frontend (login, token, permisos por rol y compra).
 */
@SpringBootTest
@AutoConfigureMockMvc
class ApiIntegracionTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper json;

    private JsonNode login(String email, String clave) throws Exception {
        MvcResult resultado = mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + clave + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return json.readTree(resultado.getResponse().getContentAsString());
    }

    private String bearer(String email, String clave) throws Exception {
        return "Bearer " + login(email, clave).get("accessToken").asText();
    }

    @Test
    void elCatalogoEsPublico() throws Exception {
        mvc.perform(get("/api/v1/productos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", greaterThan(0)));
        mvc.perform(get("/api/v1/categorias")).andExpect(status().isOk());
    }

    @Test
    void loginConClaveIncorrectaDevuelve401() throws Exception {
        mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@esen.cl\",\"password\":\"incorrecta\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void sinTokenLasRutasPrivadasDevuelven401() throws Exception {
        mvc.perform(get("/api/v1/usuarios")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/boletas")).andExpect(status().isUnauthorized());
    }

    @Test
    void elRefreshTokenRenuevaLaSesionYNoSirveComoAccessToken() throws Exception {
        String refresh = login("cliente@esen.cl", "Cliente123*").get("refreshToken").asText();

        mvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refresh + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());

        mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + refresh))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void elClienteSoloAccedeALaTienda() throws Exception {
        String cliente = bearer("cliente@esen.cl", "Cliente123*");

        mvc.perform(get("/api/v1/auth/me").header("Authorization", cliente))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rol").value("CLIENTE"));
        mvc.perform(get("/api/v1/usuarios").header("Authorization", cliente)).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/boletas").header("Authorization", cliente)).andExpect(status().isForbidden());
        mvc.perform(delete("/api/v1/productos/1").header("Authorization", cliente)).andExpect(status().isForbidden());
    }

    @Test
    void elVendedorSoloLeeProductosYOrdenes() throws Exception {
        String vendedor = bearer("vendedor@esen.cl", "Vendedor123*");

        mvc.perform(get("/api/v1/productos").header("Authorization", vendedor)).andExpect(status().isOk());
        mvc.perform(get("/api/v1/boletas").header("Authorization", vendedor)).andExpect(status().isOk());

        mvc.perform(get("/api/v1/usuarios").header("Authorization", vendedor)).andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/categorias").header("Authorization", vendedor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Accesorios\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/boletas").header("Authorization", vendedor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"direccionEnvio\":\"Calle 1\",\"items\":[{\"productoId\":1,\"cantidad\":1}]}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void elAdministradorHaceElCrudCompletoDeCategorias() throws Exception {
        String admin = bearer("admin@esen.cl", "Admin123*");

        MvcResult creada = mvc.perform(post("/api/v1/categorias").header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Accesorios\",\"descripcion\":\"Gorros y bolsos\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        long id = json.readTree(creada.getResponse().getContentAsString()).get("id").asLong();

        mvc.perform(put("/api/v1/categorias/" + id).header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Accesorios y gorros\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Accesorios y gorros"));
        mvc.perform(get("/api/v1/categorias/" + id)).andExpect(status().isOk());
        mvc.perform(delete("/api/v1/categorias/" + id).header("Authorization", admin))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/categorias/" + id)).andExpect(status().isNotFound());
    }

    @Test
    void validaLosDatosDeEntrada() throws Exception {
        String admin = bearer("admin@esen.cl", "Admin123*");

        mvc.perform(post("/api/v1/productos").header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"\",\"precio\":-5,\"stock\":1,\"categoriaId\":1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.nombre").exists())
                .andExpect(jsonPath("$.campos.precio").exists());
    }

    @Test
    void laCompraDescuentaStockYQuedaEnMisCompras() throws Exception {
        String cliente = bearer("cliente@esen.cl", "Cliente123*");
        int stockAntes = json.readTree(mvc.perform(get("/api/v1/productos/9")).andReturn()
                .getResponse().getContentAsString()).get("stock").asInt();

        mvc.perform(post("/api/v1/boletas").header("Authorization", cliente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"direccionEnvio\":\"Los Leones 220\",\"items\":[{\"productoId\":9,\"cantidad\":2}]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.total").value(39980))
                .andExpect(jsonPath("$.estado").value("PAGADA"))
                .andExpect(jsonPath("$.detalles.length()").value(1));

        mvc.perform(get("/api/v1/productos/9")).andExpect(jsonPath("$.stock").value(stockAntes - 2));
        mvc.perform(get("/api/v1/boletas/mias").header("Authorization", cliente))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", greaterThan(0)));
    }

    @Test
    void laCompraSinStockDevuelve400() throws Exception {
        String cliente = bearer("cliente@esen.cl", "Cliente123*");

        // El producto 8 (Sweater Tejido Arena) se carga con stock 0.
        mvc.perform(post("/api/v1/boletas").header("Authorization", cliente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"direccionEnvio\":\"Los Leones 220\",\"items\":[{\"productoId\":8,\"cantidad\":1}]}"))
                .andExpect(status().isBadRequest());
    }
}
