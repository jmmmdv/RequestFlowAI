package com.fromzerotohero.mission.intake;

import static org.assertj.core.api.Assertions.assertThat;

import com.fromzerotohero.mission.workitem.Priority;
import org.junit.jupiter.api.Test;

class RuleBasedRequestClassifierTest {
    private final RuleBasedRequestClassifier classifier = new RuleBasedRequestClassifier();

    @Test
    void classifiesUrgentSupportRequestsWithoutAnExternalModel() {
        var result = classifier.classify("Booking site outage",
                "Urgent: customers cannot access the booking form.");

        assertThat(result.category()).isEqualTo(RequestCategory.SUPPORT);
        assertThat(result.priority()).isEqualTo(Priority.CRITICAL);
        assertThat(result.internalSummary()).contains("Booking site outage");
        assertThat(result.recommendedNextAction()).startsWith("Acknowledge immediately");
    }

    @Test
    void recognizesBillingSalesChangeAndLowPriorityLanguage() {
        assertThat(classifier.classify("Refund request", "Please review this payment charge.").category())
                .isEqualTo(RequestCategory.BILLING);
        assertThat(classifier.classify("Website quote", "Please send pricing and an estimate.").category())
                .isEqualTo(RequestCategory.SALES);
        assertThat(classifier.classify("Update homepage", "Change the campaign message.").category())
                .isEqualTo(RequestCategory.CHANGE_REQUEST);
        assertThat(classifier.classify("General question", "No rush, information only please.").priority())
                .isEqualTo(Priority.LOW);
    }
}
