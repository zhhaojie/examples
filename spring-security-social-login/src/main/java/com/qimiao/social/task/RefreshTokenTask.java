package com.qimiao.social.task;

import jakarta.annotation.Resource;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.stereotype.Service;

@Service
public class RefreshTokenTask {

    @Resource
    OAuth2AuthorizedClientService oAuth2AuthorizedClientService;

  //  @Scheduled(initialDelay = 1, fixedDelay = 3, timeUnit = TimeUnit.SECONDS)
    void doRefresh() {
        OAuth2AuthorizedClient oAuth2AuthorizedClient = oAuth2AuthorizedClientService.loadAuthorizedClient("google", "115152964495372047642");
        System.out.println(oAuth2AuthorizedClient.getPrincipalName());
        System.out.println(oAuth2AuthorizedClient.getAccessToken().getTokenValue());
    }
}
