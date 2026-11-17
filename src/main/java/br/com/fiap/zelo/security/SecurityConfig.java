package br.com.fiap.zelo.security;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .headers(headers -> headers.frameOptions(frame -> frame.disable()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(customAuthenticationEntryPoint())
                        .accessDeniedHandler(customAccessDeniedHandler())
                )
                .authorizeHttpRequests(auth -> auth
                        // Rotas públicas: Auth, Swagger, H2 Console, Simulação
                        .requestMatchers(
                                "/api/v1/auth/**",
                                "/auth/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**",
                                "/h2-console/**",
                                "/api/v1/simulacao/**"
                        ).permitAll()

                        // Endpoints de Emergência e Check-in
                        .requestMatchers(HttpMethod.POST, "/api/v1/casas/*/emergencia").hasAnyRole("IDOSO", "CUIDADOR", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/alertas/*/checkin").hasAnyRole("IDOSO", "CUIDADOR", "ADMIN")

                        // Endpoints de Cuidador no fluxo de Alerta
                        .requestMatchers(HttpMethod.POST, "/api/v1/alertas/*/confirmacao").hasAnyRole("CUIDADOR", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/alertas/*/desfecho").hasAnyRole("CUIDADOR", "ADMIN")

                        // Consulta de Alertas e Status da Casa
                        .requestMatchers(HttpMethod.GET, "/api/v1/casas/*/status").hasAnyRole("CUIDADOR", "IDOSO", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/alertas/**").hasAnyRole("CUIDADOR", "IDOSO", "ADMIN")

                        // Abertura externa de alertas (ex.: R5 - Lembrete)
                        .requestMatchers(HttpMethod.POST, "/api/v1/alertas").hasAnyRole("SERVICO", "ADMIN", "CUIDADOR")

                        // Gestão administrativa de Casas e Regras
                        .requestMatchers(HttpMethod.POST, "/api/v1/casas").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/casas/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/casas/**").hasRole("ADMIN")

                        // Demais requisições autenticadas (incluindo GET /api/v1/casas e endpoints espelho)
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public AuthenticationEntryPoint customAuthenticationEntryPoint() {
        return (request, response, authException) -> {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write("{\"codigo\":\"NAO_AUTENTICADO\",\"mensagem\":\"Token JWT ausente ou inválido.\"}");
        };
    }

    @Bean
    public AccessDeniedHandler customAccessDeniedHandler() {
        return (request, response, accessDeniedException) -> {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write("{\"codigo\":\"ACESSO_NEGADO\",\"mensagem\":\"Permissão insuficiente para este recurso.\"}");
        };
    }
}
