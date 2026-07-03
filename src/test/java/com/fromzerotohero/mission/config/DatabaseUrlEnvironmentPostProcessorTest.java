package com.fromzerotohero.mission.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class DatabaseUrlEnvironmentPostProcessorTest {
    @Test
    void convertsNeonPostgresUrlToSpringJdbcProperties() {
        var properties = DatabaseUrlEnvironmentPostProcessor.toSpringDatasourceProperties(
                "postgresql://request_user:p%40ssword@ep-example.us-east-2.aws.neon.tech/neondb?sslmode=require");

        assertThat(properties)
                .containsEntry("spring.datasource.url",
                        "jdbc:postgresql://ep-example.us-east-2.aws.neon.tech/neondb?sslmode=require")
                .containsEntry("spring.datasource.username", "request_user")
                .containsEntry("spring.datasource.password", "p@ssword");
    }

    @Test
    void keepsJdbcUrlsAsProvidedForLocalPostgres() {
        var properties = DatabaseUrlEnvironmentPostProcessor.toSpringDatasourceProperties(
                "jdbc:postgresql://localhost:5432/requestflow");

        assertThat(properties)
                .containsEntry("spring.datasource.url", "jdbc:postgresql://localhost:5432/requestflow")
                .doesNotContainKeys("spring.datasource.username", "spring.datasource.password");
    }
}
