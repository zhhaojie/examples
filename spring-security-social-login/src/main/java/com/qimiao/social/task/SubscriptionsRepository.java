package com.qimiao.social.task;

import org.springframework.data.jpa.repository.JpaRepository;


public interface SubscriptionsRepository extends JpaRepository<SubscriptionsEntity, Long> {

    SubscriptionsEntity findBySubscriptionIdAndResourceId(String subscriptionId, String resourceId);

    SubscriptionsEntity findByClientRegistrationIdAndCalvIdAndPrincipalName(String registrationId, String calvId, String principalName);
}
