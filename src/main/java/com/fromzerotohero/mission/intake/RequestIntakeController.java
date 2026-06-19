package com.fromzerotohero.mission.intake;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@Tag(name = "Request intake", description = "Public request collection and tenant-scoped request review")
public class RequestIntakeController {
    private final RequestIntakeService service;

    public RequestIntakeController(RequestIntakeService service) { this.service = service; }

    @GetMapping("/public/intake/{organizationSlug}")
    @Operation(security = {})
    public RequestIntakeService.Portal portal(@PathVariable String organizationSlug) {
        return service.portal(organizationSlug);
    }

    @PostMapping("/public/intake/{organizationSlug}")
    @Operation(security = {})
    public ResponseEntity<RequestIntakeService.Receipt> submit(@PathVariable String organizationSlug,
            @Valid @RequestBody RequestIntakeService.IntakeRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        RequestIntakeService.Receipt receipt = service.submit(organizationSlug, request, idempotencyKey);
        URI location = URI.create("/api/requests/" + receipt.requestId());
        return ResponseEntity.status(receipt.replayed() ? HttpStatus.OK : HttpStatus.CREATED)
                .location(location).body(receipt);
    }

    @GetMapping("/requests")
    @SecurityRequirement(name = "bearer-jwt")
    public List<RequestSubmission> requests() { return service.currentRequests(); }

    @GetMapping("/requests/{requestId}")
    @SecurityRequirement(name = "bearer-jwt")
    public RequestSubmission request(@PathVariable java.util.UUID requestId) {
        return service.currentRequest(requestId);
    }
}
