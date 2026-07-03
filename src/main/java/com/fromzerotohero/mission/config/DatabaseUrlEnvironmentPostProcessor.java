package com.fromzerotohero.mission.config;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

public class DatabaseUrlEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {
    private static final String PROPERTY_SOURCE_NAME = "requestflowDatabaseUrl";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String databaseUrl = environment.getProperty("DATABASE_URL");
        if (databaseUrl == null || databaseUrl.isBlank()) return;
        Map<String, Object> properties = toSpringDatasourceProperties(databaseUrl.trim());
        if (!properties.isEmpty()) {
            environment.getPropertySources().addFirst(new MapPropertySource(PROPERTY_SOURCE_NAME, properties));
        }
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }

    static Map<String, Object> toSpringDatasourceProperties(String databaseUrl) {
        Map<String, Object> properties = new LinkedHashMap<>();
        if (databaseUrl.startsWith("jdbc:postgresql:")) {
            properties.put("spring.datasource.url", databaseUrl);
            return properties;
        }
        if (!databaseUrl.startsWith("postgresql://") && !databaseUrl.startsWith("postgres://")) {
            return properties;
        }

        URI uri = URI.create(databaseUrl);
        StringBuilder jdbc = new StringBuilder("jdbc:postgresql://");
        jdbc.append(uri.getHost());
        if (uri.getPort() > 0) jdbc.append(':').append(uri.getPort());
        jdbc.append(uri.getRawPath() == null || uri.getRawPath().isBlank() ? "/" : uri.getRawPath());
        if (uri.getRawQuery() != null && !uri.getRawQuery().isBlank()) {
            jdbc.append('?').append(uri.getRawQuery());
        } else {
            jdbc.append("?sslmode=require");
        }
        properties.put("spring.datasource.url", jdbc.toString());

        String userInfo = uri.getRawUserInfo();
        if (userInfo != null && !userInfo.isBlank()) {
            String[] parts = userInfo.split(":", 2);
            properties.put("spring.datasource.username", decode(parts[0]));
            if (parts.length > 1) properties.put("spring.datasource.password", decode(parts[1]));
        }
        return properties;
    }

    private static String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }
}
