package com.fromzerotohero.mission.saas;

import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fromzerotohero.mission.agent.AgentRunRepository;
import com.fromzerotohero.mission.workitem.Priority;
import com.fromzerotohero.mission.workitem.WorkItem;
import com.fromzerotohero.mission.workitem.WorkItemRepository;
import com.fromzerotohero.mission.workitem.WorkStatus;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest(properties = {
        "mission.seed.enabled=false",
        "mission.billing.stripe.webhook-secret=whsec_test_portfolio"
})
@AutoConfigureMockMvc
class SaasProductIntegrationTest {
    private static final UUID TENANT = UUID.fromString("00000000-0000-0000-0000-000000000001");
    @Autowired MockMvc mvc;
    @Autowired WorkItemRepository workItems;
    @Autowired AgentRunRepository agentRuns;
    @Autowired TenantMembershipRepository memberships;
    @Autowired TenantInvitationRepository invitations;
    @Autowired BillingSubscriptionRepository subscriptions;
    @Autowired TenantOrganizationRepository organizations;
    @Autowired ObjectMapper objectMapper;

    @BeforeEach
    void cleanProductState() {
        agentRuns.deleteAll(); workItems.deleteAll(); invitations.deleteAll(); memberships.deleteAll();
        subscriptions.deleteAll();
        organizations.findAll().stream().filter(organization -> !organization.getId().equals(TENANT))
                .forEach(organizations::delete);
        BillingSubscription subscription = new BillingSubscription(TENANT);
        subscription.synchronize(null, null, Plan.FREE, "ACTIVE", null);
        subscriptions.save(subscription);
        TenantOrganization organization = organizations.findById(TENANT).orElseThrow();
        organization.rename("Local Development", "local");
        organization.changePlan(Plan.FREE);
        organizations.save(organization);
    }

