package com.dismal.distribuciones.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final AuthenticationProvider authenticationProvider;
    private final AuthenticationEntryPoint authenticationEntryPoint;
    private final AccessDeniedHandler accessDeniedHandler;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .authorizeHttpRequests(req -> req
                        // Public entry points. Sensitive account and fulfillment routes remain authenticated below.
                        .requestMatchers(
                                "/api/v1/auth/**",
                                "/api/public/products/**",
                                "/api/public/storefront/**",
                                "/api/public/integrations/woocommerce/**",
                                "/api/v1/marketing/intelligence/interactions",
                                "/api/v1/integrations/notifications/templates/delivery-callback",
                                "/actuator/health/**",
                                "/actuator/prometheus",
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html"
                        ).permitAll()
                        .requestMatchers("/api/v1/users/me", "/api/v1/storefront/account/**").authenticated()
                        .requestMatchers("/api/v1/storefront/**").hasAnyAuthority("ADMIN", "MANAGER")
                        .requestMatchers("/api/v1/integrations/crm/status").authenticated()
                        .requestMatchers("/api/v1/integrations/outbox/**").hasAnyAuthority("ADMIN", "MANAGER", "OPERATOR")
                        .requestMatchers("/api/v1/integrations/n8n/**").hasAnyAuthority("ADMIN", "OPERATOR")
                        .requestMatchers("/api/v1/integrations/**").hasAnyAuthority("ADMIN", "MANAGER")
                        .requestMatchers("/api/v1/admin/**").hasAnyAuthority("ADMIN", "MANAGER")
                        .requestMatchers("/api/v1/store/**").hasAnyAuthority("ADMIN", "MANAGER")
                        .requestMatchers("/api/v1/pricing/**").hasAnyAuthority("ADMIN", "MANAGER")
                        .requestMatchers("/api/v1/inventory/**").hasAnyAuthority("ADMIN", "MANAGER")
                        .requestMatchers("/api/v1/sales/**").hasAnyAuthority("ADMIN", "MANAGER")
                        .requestMatchers("/api/v1/invoices/**").hasAnyAuthority("ADMIN", "MANAGER")
                        .requestMatchers("/api/v1/quotes/**").hasAnyAuthority("ADMIN", "MANAGER")
                        .requestMatchers("/api/v1/payments/**").hasAnyAuthority("ADMIN", "MANAGER")
                        .requestMatchers("/api/v1/payment-accounts/**").hasAnyAuthority("ADMIN", "MANAGER")
                        .requestMatchers("/api/v1/customers/**").hasAnyAuthority("ADMIN", "MANAGER")
                        .requestMatchers("/api/v1/planning/**").hasAnyAuthority("ADMIN", "MANAGER")
                        .requestMatchers("/api/v1/marketing/**").hasAnyAuthority("ADMIN", "MANAGER")
                        .requestMatchers("/api/v1/reports/**").hasAnyAuthority("ADMIN", "MANAGER")
                        .requestMatchers("/api/v1/dashboard/admin/**").hasAnyAuthority("ADMIN", "MANAGER")
                        .requestMatchers("/api/v1/expenses/**").hasAnyAuthority("ADMIN", "MANAGER")
                        .anyRequest().authenticated()
                )
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationProvider(authenticationProvider)
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
