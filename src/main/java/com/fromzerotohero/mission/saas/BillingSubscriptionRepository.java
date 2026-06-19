package com.fromzerotohero.mission.saas;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BillingSubscriptionRepository extends JpaRepository<BillingSubscription, UUID> {
    Optional<BillingSubscription> findByStripeSubscriptionId(String stripeSubscriptionId);
}
