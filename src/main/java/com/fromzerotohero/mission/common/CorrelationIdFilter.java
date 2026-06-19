package com.fromzerotohero.mission.common;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class CorrelationIdFilter extends OncePerRequestFilter {
    static final String CORRELATION_HEADER = "X-Correlation-ID";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {
        String supplied = request.getHeader(CORRELATION_HEADER);
        String correlationId = supplied != null && supplied.matches("[A-Za-z0-9._-]{1,64}")
                ? supplied : UUID.randomUUID().toString();
        response.setHeader(CORRELATION_HEADER, correlationId);
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("X-Frame-Options", "DENY");
        response.setHeader("Referrer-Policy", "no-referrer");
        response.setHeader("Content-Security-Policy",
                "default-src 'self'; style-src 'self'; script-src 'self'; connect-src 'self'");
        try (MDC.MDCCloseable ignored = MDC.putCloseable("correlationId", correlationId)) {
            chain.doFilter(request, response);
        }
    }
}
