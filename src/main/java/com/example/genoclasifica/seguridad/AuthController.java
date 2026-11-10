package com.example.genoclasifica.seguridad;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private static final long DURACION_SEGUNDOS = 3600;

    private final UserDetailsService usuarios;
    private final PasswordEncoder encoder;
    private final JwtEncoder jwtEncoder;

    public AuthController(UserDetailsService usuarios, PasswordEncoder encoder, JwtEncoder jwtEncoder) {
        this.usuarios = usuarios;
        this.encoder = encoder;
        this.jwtEncoder = jwtEncoder;
    }

    public record Login(@NotBlank String usuario, @NotBlank String clave) {
    }

    public record Token(String token, String rol, long expiraEnSegundos) {
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody Login login) {
        UserDetails usuario;
        try {
            usuario = usuarios.loadUserByUsername(login.usuario());
        } catch (UsernameNotFoundException e) {
            return credencialesInvalidas();
        }
        if (!encoder.matches(login.clave(), usuario.getPassword())) {
            return credencialesInvalidas();
        }

        String rol = usuario.getAuthorities().iterator().next().getAuthority().replace("ROLE_", "");
        Instant ahora = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
            .issuer("genoclasifica")
            .subject(usuario.getUsername())
            .issuedAt(ahora)
            .expiresAt(ahora.plusSeconds(DURACION_SEGUNDOS))
            .claim("rol", rol)
            .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return ResponseEntity.ok(new Token(token, rol, DURACION_SEGUNDOS));
    }

    // Mismo mensaje para usuario inexistente y clave incorrecta: no revela qué usuarios existen
    private ResponseEntity<Map<String, Object>> credencialesInvalidas() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(Map.of("status", 401, "mensaje", "Usuario o clave incorrectos"));
    }
}
