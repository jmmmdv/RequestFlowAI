package com.fromzerotohero.mission.saas;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fromzerotohero.mission.security.TenantContext;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.beans.factory.annotation.Autowired;

@Service
public class BillingService {
    private static final Set<String> SETTLED_CHECKOUT_PAYMENT_STATUSES = Set.of("paid", "no_payment_required");
    private static final Set<String> ENTITLED_SUBSCRIPTION_STATUSES = Set.of("ACTIVE", "TRIALING");

    private final BillingSubscriptionRepository subscriptions;
    private final BillingWebhookEventRepository webhookEvents;
    private final TenantOrganizationRepository organizations;
    private final TenantContext tenantContext;
    private final ObjectMapper objectMapper;
    private final RestClient stripe;
    private final String secretKey;
    private final String webhookSecret;
    private final String proPriceId;
    private final String businessPriceId;
    private final String frontendUrl;
    private final Clock clock;

    @Autowired
    public BillingService(BillingSubscriptionRepository subscriptions,
            BillingWebhookEventRepository webhookEvents, TenantOrganizationRepository organizations,
            TenantContext tenantContext, ObjectMapper objectMapper, RestClient.Builder restClientBuilder,
            @Value("${mission.billing.stripe.secret-key:}") String secretKey,
            @Value("${mission.billing.stripe.webhook-secret:}") String webhookSecret,
            @Value("${mission.billing.stripe.pro-price-id:}") String proPriceId,
            @Value("${mission.billing.stripe.business-price-id:}") String businessPriceId,
            @Value("${mission.frontend-url:http://localhost:8080}") String frontendUrl) {
        this(subscriptions, webhookEvents, organizations, tenantContext, objectMapper,
                restClientBuilder.baseUrl("https://api.stripe.com/v1").build(), secretKey,
                webhookSecret, proPriceId, businessPriceId, frontendUrl, Clock.systemUTC());
    }

    BillingService(BillingSubscriptionRepository subscriptions, BillingWebhookEventRepository webhookEvents,
            TenantOrganizationRepository organizations, TenantContext tenantContext,
            ObjectMapper objectMapper, RestClient stripe, String secretKey, String webhookSecret,
            String proPriceId, String businessPriceId, String frontendUrl, Clock clock) {
        this.subscriptions = subscriptions;
        this.webhookEvents = webhookEvents;
        this.organizations = organizations;
        this.tenantContext = tenantContext; this.objectMapper = objectMapper; this.stripe = stripe;
        this.secretKey = secretKey; this.webhookSecret = webhookSecret; this.proPriceId = proPriceId;
        this.businessPriceId = businessPriceId; this.frontendUrl = frontendUrl.replaceAll("/$", "");
        this.clock = clock;
    }

