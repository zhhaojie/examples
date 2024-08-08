package com.qimiao.social.task;

import com.github.f4b6a3.tsid.TsidCreator;
import com.google.api.client.auth.oauth2.BearerToken;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Channel;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 第二步: 订阅数据或更新订阅关系
 */
@Slf4j
@Service
class CalvChannelSubscriptionTask {
    private static final String APPLICATION_NAME = "NBExampleApplication";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

    @Resource
    OAuth2AuthorizedClientService oAuth2AuthorizedClientService;
    @Resource
    CustomOAuth2AuthorizedClientRepository customOAuth2AuthorizedClientRepository;

    @Resource
    CalvChannelsRepository calvChannelsRepository;

    @lombok.SneakyThrows
    @Scheduled(fixedDelay = 10, timeUnit = TimeUnit.SECONDS)
    void renewChannel() {
        long startTime = System.currentTimeMillis();
        log.info("renewChannel started at {}", LocalDateTime.now());

        Pageable pageable = PageRequest.of(0, 100);
        Page<CalvChannelsEntity> resultPage;
        do {
            resultPage = calvChannelsRepository.findAll(pageable);
            renew0(resultPage.getContent());
            pageable = resultPage.nextPageable();
        } while (resultPage.hasNext());

        long endTime = System.currentTimeMillis();
        log.info("renewChannel ended at {}", LocalDateTime.now());
        log.info("renewChannel took {} milliseconds", endTime - startTime);
    }

    void renew0(List<CalvChannelsEntity> calvChannelsEntities) {
        List<CalvChannelsEntity> googles = calvChannelsEntities.stream()
                .filter(authorizedClient -> authorizedClient.getClientRegistrationId().equals("google"))
                .toList();

        List<CalvChannelsEntity> azures = calvChannelsEntities.stream()
                .filter(authorizedClient -> authorizedClient.getClientRegistrationId().equals("azure"))
                .toList();

        for (CalvChannelsEntity calvChannelsEntity : googles) {
            if (calvChannelsEntity == null || calvChannelsEntity.nonExpired()) {
                return;
            }

            // 看看用户的授权关系是否还在
            OAuth2AuthorizedClient oAuth2AuthorizedClient = oAuth2AuthorizedClientService.loadAuthorizedClient(
                    calvChannelsEntity.getClientRegistrationId(), calvChannelsEntity.getPrincipalName());
            if (oAuth2AuthorizedClient == null || oAuth2AuthorizedClient.getAccessToken() == null) {
                return;
            }

            // 如果token已经失效. 下个周期再尝试订阅
            if (Objects.requireNonNull(oAuth2AuthorizedClient.getAccessToken().getExpiresAt()).isBefore(Instant.now())) {
                return;
            }

            Calendar calendar = new Calendar.Builder(
                    new NetHttpTransport(),
                    JSON_FACTORY,
                    new Credential(BearerToken.authorizationHeaderAccessMethod()).setAccessToken(oAuth2AuthorizedClient.getAccessToken().getTokenValue()))
                    .setApplicationName(APPLICATION_NAME)
                    .build();

            Map<String, String> params = new HashMap<>();
            Channel channel = new Channel()
                    .setId(UUID.randomUUID().toString()) // Unique identifier for the channel
                    .setType("web_hook")
                    .setAddress("https://c900-113-104-188-94.ngrok-free.app/notifications/google")
                    .setParams(params)
                    .setExpiration(System.currentTimeMillis() + 7L * 24L * 60L * 60L * 1000L);


            try {
                Channel channel1 = calendar.events().watch("primary", channel).execute();
                if (channel1 != null && channel1.getId() != null) {
                    System.out.println("Watch Kind : " + channel1.getKind());
                    System.out.println("Watch Channel ID: " + channel1.getId());
                    System.out.println("Watch Resource ID: " + channel1.getResourceId());

                    calvChannelsEntity.setChannelId(channel1.getId());
                    calvChannelsEntity.setAddress("https://c900-113-104-188-94.ngrok-free.app/notifications/google");
                    calvChannelsEntity.setResourceUri(channel1.getResourceUri());
                    calvChannelsEntity.setResourceId(channel1.getResourceId());
                    calvChannelsEntity.setChannelExpiresAt(channel1.getExpiration());
                    calvChannelsEntity.setRemark(channel1.getKind());
                    calvChannelsRepository.save(calvChannelsEntity);
                }

            } catch (Exception e) {
                log.error("calendar.events().watch:", e);
            }
        }
    }

