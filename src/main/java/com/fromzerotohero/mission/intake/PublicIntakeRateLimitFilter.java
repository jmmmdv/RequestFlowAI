package com.fromzerotohero.mission.intake;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
@ConditionalOnProperty(name = "mission.intake.rate-limit.enabled", havingValue = "true")
public class PublicIntakeRateLimitFilter extends OncePerRequestFilter {
    private final PublicIntakeProperties properties;
    private final ObjectMapper objectMapper;
    private final Map<String, Deque<Instant>> windows = new ConcurrentHashMap<>();

    public PublicIntakeRateLimitFilter(PublicIntakeProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!"POST".equalsIgnoreCase(request.getMethod())) return true;
        String path = request.getRequestURI();
        return path == null || !path.startsWith("/api/public/intake/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String key = clientKey(request);
        int limit = Math.max(1, properties.getRateLimit().getRequestsPerMinute());
        Instant cutoff = Instant.now().minus(Duration.ofMinutes(1));
        Deque<Instant> window = windows.computeIfAbsent(key, ignored -> new ArrayDeque<>());
        synchronized (window) {
            while (!window.isEmpty() && window.peekFirst().isBefore(cutoff)) window.removeFirst();
            if (window.size() >= limit) {
                writeTooManyRequests(response);
                return;
            }
            window.addLast(Instant.now());
        }
        chain.doFilter(request, response);
    }

    private String clientKey(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) return forwarded.split(",")[0].trim();
        return request.getRemoteAddr();
    }

    private void writeTooManyRequests(HttpServletResponse response) throws IOException {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.TOO_MANY_REQUESTS,
                "Too many requests. Please wait a minute and try again.");
        detail.setTitle("Rate limit exceeded");
        detail.setType(URI.create("https://from-zero-to-hero.dev/problems/rate-limit-exceeded"));
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), detail);
    }
}
