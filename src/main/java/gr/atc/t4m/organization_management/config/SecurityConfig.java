package gr.atc.t4m.organization_management.config;

import java.util.Arrays;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import gr.atc.t4m.organization_management.security.JwtAuthConverter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    /**
     * Initialize and Configure Security Filter Chain of HTTP connection
     * 
     * @param http       HttpSecurity
     * @param entryPoint UnauthorizedEntryPoint -> To add proper API Response to the
     *                   authorized request
     * @return SecurityFilterChain
     */

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
                // Convert Keycloak Roles with class to Spring Security Roles
        JwtAuthConverter jwtAuthConverter = new JwtAuthConverter();
        http
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .cors(corsCustomizer -> corsCustomizer.configurationSource(corsConfigurationSource()))
                // Configure CSRF Token
                .csrf(AbstractHttpConfigurer::disable)
                        
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers( "/organization/v3/api-docs/**",
                         "/organization/swagger-ui/**").permitAll() // Allow all API requests for now
                        .anyRequest().authenticated()) // Require authentication for other routes
                        // JWT Authentication Configuration to use with Keycloak
                        .oauth2ResourceServer(oauth2ResourceServerCustomizer -> oauth2ResourceServerCustomizer
                        .jwt(jwtCustomizer -> jwtCustomizer.jwtAuthenticationConverter(jwtAuthConverter))
                );

        return http.build();
    }

    /**
     * Settings for CORS
     *
     * @return CorsConfigurationSource
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList(
            "http://localhost:3000","http://localhost:8090")); 
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(86400L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

}
