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
import com.microsoft.graph.models.SubscriptionCollectionResponse;
import com.microsoft.graph.models.Video;
import com.microsoft.graph.serviceclient.GraphServiceClient;
import com.qimiao.social.calendars.Apps;
import com.qimiao.social.calendars.SimpleGraphAuthProvider;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * 第二步: 订阅数据或更新订阅关系
 * 一般来说, google这个订阅关系7天过期. 微软订阅关系3天过期
 */
@Slf4j
@Service
public class SubscriptionsTask {
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

    @Resource
    OAuth2AuthorizedClientService oAuth2AuthorizedClientService;
    @Resource
    CalvChannelsRepository calvChannelsRepository;

    @lombok.SneakyThrows
    @Scheduled(fixedDelay = 10, timeUnit = TimeUnit.MINUTES)
    void renewChannel() {
        long startTime = System.currentTimeMillis();
        log.info("renewChannel started at {}", LocalDateTime.now());

        Pageable pageable = PageRequest.of(0, 100);
        Page<CalvChannelsEntity> resultPage;
        do {
            resultPage = calvChannelsRepository.findAll(pageable);
            renewChannels(resultPage.getContent());
            pageable = resultPage.nextPageable();
        } while (resultPage.hasNext());

        long endTime = System.currentTimeMillis();
        log.info("renewChannel ended at {}", LocalDateTime.now());
        log.info("renewChannel took {} milliseconds", endTime - startTime);
    }

    void renewChannels(List<CalvChannelsEntity> channels) {
        channels.forEach(this::renewSingleChannel);
    }

    void renewSingleChannel(CalvChannelsEntity channelEntity) {
        if (channelEntity == null || channelEntity.nonExpired()) {
            return;
        }

        OAuth2AuthorizedClient oAuth2AuthorizedClient = oAuth2AuthorizedClientService.loadAuthorizedClient(
                channelEntity.getClientRegistrationId(), channelEntity.getPrincipalName());
        if (oAuth2AuthorizedClient == null || oAuth2AuthorizedClient.getAccessToken() == null) {
            return;
        }

        // 如果token已经失效. 下个周期再尝试订阅
        if (Objects.requireNonNull(oAuth2AuthorizedClient.getAccessToken().getExpiresAt()).isBefore(Instant.now())) {
            return;
        }

        if ("google".equals(channelEntity.getClientRegistrationId())) {
            renewGoogleChannel(channelEntity, oAuth2AuthorizedClient);
        } else if ("microsoft".equals(channelEntity.getClientRegistrationId())) {
            renewOutlookChannel(channelEntity, oAuth2AuthorizedClient);
        }
    }

    void renewGoogleChannel(CalvChannelsEntity channelEntity, OAuth2AuthorizedClient oAuth2AuthorizedClient) {

        Calendar calendar = new Calendar.Builder(
                new NetHttpTransport(),
                JSON_FACTORY,
                new Credential(BearerToken.authorizationHeaderAccessMethod()).setAccessToken(oAuth2AuthorizedClient.getAccessToken().getTokenValue()))
                .setApplicationName(Apps.Google.APPLICATION_NAME)
                .build();

        Map<String, String> params = new HashMap<>();
        Channel channel = new Channel()
                .setId(TsidCreator.getTsid().toString())
                .setType("web_hook")
                .setAddress(Apps.Google.CALL_BACK_URL)
                .setParams(params)
                .setExpiration(System.currentTimeMillis() + 7L * 24L * 60L * 60L * 1000L);


        try {
            Channel result = calendar.events().watch("primary", channel).execute();
            if (result != null) {
                System.out.println("Watch Kind : " + result.getKind());
                System.out.println("Watch Channel ID: " + result.getId());
                System.out.println("Watch Resource ID: " + result.getResourceId());

                channelEntity.setChannelId(result.getId());
                channelEntity.setNotificationUrl(Apps.Google.CALL_BACK_URL);
                channelEntity.setResourceUri(result.getResourceUri());
                channelEntity.setResourceId(result.getResourceId());
                channelEntity.setChannelExpiresAt(result.getExpiration());
                channelEntity.setRemark(result.getKind());
                calvChannelsRepository.save(channelEntity);
            }

        } catch (Exception e) {
            log.error("renewGoogleChannel:", e);
        }
    }

