package com.qimiao.social.task;

import com.github.f4b6a3.tsid.TsidCreator;
import com.google.api.client.auth.oauth2.BearerToken;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Channel;
import com.microsoft.graph.models.Subscription;
import com.microsoft.graph.serviceclient.GraphServiceClient;
import com.qimiao.social.calendars.SimpleAuthProvider;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
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
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ValueRange;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 第二步: 订阅数据或更新订阅关系
 */
@Slf4j
@Service
public class SubscriptionsTask {
    private static final String APPLICATION_NAME = "NBExampleApplication";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

    @Resource
    OAuth2AuthorizedClientService oAuth2AuthorizedClientService;
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

        List<CalvChannelsEntity> microsofts = calvChannelsEntities.stream()
                .filter(authorizedClient -> authorizedClient.getClientRegistrationId().equals("microsoft"))
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
                    .setAddress("https://82ef-223-213-179-251.ngrok-free.app/notifications/google")
                    .setParams(params)
                    .setExpiration(System.currentTimeMillis() + 7L * 24L * 60L * 60L * 1000L);


            try {
                Channel result = calendar.events().watch("primary", channel).execute();
                if (result != null) {
                    System.out.println("Watch Kind : " + result.getKind());
                    System.out.println("Watch Channel ID: " + result.getId());
                    System.out.println("Watch Resource ID: " + result.getResourceId());

                    calvChannelsEntity.setChannelId(result.getId());
                    calvChannelsEntity.setNotificationUrl("https://82ef-223-213-179-251.ngrok-free.app/notifications/google");
                    calvChannelsEntity.setResourceUri(result.getResourceUri());
                    calvChannelsEntity.setResourceId(result.getResourceId());
                    calvChannelsEntity.setChannelExpiresAt(result.getExpiration());
                    calvChannelsEntity.setRemark(result.getKind());
                    calvChannelsRepository.save(calvChannelsEntity);
                }

            } catch (Exception e) {
                log.error("calendar.events().watch:", e);
            }
        }

        for (CalvChannelsEntity calvChannelsEntity : microsofts) {
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

            SimpleAuthProvider simpleAuthProvider = new SimpleAuthProvider(oAuth2AuthorizedClient.getAccessToken().getTokenValue());
            GraphServiceClient graphClient = new GraphServiceClient(simpleAuthProvider);

            try {
                Subscription subscription = new Subscription();
                subscription.setNotificationUrl("https://82ef-223-213-179-251.ngrok-free.app/notifications/outlook");
                OffsetDateTime expirationDateTime = OffsetDateTime.now(ZoneOffset.UTC);
                subscription.setExpirationDateTime(expirationDateTime.plusHours(24 * 3));
                var result = graphClient.subscriptions().bySubscriptionId(calvChannelsEntity.getChannelId()).patch(subscription);
                if (result != null) {
                    calvChannelsEntity.setChannelId(result.getId());
                    calvChannelsEntity.setNotificationUrl("https://82ef-223-213-179-251.ngrok-free.app/notifications/outlook");
                    calvChannelsEntity.setChannelExpiresAt(Objects.requireNonNull(result.getExpirationDateTime()).toEpochSecond());
                    calvChannelsRepository.save(calvChannelsEntity);
                }

            } catch (Exception e) {
                log.error("calendar.events().watch:", e);
            }

        }

    }


    @EventListener(OAuth2AuthorizedClient.class)
    void init0(OAuth2AuthorizedClient oAuth2AuthorizedClient) {
        if (Objects.requireNonNull(oAuth2AuthorizedClient.getAccessToken().getExpiresAt()).isBefore(Instant.now())) {
            return;
        }

        String principalName = oAuth2AuthorizedClient.getPrincipalName();
        String clientRegistrationId = oAuth2AuthorizedClient.getClientRegistration().getRegistrationId();

        if ("google".equals(clientRegistrationId)) {
            String calvId = "primary";
            CalvChannelsEntity primary = calvChannelsRepository.findByClientRegistrationIdAndPrincipalNameAndCalvId(clientRegistrationId, principalName, calvId);
            if (primary != null) {
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
                    .setAddress("https://82ef-223-213-179-251.ngrok-free.app/notifications/google")
                    .setParams(params)
                    .setExpiration(Instant.now().toEpochMilli() + 7L * 24L * 60L * 60L * 1000L);


            try {
                Channel result = calendar.events().watch("primary", channel).execute();
                if (result != null && result.getId() != null) {
                    System.out.println("Watch Kind : " + result.getKind());
                    System.out.println("Watch Channel ID: " + result.getId());
                    System.out.println("Watch Resource ID: " + result.getResourceId());

                    CalvChannelsEntity calvChannelsEntity = new CalvChannelsEntity();
                    calvChannelsEntity.setId(TsidCreator.getTsid().toLong());
                    calvChannelsEntity.setPrincipalName(principalName);
                    calvChannelsEntity.setCalvId(calvId);
                    calvChannelsEntity.setClientRegistrationId(clientRegistrationId);
                    calvChannelsEntity.setNotificationUrl("https://82ef-223-213-179-251.ngrok-free.app/notifications/google");
                    calvChannelsEntity.setChannelId(result.getId());
                    calvChannelsEntity.setResourceUri(result.getResourceUri());
                    calvChannelsEntity.setResourceId(result.getResourceId());
                    calvChannelsEntity.setChannelExpiresAt(result.getExpiration());
                    calvChannelsEntity.setRemark(result.getKind());
                    calvChannelsRepository.save(calvChannelsEntity);
                }
            } catch (IOException exception) {
                log.error("SubscriptionsTask.google().watch:", exception);
            }

        }

        if ("microsoft".equals(clientRegistrationId)) {
            String calvId = "primary";
            CalvChannelsEntity primary = calvChannelsRepository.findByClientRegistrationIdAndPrincipalNameAndCalvId(clientRegistrationId, principalName, calvId);
            if (primary != null) {
                return;
            }

            SimpleAuthProvider simpleAuthProvider = new SimpleAuthProvider(oAuth2AuthorizedClient.getAccessToken().getTokenValue());
            GraphServiceClient graphClient = new GraphServiceClient(simpleAuthProvider);

            try {
                Subscription subscription = new Subscription();
                subscription.setChangeType("created,updated,deleted");
                subscription.setNotificationUrl("https://82ef-223-213-179-251.ngrok-free.app/notifications/outlook");
                subscription.setResource("me/events");
                OffsetDateTime expirationDateTime = OffsetDateTime.now(ZoneOffset.UTC);
                subscription.setExpirationDateTime(expirationDateTime.plusHours(24 * 3));
                subscription.setClientState(TsidCreator.getTsid().toString());

                Subscription result = graphClient.subscriptions().post(subscription);
                if (result == null) {
                    return;
                }

                CalvChannelsEntity calvChannelsEntity = new CalvChannelsEntity();
                calvChannelsEntity.setId(TsidCreator.getTsid().toLong());
                calvChannelsEntity.setPrincipalName(principalName); // 第三方账号ID
                calvChannelsEntity.setCalvId(calvId);
                calvChannelsEntity.setClientRegistrationId(clientRegistrationId);
                calvChannelsEntity.setNotificationUrl("https://82ef-223-213-179-251.ngrok-free.app/notifications/outlook");
                calvChannelsEntity.setChannelId(result.getId());
                calvChannelsEntity.setResourceUri(result.getResource());
                calvChannelsEntity.setResourceId(result.getNotificationUrlAppId());

                if (result.getExpirationDateTime() != null) {
                    calvChannelsEntity.setChannelExpiresAt(result.getExpirationDateTime().toEpochSecond());
                }
                calvChannelsEntity.setRemark("appClientId:" + result.getApplicationId());

                calvChannelsRepository.save(calvChannelsEntity);
            } catch (RuntimeException exception) {
                log.error("SubscriptionsTask.microsoft().watch:", exception);
            }
        }

    }

}