package com.qimiao.social.task;

import org.springframework.data.repository.ListCrudRepository;


public interface ChannelCalendarListRepository extends ListCrudRepository<ChannelCalendarList, Long> {
    ChannelCalendarList findByClientRegistrationIdAndCalvId(String clientRegistrationId, String calvId);

    void deleteByClientRegistrationIdAndCalvId(String clientRegistrationId, String calvId);
}
