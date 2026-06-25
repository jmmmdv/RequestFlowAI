package com.fromzerotohero.mission.ai.usage;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fromzerotohero.mission.ai.budget.AiBudgetProperties;
import com.fromzerotohero.mission.ai.budget.AiBudgetService;
import java.math.BigDecimal;
import java.time.Clock;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AiUsageEventServiceTest {
    private static final UUID TENANT = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Test
    void safePublicIntakeRecordingSwallowsPersistenceFailures() {
        AiUsageEventRepository repository = mock(AiUsageEventRepository.class);
        when(repository.sumEstimatedCostUsdSince(any())).thenReturn(BigDecimal.ZERO);
        when(repository.save(any())).thenThrow(new RuntimeException("database unavailable"));
        AiUsageEventService service = new AiUsageEventService(repository,
                new AiBudgetService(new AiBudgetProperties()), Clock.systemUTC());

        assertThatCode(() -> service.recordPublicIntakeClassificationSafely(
                TENANT, "local", UUID.randomUUID())).doesNotThrowAnyException();
    }
}