    @EventListener(ApplicationReadyEvent.class)
    void initChannel() {
        long startTime = System.currentTimeMillis();
        log.info("initChannel started at {}", LocalDateTime.now());

        Pageable pageable = PageRequest.of(0, 100);
        Page<OAuth2AuthorizedClientEntity> resultPage;
        do {
            resultPage = customOAuth2AuthorizedClientRepository.findAll(pageable);
            init0(resultPage.getContent());
            pageable = resultPage.nextPageable();
        } while (resultPage.hasNext());

        long endTime = System.currentTimeMillis();
        log.info("initChannel ended at {}", LocalDateTime.now());
        log.info("initChannel took {} milliseconds", endTime - startTime);

    }

    void init0(List<OAuth2AuthorizedClientEntity> auth2AuthorizedClientEntities) {
        List<OAuth2AuthorizedClientEntity> googles = auth2AuthorizedClientEntities.stream()
                .filter(authorizedClient -> authorizedClient.getPrincipal().getClientRegistrationId().equals("google"))
                .toList();

        List<OAuth2AuthorizedClientEntity> azures = auth2AuthorizedClientEntities.stream()
                .filter(authorizedClient -> authorizedClient.getPrincipal().getClientRegistrationId().equals("azure"))
                .toList();

        for (OAuth2AuthorizedClientEntity authorizedClient : googles) {
            if (authorizedClient.getAccessTokenExpiresAt().isBefore(Instant.now())) {
                return;
            }

            String principalName = authorizedClient.getPrincipal().getPrincipalName();
            String clientRegistrationId = authorizedClient.getPrincipal().getClientRegistrationId();
            String calvId = "primary";

            CalvChannelsEntity primary = calvChannelsRepository.findByClientRegistrationIdAndPrincipalNameAndCalvId(clientRegistrationId, principalName, calvId);
            if (primary != null) {
                return;
            }

            CalvChannelsEntity calvChannelsEntity = new CalvChannelsEntity();
            calvChannelsEntity.setId(TsidCreator.getTsid().toLong());
            calvChannelsEntity.setPrincipalName(principalName);
            calvChannelsEntity.setCalvId(calvId);
            calvChannelsEntity.setClientRegistrationId(clientRegistrationId);


            Calendar calendar = new Calendar.Builder(
                    new NetHttpTransport(),
                    JSON_FACTORY,
                    new Credential(BearerToken.authorizationHeaderAccessMethod()).setAccessToken(authorizedClient.getAccessTokenValue()))
                    .setApplicationName(APPLICATION_NAME)
                    .build();

            Map<String, String> params = new HashMap<>();
            Channel channel = new Channel()
                    .setId(UUID.randomUUID().toString()) // Unique identifier for the channel
                    .setType("web_hook")
                    .setAddress("https://c900-113-104-188-94.ngrok-free.app/notifications/google")
                    .setParams(params)
                    .setExpiration(System.currentTimeMillis() + 7L * 24L * 60L * 60L * 1000L);


            try {
                Channel channel1 = calendar.events().watch("primary", channel).execute();
                if (channel1 != null && channel1.getId() != null) {
                    System.out.println("Watch Kind : " + channel1.getKind());
                    System.out.println("Watch Channel ID: " + channel1.getId());
                    System.out.println("Watch Resource ID: " + channel1.getResourceId());

                    calvChannelsEntity.setChannelId(channel1.getId());
                    calvChannelsEntity.setAddress("https://c900-113-104-188-94.ngrok-free.app/notifications/google");
                    calvChannelsEntity.setResourceUri(channel1.getResourceUri());
                    calvChannelsEntity.setResourceId(channel1.getResourceId());
                    calvChannelsEntity.setChannelExpiresAt(channel1.getExpiration());
                    calvChannelsEntity.setRemark(channel1.getKind());
                    calvChannelsRepository.save(calvChannelsEntity);
                }
            } catch (IOException e) {
                log.error("calendar.events().watch:", e);
            }
        }
    }
}