package com.fromzerotohero.mission.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;

@Configuration
public class CognitoIdentityConfig {
    @Bean(destroyMethod = "close")
    @ConditionalOnExpression("!'${mission.cognito.user-pool-id:}'.isBlank()")
    CognitoIdentityProviderClient cognitoIdentityProviderClient(
            @Value("${AWS_REGION:us-east-1}") String region) {
        return CognitoIdentityProviderClient.builder()
                .region(Region.of(region))
                .build();
    }
}
