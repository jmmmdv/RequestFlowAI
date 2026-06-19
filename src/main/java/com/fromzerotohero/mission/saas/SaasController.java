package com.fromzerotohero.mission.saas;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/saas")
@Tag(name = "SaaS administration", description = "Organization onboarding, membership, quota, and billing state")
@SecurityRequirement(name = "bearer-jwt")
public class SaasController {
    private final SaasService service;
    public SaasController(SaasService service) { this.service = service; }

    @GetMapping("/organization") public SaasService.OrganizationOverview organization() { return service.current(); }
    @PatchMapping("/organization") public SaasService.OrganizationOverview rename(
            @Valid @RequestBody SaasService.UpdateOrganizationRequest request) { return service.rename(request); }
    @GetMapping("/members") public List<TenantMembership> members() { return service.members(); }
    @PostMapping("/invitations") public SaasService.InvitationCreated invite(
            @Valid @RequestBody SaasService.InviteMemberRequest request) { return service.invite(request); }
    @GetMapping("/invitations") public List<TenantInvitation> invitations() { return service.invitations(); }
    @PostMapping("/invitations/accept") public SaasService.OrganizationOverview accept(
            @Valid @RequestBody SaasService.AcceptInvitationRequest request) { return service.accept(request); }
}
