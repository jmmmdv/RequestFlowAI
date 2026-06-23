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
    private BillingWebhookEventRepository webhookEvents;
    private TenantOrganizationRepository organizations;

    @BeforeEach
    void setUp() {
        subscriptions = Mockito.mock(BillingSubscriptionRepository.class);
        webhookEvents = Mockito.mock(BillingWebhookEventRepository.class);
        organizations = Mockito.mock(TenantOrganizationRepository.class);
        tenantContext = Mockito.mock(TenantContext.class);
        when(tenantContext.tenantId()).thenReturn(TENANT);
        when(subscriptions.findById(TENANT)).thenReturn(Optional.empty());

        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.stripe.com/v1");
        stripeServer = MockRestServiceServer.bindTo(builder).build();
        stripe = builder.build();
    }

    private BillingService billingWith(String proPriceId, String businessPriceId) {
        return new BillingService(subscriptions, webhookEvents, organizations, tenantContext, new ObjectMapper(),
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
    void createCheckoutRejectsExistingEntitledSubscription() {
        BillingSubscription existing = new BillingSubscription(TENANT);
        existing.synchronize("cus_existing", "sub_existing", Plan.PRO, "ACTIVE", null);
        when(subscriptions.findById(TENANT)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> billingWith("price_pro", "price_business")
                .createCheckout(new BillingService.CheckoutRequest(Plan.BUSINESS, "checkout-key-05")))
                .isInstanceOf(SaasException.class)
                .hasMessageContaining("billing portal");
        stripeServer.verify();
    }

    @Test
    void checkoutWithUnpaidPaymentStatusDoesNotActivatePlan() throws Exception {
        when(webhookEvents.existsById("evt_unpaid")).thenReturn(false);

        String payload = """
                {"id":"evt_unpaid","type":"checkout.session.completed","data":{"object":{
                  "client_reference_id":"00000000-0000-0000-0000-000000000009",
                  "customer":"cus_unpaid","subscription":"sub_unpaid",
                  "payment_status":"unpaid","metadata":{"plan":"PRO"}}}}
                """.trim();
        billingWith("price_pro", "price_business").processWebhook(payload, signedHeader(payload));

        verify(subscriptions, never()).save(org.mockito.ArgumentMatchers.any());
        verify(organizations, never()).findById(any());
    }

    @Test
    void paidCheckoutStoresSubscriptionButDefersPlanUntilSubscriptionWebhook() throws Exception {
        when(webhookEvents.existsById("evt_paid_checkout")).thenReturn(false);
        BillingSubscription pending = new BillingSubscription(TENANT);
        when(subscriptions.findById(TENANT)).thenReturn(Optional.of(pending));
        TenantOrganization organization = new TenantOrganization(TENANT, "Acme", "acme");
        when(organizations.findById(TENANT)).thenReturn(Optional.of(organization));

        String checkoutPayload = """
                {"id":"evt_paid_checkout","type":"checkout.session.completed","data":{"object":{
                  "client_reference_id":"00000000-0000-0000-0000-000000000009",
                  "customer":"cus_paid","subscription":"sub_paid",
                  "payment_status":"paid","metadata":{"plan":"PRO"}}}}
                """.trim();
        billingWith("price_pro", "price_business").processWebhook(checkoutPayload, signedHeader(checkoutPayload));

        assertThat(organization.getPlan()).isEqualTo(Plan.FREE);
        assertThat(pending.getStripeCustomerId()).isEqualTo("cus_paid");
        assertThat(pending.getStripeSubscriptionId()).isEqualTo("sub_paid");
        assertThat(pending.getPlan()).isEqualTo(Plan.PRO);
        assertThat(pending.getStatus()).isEqualTo("PENDING");

        when(subscriptions.findByStripeSubscriptionId("sub_paid")).thenReturn(Optional.of(pending));
        String subscriptionPayload = """
                {"id":"evt_paid_active","type":"customer.subscription.updated","data":{"object":{
                  "id":"sub_paid","customer":"cus_paid","status":"active","current_period_end":1893456000}}}
                """.trim();
        when(webhookEvents.existsById("evt_paid_active")).thenReturn(false);
        billingWith("price_pro", "price_business").processWebhook(subscriptionPayload, signedHeader(subscriptionPayload));

        assertThat(organization.getPlan()).isEqualTo(Plan.PRO);
        assertThat(pending.getStatus()).isEqualTo("ACTIVE");
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
                {"id":"evt_trialing","type":"customer.subscription.updated","data":{"object":{
                  "id":"sub_x","customer":"cus_x","status":"trialing","current_period_end":1893456000}}}
                """.trim();
        when(webhookEvents.existsById("evt_trialing")).thenReturn(false);
        billingWith("price_pro", "price_business").processWebhook(payload, signedHeader(payload));

        assertThat(existing.getStatus()).isEqualTo("TRIALING");
        assertThat(existing.getPlan()).isEqualTo(Plan.PRO);
        assertThat(organization.getPlan()).isEqualTo(Plan.PRO);
        verify(webhookEvents).save(org.mockito.ArgumentMatchers.any(BillingWebhookEvent.class));
    }

    @Test
    void duplicateWebhookEventIsIgnored() throws Exception {
        when(webhookEvents.existsById("evt_duplicate")).thenReturn(true);
        String payload = """
                {"id":"evt_duplicate","type":"checkout.session.completed","data":{"object":{
                  "client_reference_id":"00000000-0000-0000-0000-000000000009",
                  "customer":"cus_dup","subscription":"sub_dup",
                  "payment_status":"paid","metadata":{"plan":"PRO"}}}}
                """.trim();
        billingWith("price_pro", "price_business").processWebhook(payload, signedHeader(payload));
        verify(subscriptions, never()).save(org.mockito.ArgumentMatchers.any());
        verify(webhookEvents, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void unknownSubscriptionEventIsIgnored() throws Exception {
        when(subscriptions.findByStripeSubscriptionId("sub_missing")).thenReturn(Optional.empty());
        when(webhookEvents.existsById("evt_missing")).thenReturn(false);

        String payload = """
                {"id":"evt_missing","type":"customer.subscription.deleted","data":{"object":{
                  "id":"sub_missing","customer":"cus_missing","status":"canceled"}}}
                """.trim();
        billingWith("price_pro", "price_business").processWebhook(payload, signedHeader(payload));

        verify(organizations, never()).findById(any());
        verify(webhookEvents).save(org.mockito.ArgumentMatchers.any(BillingWebhookEvent.class));
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
