package com.qimiao.social.task;

import org.springframework.data.repository.ListCrudRepository;


public interface CalvChannelsRepository extends ListCrudRepository<CalvChannelsEntity, Long> {

    CalvChannelsEntity findByClientRegistrationIdAndPrincipalNameAndCalvId(String client, String account, String calvId);

    CalvChannelsEntity findByChannelIdAndResourceId(String channelId, String resourceId);
}
