package com.fromzerotohero.mission.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Refuses to start when a production-shaped PostgreSQL datasource is configured without API security.
 */
@Component
public class ProductionSafetyValidator implements ApplicationRunner {
    private final String datasourceUrl;
    private final boolean securityEnabled;
    private final boolean enforcementEnabled;
    private final String securityProvider;
    private final String adminPasswordHash;

    public ProductionSafetyValidator(
            @Value("${spring.datasource.url:}") String datasourceUrl,
            @Value("${mission.security.enabled:false}") boolean securityEnabled,
            @Value("${mission.production-safety.enabled:true}") boolean enforcementEnabled,
            @Value("${mission.security.provider:oauth2}") String securityProvider,
            @Value("${requestflow.admin.password-hash:}") String adminPasswordHash) {
        this.datasourceUrl = datasourceUrl;
        this.securityEnabled = securityEnabled;
        this.enforcementEnabled = enforcementEnabled;
        this.securityProvider = securityProvider == null ? "" : securityProvider.trim().toLowerCase();
        this.adminPasswordHash = adminPasswordHash == null ? "" : adminPasswordHash.trim();
    }

    @Override
    public void run(ApplicationArguments args) {
        validate();
    }

    void validate() {
        if (!enforcementEnabled) {
            return;
        }
        if (!isPostgreSql(datasourceUrl)) {
            return;
        }
        if (!securityEnabled) {
            throw new IllegalStateException(
                    "Refusing to start: PostgreSQL is configured but mission.security.enabled=false. "
                            + "Enable security or use an in-memory datasource for local development.");
        }
        if (securityProvider.equals("basic") && adminPasswordHash.isBlank()) {
            throw new IllegalStateException(
                    "Refusing to start: Basic Auth is configured for PostgreSQL but ADMIN_PASSWORD_HASH is blank. "
                            + "Set a BCrypt hash through the environment before using Neon or another Postgres database.");
        }
    }

    private static boolean isPostgreSql(String url) {
        return url != null && url.toLowerCase().contains("jdbc:postgresql:");
    }
}
