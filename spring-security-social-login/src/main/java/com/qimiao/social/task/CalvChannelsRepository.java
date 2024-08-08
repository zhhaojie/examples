package com.qimiao.social.task;

import org.springframework.data.jpa.repository.JpaRepository;


public interface CalvChannelsRepository extends JpaRepository<CalvChannelsEntity, Long> {

    CalvChannelsEntity findByClientRegistrationIdAndPrincipalNameAndCalvId(String client, String account, String calvId);

    CalvChannelsEntity findByChannelIdAndResourceId(String channelId, String resourceId);
}
