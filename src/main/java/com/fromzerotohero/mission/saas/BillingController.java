package com.fromzerotohero.mission.saas;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/billing")
public class BillingController {
    private final BillingService service;
    public BillingController(BillingService service) { this.service = service; }

    @PostMapping("/checkout")
    public BillingService.CheckoutResponse checkout(@Valid @RequestBody BillingService.CheckoutRequest request) {
        return service.createCheckout(request);
    }

    @PostMapping("/webhook")
    public ResponseEntity<Void> webhook(@RequestBody String payload,
            @RequestHeader("Stripe-Signature") String signature) {
        service.processWebhook(payload, signature);
        return ResponseEntity.ok().build();
    }
}
