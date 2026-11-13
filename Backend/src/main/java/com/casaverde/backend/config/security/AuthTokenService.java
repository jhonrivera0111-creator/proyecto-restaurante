package com.casaverde.backend.config.security;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.casaverde.backend.capaAccesoADatos.models.entitys.Usuario;

/**
 * Emite y valida los tokens de sesión (formato propio, parecido a un JWT).
 * Token = base64(payload) + "." + firma, donde payload = "id:rol:expiración:nombre"
 * y firma = HMAC-SHA256(payload, secreto). Si alguien altera el payload, la firma
 * ya no coincide y el token se rechaza. Caduca a las 12 h (configurable).
 * El secreto viene de la propiedad app.security.token-secret (en producción, de
 * la variable de entorno APP_SECURITY_TOKEN_SECRET).
 */
@Service
public class AuthTokenService {

    private static final String HMAC_ALGORITHM = "HmacSHA256";

    @Value("${app.security.token-secret}")
    private String secret;

    @Value("${app.security.token-expiration-hours:12}")
    private long expirationHours;

    /** Crea el token firmado para un usuario que acaba de iniciar sesión. */
    public String generar(Usuario usuario) {
        long expiraEn = Instant.now().plusSeconds(expirationHours * 3600).getEpochSecond();
        // Payload: id:rol:expiraEn:nombre  (split limit=4 para tolerar ":" en el nombre)
        String payload = usuario.getId() + ":" + usuario.getRol() + ":" + expiraEn + ":" + usuario.getNombre();
        return encode(payload) + "." + firmar(payload);
    }

    /** Devuelve el usuario si el token es auténtico y no ha vencido; null en cualquier otro caso. */
    public UsuarioAutenticado validar(String token) {
        try {
            return validarInterno(token);
        } catch (RuntimeException e) {
            return null;
        }
    }

    private UsuarioAutenticado validarInterno(String token) {
        if (token == null || token.isBlank() || !token.contains(".")) {
            return null;
        }
        String[] parts = token.split("\\.", 2);
        String payload = decode(parts[0]);
        if (!firmar(payload).equals(parts[1])) {
            return null;
        }
        String[] campos = payload.split(":", 4);
        if (campos.length < 3) {
            return null;
        }
        long expiraEn = Long.parseLong(campos[2]);
        if (Instant.now().getEpochSecond() > expiraEn) {
            return null;
        }
        String nombre = campos.length == 4 ? campos[3] : "usuario-" + campos[0];
        return new UsuarioAutenticado(Long.valueOf(campos[0]), nombre, campos[1]);
    }

    private String firmar(String payload) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo firmar el token de sesion", e);
        }
    }

    private String encode(String raw) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    private String decode(String encoded) {
        return new String(Base64.getUrlDecoder().decode(encoded), StandardCharsets.UTF_8);
    }

    public record UsuarioAutenticado(Long id, String nombre, String rol) {
        public List<String> authorities() {
            return List.of("ROLE_" + rol);
        }
    }
}
