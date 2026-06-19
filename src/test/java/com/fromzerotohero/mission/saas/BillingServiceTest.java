package com.fromzerotohero.mission.saas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fromzerotohero.mission.security.TenantContext;
import java.time.Clock;
import java.util.UUID;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/**
 * Focused unit test for the Stripe checkout path, which the Spring integration test cannot exercise
 * because it would require a live Stripe API. A mocked {@link RestClient} lets us prove the
 * plan-to-price mapping, authorization, idempotency, and response handling without external calls.
 */
class BillingServiceTest {
    private static final UUID TENANT = UUID.fromString("00000000-0000-0000-0000-000000000009");

    private TenantContext tenantContext;
    private MockRestServiceServer stripeServer;
    private RestClient stripe;
    private BillingSubscriptionRepository subscriptions;
    private TenantOrganizationRepository organizations;

    @BeforeEach
    void setUp() {
        subscriptions = Mockito.mock(BillingSubscriptionRepository.class);
        organizations = Mockito.mock(TenantOrganizationRepository.class);
        tenantContext = Mockito.mock(TenantContext.class);
        when(tenantContext.tenantId()).thenReturn(TENANT);

        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.stripe.com/v1");
        stripeServer = MockRestServiceServer.bindTo(builder).build();
        stripe = builder.build();
    }

    private BillingService billingWith(String proPriceId, String businessPriceId) {
        return new BillingService(subscriptions, organizations, tenantContext, new ObjectMapper(),
                stripe, "sk_test_123", "whsec_test", proPriceId, businessPriceId,
                "https://app.example.com/", Clock.systemUTC());
    }

    @Test
    void createCheckoutReturnsStripeSessionForPaidPlan() {
        stripeServer.expect(requestTo("https://api.stripe.com/v1/checkout/sessions"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer sk_test_123"))
                .andExpect(header("Idempotency-Key", "checkout-key-01"))
                .andExpect(content().string(Matchers.containsString("price_pro")))
                .andRespond(withSuccess(
                        "{\"id\":\"cs_test_123\",\"url\":\"https://checkout.stripe.com/pay/cs_test_123\"}",
                        MediaType.APPLICATION_JSON));

        BillingService.CheckoutResponse response = billingWith("price_pro", "price_business")
                .createCheckout(new BillingService.CheckoutRequest(Plan.PRO, "checkout-key-01"));

        assertThat(response.sessionId()).isEqualTo("cs_test_123");
        assertThat(response.checkoutUrl()).isEqualTo("https://checkout.stripe.com/pay/cs_test_123");
        stripeServer.verify();
    }

    @Test
    void createCheckoutRejectsFreePlanWithoutCallingStripe() {
        assertThatThrownBy(() -> billingWith("price_pro", "price_business")
                .createCheckout(new BillingService.CheckoutRequest(Plan.FREE, "checkout-key-02")))
                .isInstanceOf(SaasException.class)
                .hasMessageContaining("FREE");
        stripeServer.verify();
    }

    @Test
    void createCheckoutFailsWhenStripeOmitsCheckoutUrl() {
        stripeServer.expect(requestTo("https://api.stripe.com/v1/checkout/sessions"))
                .andRespond(withSuccess("{\"id\":\"cs_test\"}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> billingWith("price_pro", "price_business")
                .createCheckout(new BillingService.CheckoutRequest(Plan.BUSINESS, "checkout-key-03")))
                .isInstanceOf(SaasException.class)
                .hasMessageContaining("checkout URL");
        stripeServer.verify();
    }

    @Test
    void createCheckoutReportsUnconfiguredPlanPrice() {
        assertThatThrownBy(() -> billingWith("", "price_business")
                .createCheckout(new BillingService.CheckoutRequest(Plan.PRO, "checkout-key-04")))
                .isInstanceOf(SaasException.class)
                .hasMessageContaining("Stripe price is not configured");
        stripeServer.verify();
    }
}