    @Test
    void bootstrapsOrganizationMembershipAndReportsQuotaUsage() throws Exception {
        mvc.perform(get("/api/saas/organization"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Local Development"))
                .andExpect(jsonPath("$.currentUserRole").value("ADMIN"))
                .andExpect(jsonPath("$.usage.plan").value("FREE"))
                .andExpect(jsonPath("$.usage.workItemsLimit").value(25))
                .andExpect(jsonPath("$.usage.agentRunsLimit").value(10));
        org.assertj.core.api.Assertions.assertThat(memberships.findAllByTenantIdOrderByJoinedAt(TENANT)).hasSize(1);
    }

    @Test
    void adminCanRenameOrganizationAndCreateExpiringInvitation() throws Exception {
        mvc.perform(patch("/api/saas/organization").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Launch Team\",\"slug\":\"launch-team\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.slug").value("launch-team"));
        mvc.perform(post("/api/saas/invitations").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"new.member@example.com\",\"role\":\"MEMBER\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("new.member@example.com"))
                .andExpect(jsonPath("$.token", matchesPattern("[A-Za-z0-9_-]{40,}")));
    }

    @Test
    void freePlanRejectsWorkBeyondItsCapacity() throws Exception {
        for (int index = 0; index < Plan.FREE.workItemLimit(); index++) {
            workItems.save(new WorkItem("Quota " + index, null, Priority.LOW, WorkStatus.BACKLOG, TENANT));
        }
        mvc.perform(post("/api/work-items").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"One too many\",\"priority\":\"LOW\",\"status\":\"BACKLOG\"}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.title").value("Plan quota exceeded"));
    }

    @Test
    void signedStripeCheckoutWebhookActivatesProPlan() throws Exception {
        long timestamp = Instant.now().getEpochSecond();
        String checkoutPayload = """
                {"id":"evt_checkout_01","type":"checkout.session.completed","data":{"object":{
                  "client_reference_id":"00000000-0000-0000-0000-000000000001",
                  "customer":"cus_test","subscription":"sub_test",
                  "payment_status":"paid",
                  "metadata":{"plan":"PRO"}
                }}}
                """.trim();
        mvc.perform(post("/api/billing/webhook").contentType(MediaType.APPLICATION_JSON)
                        .header("Stripe-Signature", "t=" + timestamp + ",v1=" + sign(timestamp + "." + checkoutPayload))
                        .content(checkoutPayload))
                .andExpect(status().isOk());
        org.assertj.core.api.Assertions.assertThat(organizations.findById(TENANT).orElseThrow().getPlan())
                .isEqualTo(Plan.FREE);

        String subscriptionPayload = """
                {"id":"evt_checkout_01_active","type":"customer.subscription.updated","data":{"object":{
                  "id":"sub_test","customer":"cus_test","status":"active","current_period_end":1893456000}}}
                """.trim();
        long activeTimestamp = Instant.now().getEpochSecond();
        mvc.perform(post("/api/billing/webhook").contentType(MediaType.APPLICATION_JSON)
                        .header("Stripe-Signature",
                                "t=" + activeTimestamp + ",v1=" + sign(activeTimestamp + "." + subscriptionPayload))
                        .content(subscriptionPayload))
                .andExpect(status().isOk());
        org.assertj.core.api.Assertions.assertThat(organizations.findById(TENANT).orElseThrow().getPlan())
                .isEqualTo(Plan.PRO);
        org.assertj.core.api.Assertions.assertThat(subscriptions.findById(TENANT).orElseThrow().getStripeCustomerId())
                .isEqualTo("cus_test");
    }

    @Test
    void unpaidCheckoutWebhookDoesNotActivatePlan() throws Exception {
        signedWebhook("""
                {"id":"evt_checkout_unpaid","type":"checkout.session.completed","data":{"object":{
                  "client_reference_id":"00000000-0000-0000-0000-000000000001",
                  "customer":"cus_unpaid","subscription":"sub_unpaid",
                  "payment_status":"unpaid","metadata":{"plan":"PRO"}}}}
                """).andExpect(status().isOk());
        org.assertj.core.api.Assertions.assertThat(organizations.findById(TENANT).orElseThrow().getPlan())
                .isEqualTo(Plan.FREE);
        org.assertj.core.api.Assertions.assertThat(subscriptions.findById(TENANT).orElseThrow().getStripeCustomerId())
                .isNull();
    }

    @Test
    void rejectsWebhookWithInvalidSignature() throws Exception {
        mvc.perform(post("/api/billing/webhook").contentType(MediaType.APPLICATION_JSON)
                        .header("Stripe-Signature", "t=" + Instant.now().getEpochSecond() + ",v1=bad")
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void invitationCanBeListedAcceptedOnceAndCreatesMembership() throws Exception {
        String response = mvc.perform(post("/api/saas/invitations").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"developer@local.test\",\"role\":\"VIEWER\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String token = objectMapper.readTree(response).path("token").asText();

        mvc.perform(get("/api/saas/invitations")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email").value("developer@local.test"));
        mvc.perform(post("/api/saas/invitations/accept").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + token + "\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.currentUserRole").value("VIEWER"));
        mvc.perform(get("/api/saas/members")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email").value("developer@local.test"));
        mvc.perform(post("/api/saas/invitations/accept").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + token + "\"}"))
                .andExpect(status().isGone());
    }

    @Test
    void organizationSlugMustBeUnique() throws Exception {
        organizations.save(new TenantOrganization(UUID.randomUUID(), "Other", "already-used"));
        mvc.perform(patch("/api/saas/organization").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Conflict\",\"slug\":\"already-used\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void duplicateStripeWebhookEventIsIgnored() throws Exception {
        String payload = """
                {"id":"evt_duplicate_integration","type":"checkout.session.completed","data":{"object":{
                  "client_reference_id":"00000000-0000-0000-0000-000000000001",
                  "customer":"cus_dup","subscription":"sub_dup",
                  "payment_status":"paid","metadata":{"plan":"PRO"}}}}
                """.trim();
        signedWebhook(payload).andExpect(status().isOk());
        signedWebhook("""
                {"id":"evt_duplicate_integration_active","type":"customer.subscription.updated","data":{"object":{
                  "id":"sub_dup","customer":"cus_dup","status":"active","current_period_end":1893456000}}}
                """).andExpect(status().isOk());
        org.assertj.core.api.Assertions.assertThat(organizations.findById(TENANT).orElseThrow().getPlan())
                .isEqualTo(Plan.PRO);
        signedWebhook(payload).andExpect(status().isOk());
        org.assertj.core.api.Assertions.assertThat(subscriptions.findById(TENANT).orElseThrow().getStripeCustomerId())
                .isEqualTo("cus_dup");
    }

    @Test
    void subscriptionLifecycleFallsBackToFreeWhenCancelled() throws Exception {
        signedWebhook("""
                {"id":"evt_lifecycle_checkout","type":"checkout.session.completed","data":{"object":{
                  "client_reference_id":"00000000-0000-0000-0000-000000000001",
                  "customer":"cus_lifecycle","subscription":"sub_lifecycle",
                  "payment_status":"paid","metadata":{"plan":"PRO"}}}}
                """).andExpect(status().isOk());
        signedWebhook("""
                {"id":"evt_lifecycle_active","type":"customer.subscription.updated","data":{"object":{
                  "id":"sub_lifecycle","customer":"cus_lifecycle","status":"active",
                  "current_period_end":1893456000}}}
                """).andExpect(status().isOk());
        org.assertj.core.api.Assertions.assertThat(subscriptions.findById(TENANT).orElseThrow().getStatus())
                .isEqualTo("ACTIVE");

        signedWebhook("""
                {"id":"evt_lifecycle_cancel","type":"customer.subscription.deleted","data":{"object":{
                  "id":"sub_lifecycle","customer":"cus_lifecycle","status":"canceled"}}}
                """).andExpect(status().isOk());
        org.assertj.core.api.Assertions.assertThat(organizations.findById(TENANT).orElseThrow().getPlan())
                .isEqualTo(Plan.FREE);
    }

    @Test
    void rejectsStaleAndMalformedSignedWebhooks() throws Exception {
        long stale = Instant.now().minusSeconds(301).getEpochSecond();
        mvc.perform(post("/api/billing/webhook").contentType(MediaType.APPLICATION_JSON)
                        .header("Stripe-Signature", "t=" + stale + ",v1=" + sign(stale + ".{}"))
                        .content("{}"))
                .andExpect(status().isBadRequest());
        signedWebhook("not-json").andExpect(status().isBadRequest());
    }

    @Test
    void checkoutExplainsMissingStripeConfiguration() throws Exception {
        mvc.perform(post("/api/billing/checkout").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"plan\":\"PRO\",\"idempotencyKey\":\"checkout-test-01\"}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.detail").value("Stripe secret key is not configured"));
    }

    private org.springframework.test.web.servlet.ResultActions signedWebhook(String payload) throws Exception {
        String compact = payload.trim();
        long timestamp = Instant.now().getEpochSecond();
        return mvc.perform(post("/api/billing/webhook").contentType(MediaType.APPLICATION_JSON)
                .header("Stripe-Signature", "t=" + timestamp + ",v1=" + sign(timestamp + "." + compact))
                .content(compact));
    }

    private String sign(String value) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec("whsec_test_portfolio".getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
    }
}
