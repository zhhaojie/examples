package com.qimiao.social.task;

import org.springframework.data.jpa.repository.JpaRepository;


public interface SubscriptionsRepository extends JpaRepository<SubscriptionsEntity, Long> {
    // 删除日历时. 把订阅关系删掉
    void deleteAllByClientRegistrationIdAndCalvId(String clientRegistrationId, String calvId);

    SubscriptionsEntity findBySubscriptionIdAndResourceId(String subscriptionId, String resourceId);

    @Deprecated
    SubscriptionsEntity findByClientRegistrationIdAndCalvIdAndPrincipalName(String registrationId, String calvId, String principalName);
}
