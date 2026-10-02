package com.dismal.distribuciones.config;

import com.dismal.distribuciones.exception.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.web.context.request.ServletWebRequest;

import java.io.IOException;
import java.time.LocalDateTime;

@Configuration
public class SecurityHandlersConfig {

    @Bean
    public AuthenticationEntryPoint restAuthenticationEntryPoint(ObjectMapper mapper) {
        return (request, response, authException) -> {
            ErrorResponse error = new ErrorResponse(
                    LocalDateTime.now(),
                    HttpStatus.UNAUTHORIZED.value(),
                    "Unauthorized",
                    "Authentication is required.",
                    new ServletWebRequest(request).getRequest().getRequestURI()
            );
            writeJson(response, mapper, HttpStatus.UNAUTHORIZED.value(), error);
        };
    }

    @Bean
    public AccessDeniedHandler restAccessDeniedHandler(ObjectMapper mapper) {
        return (request, response, accessDeniedException) -> {
            ErrorResponse error = new ErrorResponse(
                    LocalDateTime.now(),
                    HttpStatus.FORBIDDEN.value(),
                    "Forbidden",
                    "Access is denied.",
                    new ServletWebRequest(request).getRequest().getRequestURI()
            );
            writeJson(response, mapper, HttpStatus.FORBIDDEN.value(), error);
        };
    }

    private void writeJson(
            HttpServletResponse response,
            ObjectMapper mapper,
            int status,
            ErrorResponse error
    ) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        mapper.writeValue(response.getOutputStream(), error);
    }
}
