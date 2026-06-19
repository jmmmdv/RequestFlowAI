package com.fromzerotohero.mission.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class InfrastructureAsCodeTest {
    private static final Path AWS_TEMPLATE = Path.of("infrastructure/aws/template.yaml");

    @Test
    void productionTemplateKeepsPostgreSqlPrivateRecoverableAndSecretBacked() throws Exception {
        String template = Files.readString(AWS_TEMPLATE);

        assertThat(template)
                .contains("Type: AWS::RDS::DBInstance")
                .contains("PubliclyAccessible: false")
                .contains("StorageEncrypted: true")
                .contains("DeletionPolicy: Snapshot")
                .contains("BackupRetentionPeriod:")
                .contains("Type: AWS::SecretsManager::Secret")
                .contains("RuntimeEnvironmentSecrets:")
                .contains("DestinationSecurityGroupId: !Ref DatabaseSecurityGroup");
    }

    @Test
    void productionTemplateProvidesNetworkAndOperationalEvidence() throws Exception {
        String template = Files.readString(AWS_TEMPLATE);

        assertThat(template)
                .contains("Type: AWS::AppRunner::VpcConnector")
                .contains("EgressType: VPC")
                .contains("Type: AWS::CloudWatch::Dashboard")
                .contains("MetricName: 5xxStatusResponses")
                .contains("MetricName: RequestLatency")
                .contains("Type: AWS::SNS::Topic");
    }

    @Test
    void localObservabilityStackIsProvisionedAsCode() {
        assertThat(Path.of("infrastructure/observability/otel-collector.yaml")).exists();
        assertThat(Path.of("infrastructure/observability/prometheus.yml")).exists();
        assertThat(Path.of("infrastructure/observability/grafana/dashboards/mission-control.json")).exists();
        assertThat(Path.of("docs/operations/SLO.md")).exists();
        assertThat(Path.of("docs/operations/INCIDENT-RUNBOOK.md")).exists();
        assertThat(Path.of("docs/operations/RESTORE-DRILL.md")).exists();
    }

    @Test
    void vercelPublishesAnExplicitStaticPortfolioPreview() throws Exception {
        String configuration = Files.readString(Path.of("vercel.json"));

        assertThat(configuration)
                .contains("\"framework\": null")
                .contains("\"outputDirectory\": \"src/main/resources/static\"");
        assertThat(Files.readString(Path.of("src/main/resources/static/app.js")))
                .contains("location.hostname.endsWith('.vercel.app')")
                .contains("mission-control-demo-v1");
    }
}