    void renewOutlookChannel(CalvChannelsEntity channelEntity, OAuth2AuthorizedClient oAuth2AuthorizedClient) {

        SimpleGraphAuthProvider simpleGraphAuthProvider = new SimpleGraphAuthProvider(oAuth2AuthorizedClient.getAccessToken().getTokenValue());
        GraphServiceClient graphClient = new GraphServiceClient(simpleGraphAuthProvider);

        try {
            Subscription subscription = new Subscription();
            subscription.setNotificationUrl(Apps.Outlook.CALL_BACK_URL);
            OffsetDateTime expirationDateTime = OffsetDateTime.now(ZoneOffset.UTC);
            subscription.setExpirationDateTime(expirationDateTime.plusHours(24 * 3));
            var result = graphClient.subscriptions().bySubscriptionId(channelEntity.getChannelId()).patch(subscription);
            if (result != null) {
                channelEntity.setChannelId(result.getId());
                channelEntity.setNotificationUrl(Apps.Outlook.CALL_BACK_URL);
                channelEntity.setChannelExpiresAt(Objects.requireNonNull(result.getExpirationDateTime()).toEpochSecond());
                calvChannelsRepository.save(channelEntity);
            }

        } catch (Exception e) {
            log.error("renewOutlookChannel:", e);
        }

    }

    @EventListener(OAuth2AuthorizedClient.class)
    void initChannel(OAuth2AuthorizedClient oAuth2AuthorizedClient) {
        if (Objects.requireNonNull(oAuth2AuthorizedClient.getAccessToken().getExpiresAt()).isBefore(Instant.now())) {
            return;
        }

        String principalName = oAuth2AuthorizedClient.getPrincipalName();
        String clientRegistrationId = oAuth2AuthorizedClient.getClientRegistration().getRegistrationId();
        String calvId = "primary";
        CalvChannelsEntity primary = calvChannelsRepository.findByClientRegistrationIdAndPrincipalNameAndCalvId(clientRegistrationId, principalName, calvId);
        if (primary != null) {
            return;
        }


        if ("google".equals(clientRegistrationId)) {

            Calendar calendar = new Calendar.Builder(
                    new NetHttpTransport(),
                    JSON_FACTORY,
                    new Credential(BearerToken.authorizationHeaderAccessMethod()).setAccessToken(oAuth2AuthorizedClient.getAccessToken().getTokenValue()))
                    .setApplicationName(Apps.Google.APPLICATION_NAME)
                    .build();

            Map<String, String> params = new HashMap<>();
            Channel channel = new Channel()
                    .setId(TsidCreator.getTsid().toString())
                    .setType("web_hook")
                    .setAddress(Apps.Google.CALL_BACK_URL)
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
                    calvChannelsEntity.setNotificationUrl(Apps.Google.CALL_BACK_URL);
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
            SimpleGraphAuthProvider simpleGraphAuthProvider = new SimpleGraphAuthProvider(oAuth2AuthorizedClient.getAccessToken().getTokenValue());
            GraphServiceClient graphClient = new GraphServiceClient(simpleGraphAuthProvider);

            try {
                Subscription subscription = new Subscription();
                subscription.setChangeType("created,updated,deleted");
                subscription.setNotificationUrl(Apps.Outlook.CALL_BACK_URL);
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
                calvChannelsEntity.setNotificationUrl(Apps.Outlook.CALL_BACK_URL);
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

//    @Scheduled(fixedDelay = 10, timeUnit = TimeUnit.SECONDS)
//    void cleanChannel(){
//        OAuth2AuthorizedClient oAuth2AuthorizedClient = oAuth2AuthorizedClientService.loadAuthorizedClient(
//                "microsoft", "00000000-0000-0000-1bc9-b513b1153f74");
//        if (oAuth2AuthorizedClient == null || oAuth2AuthorizedClient.getAccessToken() == null) {
//            return;
//        }
//
//        SimpleGraphAuthProvider simpleGraphAuthProvider = new SimpleGraphAuthProvider(oAuth2AuthorizedClient.getAccessToken().getTokenValue());
//        GraphServiceClient graphClient = new GraphServiceClient(simpleGraphAuthProvider);
//
//        SubscriptionCollectionResponse subscriptionCollectionResponse = graphClient.subscriptions().get();
//        for (Subscription subscription : subscriptionCollectionResponse.getValue()) {
//            graphClient.subscriptions().bySubscriptionId(subscription.getId()).delete();
//        }
//    }

}