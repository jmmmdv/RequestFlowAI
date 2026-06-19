package com.fromzerotohero.mission.security;

import java.util.ArrayList;
import java.util.Collection;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.http.HttpMethod;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.config.Customizer;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.List;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    @Bean
    @ConditionalOnProperty(name = "mission.security.enabled", havingValue = "false", matchIfMissing = true)
    SecurityFilterChain localSecurity(HttpSecurity http) throws Exception {
        return http.csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .authorizeHttpRequests(requests -> requests.anyRequest().permitAll()).build();
    }

    @Bean
    @ConditionalOnProperty(name = "mission.security.enabled", havingValue = "true")
    SecurityFilterChain productionSecurity(HttpSecurity http) throws Exception {
        return http.csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(requests -> requests
                        .requestMatchers("/actuator/health/**", "/actuator/info").permitAll()
                        .requestMatchers("/actuator/**").hasRole("ADMIN")
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers("/api/public/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/billing/webhook").permitAll()
                        .requestMatchers("/api/billing/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/agent/runs/*/approve").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/agent/runs").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/saas/organization").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/saas/portal").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/saas/portal/rotate-token").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/saas/onboarding").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/saas/portal").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/saas/invitations").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/saas/members", "/api/saas/invitations").hasRole("ADMIN")
                        .requestMatchers("/api/agent/**").hasAnyRole("MEMBER", "ADMIN")
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().permitAll())
                .oauth2ResourceServer(oauth -> oauth.jwt(jwt ->
                        jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())))
                .build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(
            @Value("${mission.frontend-url:http://localhost:8080}") String frontendUrl) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(frontendUrl));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "Idempotency-Key", "X-Correlation-ID", "X-Portal-Token"));
        configuration.setExposedHeaders(List.of("Idempotency-Key", "X-Correlation-ID", "Location"));
        configuration.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }

    private JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter scopes = new JwtGrantedAuthoritiesConverter();
        Converter<Jwt, Collection<GrantedAuthority>> authorities = jwt -> {
            Collection<GrantedAuthority> result = new ArrayList<>(scopes.convert(jwt));
            Collection<String> roles = jwt.getClaimAsStringList("roles");
            if (roles != null) roles.forEach(role -> result.add(new SimpleGrantedAuthority("ROLE_" + role)));
            Collection<String> groups = jwt.getClaimAsStringList("cognito:groups");
            if (groups != null) groups.forEach(group -> result.add(new SimpleGrantedAuthority("ROLE_" + group)));
            return result;
        };
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);
        return converter;
    }
}
