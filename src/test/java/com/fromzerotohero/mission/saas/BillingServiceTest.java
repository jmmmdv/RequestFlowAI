package com.fromzerotohero.mission.saas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fromzerotohero.mission.security.TenantContext;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
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

    @Test
    void trialingSubscriptionKeepsTheCurrentPaidPlan() throws Exception {
        BillingSubscription existing = new BillingSubscription(TENANT);
        existing.synchronize("cus_x", "sub_x", Plan.PRO, "ACTIVE", null);
        when(subscriptions.findByStripeSubscriptionId("sub_x")).thenReturn(Optional.of(existing));
        TenantOrganization organization = new TenantOrganization(TENANT, "Acme", "acme");
        organization.changePlan(Plan.PRO);
        when(organizations.findById(TENANT)).thenReturn(Optional.of(organization));

        String payload = """
                {"type":"customer.subscription.updated","data":{"object":{
                  "id":"sub_x","customer":"cus_x","status":"trialing","current_period_end":1893456000}}}
                """.trim();
        billingWith("price_pro", "price_business").processWebhook(payload, signedHeader(payload));

        assertThat(existing.getStatus()).isEqualTo("TRIALING");
        assertThat(existing.getPlan()).isEqualTo(Plan.PRO);
        assertThat(organization.getPlan()).isEqualTo(Plan.PRO);
    }

    @Test
    void unknownSubscriptionEventIsIgnored() throws Exception {
        when(subscriptions.findByStripeSubscriptionId("sub_missing")).thenReturn(Optional.empty());

        String payload = """
                {"type":"customer.subscription.deleted","data":{"object":{
                  "id":"sub_missing","customer":"cus_missing","status":"canceled"}}}
                """.trim();
        billingWith("price_pro", "price_business").processWebhook(payload, signedHeader(payload));

        verify(organizations, never()).findById(any());
    }

    private String signedHeader(String payload) throws Exception {
        long timestamp = Instant.now().getEpochSecond();
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec("whsec_test".getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        String signature = HexFormat.of()
                .formatHex(mac.doFinal((timestamp + "." + payload).getBytes(StandardCharsets.UTF_8)));
        return "t=" + timestamp + ",v1=" + signature;
    }
}
