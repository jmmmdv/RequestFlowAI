package com.fromzerotohero.mission.config;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ProductionSafetyValidatorTest {
    @Test
    void rejectsPostgreSqlWithoutSecurity() {
        ProductionSafetyValidator validator = new ProductionSafetyValidator(
                "jdbc:postgresql://db.internal:5432/mission_control", false, true);

        assertThatThrownBy(validator::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("PostgreSQL is configured")
                .hasMessageContaining("mission.security.enabled=false");
    }

    @Test
    void allowsPostgreSqlWhenSecurityEnabled() {
        ProductionSafetyValidator validator = new ProductionSafetyValidator(
                "jdbc:postgresql://db.internal:5432/mission_control", true, true);

        assertThatCode(validator::validate).doesNotThrowAnyException();
    }

    @Test
    void allowsH2WithoutSecurity() {
        ProductionSafetyValidator validator = new ProductionSafetyValidator(
                "jdbc:h2:mem:requestflow", false, true);

        assertThatCode(validator::validate).doesNotThrowAnyException();
    }

    @Test
    void canBeDisabledForIntegrationTests() {
        ProductionSafetyValidator validator = new ProductionSafetyValidator(
                "jdbc:postgresql://localhost:5432/test", false, false);

        assertThatCode(validator::validate).doesNotThrowAnyException();
    }
}
