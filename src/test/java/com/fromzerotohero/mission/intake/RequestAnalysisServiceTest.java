package com.fromzerotohero.mission.intake;

import static org.assertj.core.api.Assertions.assertThat;

import com.fromzerotohero.mission.ai.usage.AiAnalysisSource;
import com.fromzerotohero.mission.workitem.Priority;
import org.junit.jupiter.api.Test;

class RequestAnalysisServiceTest {
    private final RuleBasedRequestClassifier classifier = new RuleBasedRequestClassifier();
    private final RequestAnalysisService service = new RequestAnalysisService(classifier);

    @Test
    void matchesRuleBasedClassifierForUrgentSupportRequest() {
        RequestAnalysisInput input = new RequestAnalysisInput("Booking site outage",
                "Urgent: customers cannot access the booking form.", null, null);

        RequestAnalysisResult facadeResult = service.analyze(input);
        RuleBasedRequestClassifier.Classification directResult = classifier.classify(
                input.title(), input.details(), input.requestedCategory(), input.requestedUrgency());

        assertThat(facadeResult.classification()).isEqualTo(directResult);
        assertThat(facadeResult.analysisSource()).isEqualTo(AiAnalysisSource.RULE_BASED);
        assertThat(facadeResult.paidAiUsed()).isFalse();
        assertThat(facadeResult.fallbackUsed()).isFalse();
        assertThat(facadeResult.classification().category()).isEqualTo(RequestCategory.SUPPORT);
        assertThat(facadeResult.classification().priority()).isEqualTo(Priority.CRITICAL);
    }

    @Test
    void matchesRuleBasedClassifierForBillingAndRequesterSignals() {
        RequestAnalysisInput input = new RequestAnalysisInput("Please help",
                "This request has enough detail.", RequestCategory.BILLING, RequestUrgency.HIGH);

        assertThat(service.analyze(input).classification()).isEqualTo(classifier.classify(
                input.title(), input.details(), input.requestedCategory(), input.requestedUrgency()));
    }

    @Test
    void matchesRuleBasedClassifierForLowPriorityGeneralRequest() {
        RequestAnalysisInput input = new RequestAnalysisInput("General question",
                "No rush, information only please.", null, null);

        assertThat(service.analyze(input).classification()).isEqualTo(classifier.classify(
                input.title(), input.details(), input.requestedCategory(), input.requestedUrgency()));
        assertThat(service.analyze(input).classification().priority()).isEqualTo(Priority.LOW);
    }
}
