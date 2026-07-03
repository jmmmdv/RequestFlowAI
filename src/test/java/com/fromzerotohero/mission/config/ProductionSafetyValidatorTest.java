package com.fromzerotohero.mission.config;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ProductionSafetyValidatorTest {
    @Test
    void rejectsPostgreSqlWithoutSecurity() {
        ProductionSafetyValidator validator = new ProductionSafetyValidator(
                "jdbc:postgresql://db.internal:5432/mission_control", false, true, "oauth2", "");

        assertThatThrownBy(validator::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("PostgreSQL is configured")
                .hasMessageContaining("mission.security.enabled=false");
    }

    @Test
    void allowsPostgreSqlWhenSecurityEnabled() {
        ProductionSafetyValidator validator = new ProductionSafetyValidator(
                "jdbc:postgresql://db.internal:5432/mission_control", true, true, "oauth2", "");

        assertThatCode(validator::validate).doesNotThrowAnyException();
    }

    @Test
    void allowsH2WithoutSecurity() {
        ProductionSafetyValidator validator = new ProductionSafetyValidator(
                "jdbc:h2:mem:requestflow", false, true, "basic", "");

        assertThatCode(validator::validate).doesNotThrowAnyException();
    }

    @Test
    void canBeDisabledForIntegrationTests() {
        ProductionSafetyValidator validator = new ProductionSafetyValidator(
                "jdbc:postgresql://localhost:5432/test", false, false, "basic", "");

        assertThatCode(validator::validate).doesNotThrowAnyException();
    }

    @Test
    void rejectsPostgreSqlBasicAuthWithoutPasswordHash() {
        ProductionSafetyValidator validator = new ProductionSafetyValidator(
                "jdbc:postgresql://db.internal:5432/requestflow", true, true, "basic", "");

        assertThatThrownBy(validator::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ADMIN_PASSWORD_HASH");
    }

    @Test
    void allowsPostgreSqlBasicAuthWithPasswordHash() {
        ProductionSafetyValidator validator = new ProductionSafetyValidator(
                "jdbc:postgresql://db.internal:5432/requestflow", true, true,
                "basic", "{bcrypt}$2a$10$012345678901234567890u1QJZkD9fZxTQf77NOZpQd6twxFz9t2K");

        assertThatCode(validator::validate).doesNotThrowAnyException();
    }
}
