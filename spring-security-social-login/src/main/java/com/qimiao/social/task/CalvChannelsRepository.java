package com.qimiao.social.task;

import org.springframework.data.repository.ListCrudRepository;


public interface CalvChannelsRepository extends ListCrudRepository<CalvChannelsEntity, Long> {

    CalvChannelsEntity findByClientRegistrationIdAndAccountIdAndCalvId(String Client, String AccountId, String calvId);

}