    public CheckoutResponse createCheckout(CheckoutRequest request) {
        requireConfigured(secretKey, "Stripe secret key");
        String priceId = switch (request.plan()) {
            case PRO -> proPriceId;
            case BUSINESS -> businessPriceId;
            case FREE -> throw new SaasException(HttpStatus.BAD_REQUEST, "FREE does not require checkout");
        };
        requireConfigured(priceId, request.plan() + " Stripe price");
        UUID tenantId = tenantContext.tenantId();
        BillingSubscription existing = subscriptions.findById(tenantId).orElse(null);
        if (existing != null && hasEntitledStripeSubscription(existing)) {
            throw new SaasException(HttpStatus.CONFLICT,
                    "An active Stripe subscription already exists for this organization. "
                            + "Manage plan changes through the billing portal.");
        }
        var form = new LinkedMultiValueMap<String, String>();
        form.add("mode", "subscription");
        form.add("line_items[0][price]", priceId);
        form.add("line_items[0][quantity]", "1");
        form.add("success_url", frontendUrl + "/?billing=success&session_id={CHECKOUT_SESSION_ID}");
        form.add("cancel_url", frontendUrl + "/?billing=cancelled");
        form.add("client_reference_id", tenantId.toString());
        form.add("metadata[tenant_id]", tenantId.toString());
        form.add("metadata[plan]", request.plan().name());
        JsonNode response = stripe.post().uri("/checkout/sessions")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + secretKey)
                .header("Idempotency-Key", request.idempotencyKey())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED).body(form).retrieve().body(JsonNode.class);
        if (response == null || response.path("url").isMissingNode()) {
            throw new SaasException(HttpStatus.BAD_GATEWAY, "Stripe did not return a checkout URL");
        }
        return new CheckoutResponse(response.path("id").asText(), response.path("url").asText());
    }

    @Transactional
    public void processWebhook(String payload, String signature) {
        requireConfigured(webhookSecret, "Stripe webhook secret");
        verifySignature(payload, signature);
        try {
            JsonNode event = objectMapper.readTree(payload);
            String eventId = event.path("id").asText(null);
            if (eventId == null || eventId.isBlank()) {
                throw new SaasException(HttpStatus.BAD_REQUEST, "Stripe event id is required");
            }
            if (webhookEvents.existsById(eventId)) {
                return;
            }
            String type = event.path("type").asText();
            JsonNode object = event.path("data").path("object");
            if (type.equals("checkout.session.completed")) synchronizeCheckout(object);
            if (type.startsWith("customer.subscription.")) synchronizeSubscription(object);
            webhookEvents.save(new BillingWebhookEvent(eventId, clock.instant()));
        } catch (JsonProcessingException exception) {
            throw new SaasException(HttpStatus.BAD_REQUEST, "Stripe webhook payload is not valid JSON");
        }
    }

    private void synchronizeCheckout(JsonNode object) {
        String paymentStatus = text(object, "payment_status");
        if (paymentStatus == null
                || !SETTLED_CHECKOUT_PAYMENT_STATUSES.contains(paymentStatus.toLowerCase(Locale.ROOT))) {
            return;
        }
        UUID tenantId = parseTenant(object.path("client_reference_id").asText());
        Plan plan = parsePlan(object.path("metadata").path("plan").asText());
        BillingSubscription subscription = subscriptions.findById(tenantId)
                .orElseGet(() -> new BillingSubscription(tenantId));
        subscription.synchronize(text(object, "customer"), text(object, "subscription"), plan,
                "PENDING", null);
        subscriptions.save(subscription);
    }

    private void synchronizeSubscription(JsonNode object) {
        String subscriptionId = text(object, "id");
        BillingSubscription subscription = subscriptions.findByStripeSubscriptionId(subscriptionId).orElse(null);
        if (subscription == null) return;
        String status = text(object, "status").toUpperCase(Locale.ROOT);
        Plan effectivePlan = ENTITLED_SUBSCRIPTION_STATUSES.contains(status) ? subscription.getPlan() : Plan.FREE;
        Instant periodEnd = object.path("current_period_end").canConvertToLong()
                ? Instant.ofEpochSecond(object.path("current_period_end").asLong()) : null;
        subscription.synchronize(text(object, "customer"), subscriptionId, effectivePlan, status, periodEnd);
        subscriptions.save(subscription);
        organizations.findById(subscription.getTenantId()).ifPresent(organization -> organization.changePlan(effectivePlan));
    }

    private boolean hasEntitledStripeSubscription(BillingSubscription subscription) {
        if (subscription.getStripeSubscriptionId() == null || subscription.getStripeSubscriptionId().isBlank()) {
            return false;
        }
        return ENTITLED_SUBSCRIPTION_STATUSES.contains(subscription.getStatus());
    }

    private void verifySignature(String payload, String header) {
        if (header == null) throw new SaasException(HttpStatus.BAD_REQUEST, "Stripe-Signature is required");
        String timestamp = null; String received = null;
        for (String part : header.split(",")) {
            String[] pair = part.split("=", 2);
            if (pair.length == 2 && pair[0].equals("t")) timestamp = pair[1];
            if (pair.length == 2 && pair[0].equals("v1")) received = pair[1];
        }
        if (timestamp == null || received == null) throw new SaasException(HttpStatus.BAD_REQUEST, "Stripe signature is malformed");
        long signedAt;
        try { signedAt = Long.parseLong(timestamp); }
        catch (NumberFormatException exception) { throw new SaasException(HttpStatus.BAD_REQUEST, "Stripe timestamp is malformed"); }
        if (Math.abs(clock.instant().getEpochSecond() - signedAt) > 300) {
            throw new SaasException(HttpStatus.BAD_REQUEST, "Stripe signature timestamp is outside tolerance");
        }
        String expected = hmac(timestamp + "." + payload);
        if (!MessageDigestSupport.constantTimeHexEquals(expected, received)) {
            throw new SaasException(HttpStatus.BAD_REQUEST, "Stripe signature verification failed");
        }
    }

    private String hmac(String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.GeneralSecurityException exception) { throw new IllegalStateException(exception); }
    }

    private UUID parseTenant(String value) {
        try { return UUID.fromString(value); }
        catch (RuntimeException exception) { throw new SaasException(HttpStatus.BAD_REQUEST, "Stripe event has no valid tenant reference"); }
    }
    private Plan parsePlan(String value) {
        try { return Plan.valueOf(value); }
        catch (RuntimeException exception) { throw new SaasException(HttpStatus.BAD_REQUEST, "Stripe event has no valid plan"); }
    }
    private String text(JsonNode node, String field) { return node.path(field).isNull() ? null : node.path(field).asText(null); }
    private void requireConfigured(String value, String label) {
        if (value == null || value.isBlank()) throw new SaasException(HttpStatus.SERVICE_UNAVAILABLE, label + " is not configured");
    }

    public record CheckoutRequest(@jakarta.validation.constraints.NotNull Plan plan,
            @jakarta.validation.constraints.NotBlank
            @jakarta.validation.constraints.Pattern(regexp = "[A-Za-z0-9._:-]{8,80}") String idempotencyKey) {}
    public record CheckoutResponse(String sessionId, String checkoutUrl) {}

    private static final class MessageDigestSupport {
        static boolean constantTimeHexEquals(String left, String right) {
            return java.security.MessageDigest.isEqual(left.getBytes(StandardCharsets.US_ASCII),
                    right.getBytes(StandardCharsets.US_ASCII));
        }
    }
}
