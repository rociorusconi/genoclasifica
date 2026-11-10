package com.example.genoclasifica.seguridad;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

// Matriz de permisos por rol (supuesto de RF-09, sección 3). Sin token: 401. Rol sin permiso: 403.
@Configuration
public class SeguridadConfig {

    @Bean
    public SecurityFilterChain filtros(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.POST, "/api/v1/auth/login").permitAll()
                // Archivos del portal: no tienen datos, los datos se piden a la API con el token
                .requestMatchers(HttpMethod.GET, "/", "/index.html", "/portal.js", "/portal.css", "/favicon.ico").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/samples").hasAnyRole("TECNICO", "ADMINISTRADOR")
                .requestMatchers(HttpMethod.POST, "/api/v1/samples/*/process").hasAnyRole("BIOINFORMATICO", "ADMINISTRADOR")
                // La firma la controla el servicio, para responder con el motivo del rechazo (CP-ST-INV-02)
                .requestMatchers(HttpMethod.POST, "/api/v1/samples/*/sign").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/v1/reports/*/pdf").hasAnyRole("TECNICO", "GENETISTA", "ADMINISTRADOR")
                .requestMatchers(HttpMethod.GET, "/api/v1/samples/**").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/v1/reclassifications").hasRole("ADMINISTRADOR")
                .requestMatchers(HttpMethod.GET, "/api/v1/notifications").hasAnyRole("GENETISTA", "ADMINISTRADOR")
                .requestMatchers(HttpMethod.GET, "/api/v1/audit").hasAnyRole("AUDITOR", "ADMINISTRADOR")
                .anyRequest().denyAll())
            .oauth2ResourceServer(o -> o.jwt(jwt -> jwt.jwtAuthenticationConverter(rolesDelToken())))
            .exceptionHandling(e -> e
                .authenticationEntryPoint((req, res, ex) ->
                    escribirError(res, HttpStatus.UNAUTHORIZED, "Falta iniciar sesión o el token es inválido"))
                .accessDeniedHandler((req, res, ex) ->
                    escribirError(res, HttpStatus.FORBIDDEN, "Acceso denegado: el rol no tiene permiso para esta operación")));
        return http.build();
    }

    // Lee el claim "rol" del token y lo convierte en ROLE_GENETISTA, ROLE_TECNICO, etc.
    private JwtAuthenticationConverter rolesDelToken() {
        JwtGrantedAuthoritiesConverter roles = new JwtGrantedAuthoritiesConverter();
        roles.setAuthoritiesClaimName("rol");
        roles.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(roles);
        return converter;
    }

    private static void escribirError(jakarta.servlet.http.HttpServletResponse res, HttpStatus status, String mensaje)
            throws java.io.IOException {
        res.setStatus(status.value());
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        res.setCharacterEncoding(StandardCharsets.UTF_8.name());
        res.getWriter().write("{\"status\":" + status.value() + ",\"mensaje\":\"" + mensaje + "\"}");
    }

    // Usuarios sintéticos, uno por rol. No hay datos reales (leyes 25.326 y 26.529).
    @Bean
    public UserDetailsService usuarios(PasswordEncoder encoder, @Value("${genoclasifica.usuarios.clave}") String clave) {
        String hash = encoder.encode(clave);
        return new InMemoryUserDetailsManager(
            User.withUsername("tecnico_lab").password(hash).roles(Rol.TECNICO.name()).build(),
            User.withUsername("bio_user").password(hash).roles(Rol.BIOINFORMATICO.name()).build(),
            User.withUsername("genetista_1").password(hash).roles(Rol.GENETISTA.name()).build(),
            User.withUsername("admin").password(hash).roles(Rol.ADMINISTRADOR.name()).build(),
            User.withUsername("auditor").password(hash).roles(Rol.AUDITOR.name()).build());
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public JwtEncoder jwtEncoder(SecretKey clave) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(clave));
    }

    @Bean
    public JwtDecoder jwtDecoder(SecretKey clave) {
        return NimbusJwtDecoder.withSecretKey(clave).macAlgorithm(MacAlgorithm.HS256).build();
    }

    @Bean
    public SecretKey claveJwt(@Value("${genoclasifica.jwt.secreto}") String secreto) {
        return new SecretKeySpec(secreto.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }
}
