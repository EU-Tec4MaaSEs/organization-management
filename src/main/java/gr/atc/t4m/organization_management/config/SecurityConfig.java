package gr.atc.t4m.organization_management.config;

import java.util.Arrays;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

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
        CsrfTokenRequestAttributeHandler requestHandler = new CsrfTokenRequestAttributeHandler();

        http
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .cors(corsCustomizer -> corsCustomizer.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.csrfTokenRequestHandler(requestHandler)
                            .ignoringRequestMatchers("/api/organization/**","/organization/swagger-ui/**","/organization/v3/api-docs/**") // For now ignore all requests under api/organization
                            .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse()))
                        
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/organization/**", "/organization/v3/api-docs/**",
                         "/organization/swagger-ui/**").permitAll() // Allow all API requests for now
                         .requestMatchers(HttpMethod.PUT, "/api/organization/updateOrganization/**").permitAll() 

                        .anyRequest().authenticated() // Require authentication for other routes
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
