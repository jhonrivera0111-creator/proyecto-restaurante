package com.casaverde.backend.seguridad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import com.casaverde.backend.capaAccesoADatos.models.entitys.Usuario;
import com.casaverde.backend.capaAccesoADatos.repositories.UsuarioRepository;
import com.casaverde.backend.config.security.AuthTokenService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Pruebas de SEGURIDAD con la autenticación ACTIVADA (como en producción).
 *
 * El resto de pruebas de integración corren con app.security.enabled=false para
 * concentrarse en las reglas de negocio; esta clase cubre ese hueco y verifica
 * que la API de verdad rechaza a quien no debe entrar:
 *  - sin token, con token inventado, alterado o vencido -> 401
 *  - login con clave incorrecta -> rechazado y sin token
 *  - OPERADOR no puede entrar a rutas solo-ADMIN (configuración, usuarios, backups) -> 403
 *  - ADMIN sí puede; las rutas públicas (login, lista de usuarios, health) siguen abiertas
 *
 * Usa su propia base SQLite temporal para no interferir con las otras pruebas.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "app.security.enabled=true",
        "app.datasource.path=./target/test-data/casaverde-seguridad-test.db"
})
class SeguridadEndpointsTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private AuthTokenService tokenService;

    @Autowired
    private UsuarioRepository usuarioRepo;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${app.security.token-secret}")
    private String tokenSecret;

    // ---------- helpers ----------

    private Usuario admin() {
        return usuarioRepo.findAllByOrderByNombreAsc().stream()
                .filter(u -> "ADMIN".equals(u.getRol()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("DataInitializer debe sembrar un ADMIN"));
    }

    /** Usuario OPERADOR ficticio solo para firmar un token (la validación del token no consulta la BD). */
    private Usuario operadorFicticio() {
        Usuario u = new Usuario();
        u.setId(999L);
        u.setNombre("Operador Prueba");
        u.setRol("OPERADOR");
        return u;
    }

    private String bearer(Usuario u) {
        return "Bearer " + tokenService.generar(u);
    }

    private String loginJson(Long usuarioId, String password) throws Exception {
        return objectMapper.writeValueAsString(Map.of("usuarioId", usuarioId, "password", password));
    }

    // ---------- acceso sin credenciales ----------

    @Test
    void sinToken_rutaProtegida_responde401() throws Exception {
        mvc.perform(get("/api/v1/pedidos"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenInventado_responde401() throws Exception {
        mvc.perform(get("/api/v1/pedidos").header("Authorization", "Bearer esto-no-es-un-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenAlterado_paraSubirseARol_responde401() throws Exception {
        // Se toma un token válido de OPERADOR y se cambia el payload para decir ADMIN
        // sin poder recalcular la firma: el servidor debe rechazarlo.
        String token = tokenService.generar(operadorFicticio());
        String[] partes = token.split("\\.", 2);
        String payloadFalso = java.util.Base64.getUrlEncoder().withoutPadding()
                .encodeToString("999:ADMIN:9999999999:Operador Prueba".getBytes());
        String tokenFalso = payloadFalso + "." + partes[1];

        mvc.perform(get("/api/v1/configuracion").header("Authorization", "Bearer " + tokenFalso))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenVencido_responde401() throws Exception {
        // Mismo secreto, pero con vigencia negativa: el token nace vencido.
        AuthTokenService emisorVencido = new AuthTokenService();
        ReflectionTestUtils.setField(emisorVencido, "secret", tokenSecret);
        ReflectionTestUtils.setField(emisorVencido, "expirationHours", -1L);
        String tokenVencido = emisorVencido.generar(admin());

        mvc.perform(get("/api/v1/pedidos").header("Authorization", "Bearer " + tokenVencido))
                .andExpect(status().isUnauthorized());
    }

    // ---------- login ----------

    @Test
    void loginAdmin_conClaveCorrecta_entregaTokenQueAbreLaApi() throws Exception {
        String respuesta = mvc.perform(post("/api/v1/sesion/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(admin().getId(), "admin123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rol").value("ADMIN"))
                .andReturn().getResponse().getContentAsString();

        JsonNode json = objectMapper.readTree(respuesta);
        String token = json.get("token").asText();
        assertThat(token).isNotBlank();

        mvc.perform(get("/api/v1/pedidos").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void loginAdmin_conClaveIncorrecta_esRechazadoYNoEntregaToken() throws Exception {
        mvc.perform(post("/api/v1/sesion/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(admin().getId(), "clave-equivocada")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.token").doesNotExist())
                .andExpect(jsonPath("$.mensaje").value("Clave de administrador incorrecta"));
    }

    @Test
    void respuestaDeLogin_noExponeElHashDeLaClave() throws Exception {
        mvc.perform(post("/api/v1/sesion/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(admin().getId(), "admin123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    // ---------- roles ----------

    @Test
    void operador_puedeOperarPedidos() throws Exception {
        mvc.perform(get("/api/v1/pedidos").header("Authorization", bearer(operadorFicticio())))
                .andExpect(status().isOk());
    }

    @Test
    void operador_noPuedeEntrarARutasDeAdmin_responde403() throws Exception {
        String token = bearer(operadorFicticio());
        mvc.perform(get("/api/v1/configuracion").header("Authorization", token))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/backups").header("Authorization", token))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/usuarios").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Intruso Prueba\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void admin_puedeEntrarARutasDeAdmin() throws Exception {
        mvc.perform(get("/api/v1/configuracion").header("Authorization", bearer(admin())))
                .andExpect(status().isOk());
    }

    // ---------- rutas públicas ----------

    @Test
    void rutasPublicas_siguenAbiertasSinToken() throws Exception {
        mvc.perform(get("/api/v1/usuarios")).andExpect(status().isOk());
        mvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }
}
