package com.qimiao.social.task;

import com.azure.identity.OnBehalfOfCredential;
import com.azure.identity.OnBehalfOfCredentialBuilder;
import com.github.f4b6a3.tsid.TsidCreator;
import com.google.api.client.auth.oauth2.BearerToken;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.CalendarList;
import com.google.api.services.calendar.model.CalendarListEntry;
import com.google.api.services.calendar.model.Channel;
import com.microsoft.graph.models.CalendarCollectionResponse;
import com.microsoft.graph.models.Subscription;
import com.microsoft.graph.serviceclient.GraphServiceClient;
import com.qimiao.social.calendars.Apps;
import com.qimiao.social.calendars.SimpleGraphAuthProvider;
import jakarta.annotation.Resource;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;
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
    SubscriptionsRepository subscriptionsRepository;
    @Resource
    ChannelCalendarListRepository channelCalendarListRepository;

    @SneakyThrows
    @Scheduled(fixedDelay = 10, timeUnit = TimeUnit.MINUTES)
    void renewSubscriptions() {
        long startTime = System.currentTimeMillis();
        log.info("renewChannel started at {}", LocalDateTime.now());

        Pageable pageable = PageRequest.of(0, 100);
        Page<SubscriptionsEntity> resultPage;
        do {
            resultPage = subscriptionsRepository.findAll(pageable);
            subscriptions(resultPage.getContent());
            pageable = resultPage.nextPageable();
        } while (resultPage.hasNext());

        long endTime = System.currentTimeMillis();
        log.info("renewChannel ended at {}", LocalDateTime.now());
        log.info("renewChannel took {} milliseconds", endTime - startTime);
    }

    void subscriptions(List<SubscriptionsEntity> subscriptionsEntities) {
        subscriptionsEntities.forEach(this::renewSingleChannel);
    }

    void renewSingleChannel(SubscriptionsEntity subscriptionsEntity) {
        // 订阅关系没有过期
        if (subscriptionsEntity == null || subscriptionsEntity.nonExpired()) {
            return;
        }

        OAuth2AuthorizedClient oAuth2AuthorizedClient = oAuth2AuthorizedClientService.loadAuthorizedClient(
                subscriptionsEntity.getClientRegistrationId(), subscriptionsEntity.getPrincipalName());
        if (oAuth2AuthorizedClient == null || oAuth2AuthorizedClient.getAccessToken() == null) {
            return;
        }

        // 如果token已经失效(无法访问第三方API).因此需要等等待token更新. 可以等下个周期再尝试
        if (Objects.requireNonNull(oAuth2AuthorizedClient.getAccessToken().getExpiresAt()).isBefore(Instant.now())) {
            return;
        }

        if ("google".equals(subscriptionsEntity.getClientRegistrationId())) {
            renewGoogleChannel(subscriptionsEntity, oAuth2AuthorizedClient);
        } else if ("microsoft".equals(subscriptionsEntity.getClientRegistrationId())) {
            renewOutlookChannel(subscriptionsEntity, oAuth2AuthorizedClient);
        }
    }

    void renewGoogleChannel(SubscriptionsEntity subscriptionsEntity, OAuth2AuthorizedClient oAuth2AuthorizedClient) {

        Calendar calendar = new Calendar.Builder(
                new NetHttpTransport(),
                JSON_FACTORY,
                new Credential(BearerToken.authorizationHeaderAccessMethod()).setAccessToken(oAuth2AuthorizedClient.getAccessToken().getTokenValue()))
                .setApplicationName(Apps.GOOGLE.APPLICATION_NAME)
                .build();

        Map<String, String> params = new HashMap<>();
        Channel channel = new Channel()
                .setId(TsidCreator.getTsid().toString())
                .setType("web_hook")
                .setAddress(Apps.GOOGLE.APPLICATION_NAME)
                .setParams(params)
                .setExpiration(System.currentTimeMillis() + 7L * 24L * 60L * 60L * 1000L);


        try {
            Channel result = calendar.events().watch(subscriptionsEntity.getCalvId(), channel).execute();
            if (result != null) {
                System.out.println("Watch Kind : " + result.getKind());
                System.out.println("Watch Channel ID: " + result.getId());
                System.out.println("Watch Resource ID: " + result.getResourceId());

                subscriptionsEntity.setSubscriptionId(result.getId());
                subscriptionsEntity.setNotificationUrl(Apps.GOOGLE.CALL_BACK_URL);
                subscriptionsEntity.setResourceUri(result.getResourceUri());
                subscriptionsEntity.setResourceId(result.getResourceId());
                subscriptionsEntity.setExpiresAt(result.getExpiration());
                subscriptionsEntity.setRemark(result.getKind());
                subscriptionsRepository.save(subscriptionsEntity);
            }

        } catch (Exception e) {
            log.error("renewGoogleChannel:", e);
        }
    }

    void renewOutlookChannel(SubscriptionsEntity subscriptionsEntity, OAuth2AuthorizedClient oAuth2AuthorizedClient) {

        SimpleGraphAuthProvider simpleGraphAuthProvider = new SimpleGraphAuthProvider(oAuth2AuthorizedClient.getAccessToken().getTokenValue());
        GraphServiceClient graphClient = new GraphServiceClient(simpleGraphAuthProvider);

        try {
            Subscription subscription = new Subscription();
            subscription.setNotificationUrl(Apps.OUTLOOK.CALL_BACK_URL);
            OffsetDateTime expirationDateTime = OffsetDateTime.now(ZoneOffset.UTC);
            subscription.setExpirationDateTime(expirationDateTime.plusHours(24 * 3));
            var result = graphClient.subscriptions().bySubscriptionId(subscriptionsEntity.getSubscriptionId()).patch(subscription);
            if (result != null) {
                subscriptionsEntity.setSubscriptionId(result.getId());
                subscriptionsEntity.setNotificationUrl(Apps.OUTLOOK.CALL_BACK_URL);
                subscriptionsEntity.setExpiresAt(Objects.requireNonNull(result.getExpirationDateTime()).toInstant().toEpochMilli());
                subscriptionsRepository.save(subscriptionsEntity);
            }

        } catch (Exception e) {
            log.error("renewOutlookChannel:", e);
        }

    }

    @EventListener(OAuth2AuthorizedClient.class)
    void saveUserCalendarList(OAuth2AuthorizedClient oAuth2AuthorizedClient) {
        if (Objects.requireNonNull(oAuth2AuthorizedClient.getAccessToken().getExpiresAt()).isBefore(Instant.now())) {
            return;
        }
        String clientRegistrationId = oAuth2AuthorizedClient.getClientRegistration().getRegistrationId();
        String principalName = oAuth2AuthorizedClient.getPrincipalName();

        if ("google".equals(clientRegistrationId)) {
            Calendar service = new Calendar.Builder(
                    new NetHttpTransport(),
                    JSON_FACTORY,
                    new Credential(BearerToken.authorizationHeaderAccessMethod()).setAccessToken(oAuth2AuthorizedClient.getAccessToken().getTokenValue()))
                    .setApplicationName(Apps.GOOGLE.APPLICATION_NAME)
                    .build();
            try {
                CalendarList calendarList = service.calendarList().list().execute();
                for (CalendarListEntry entry : calendarList.getItems()) {
                    ChannelCalendarList registrationIdAndCalvId = channelCalendarListRepository.findByClientRegistrationIdAndCalvId(clientRegistrationId, entry.getId());

                    ChannelCalendarList channelCalendarList = registrationIdAndCalvId != null ? registrationIdAndCalvId : new ChannelCalendarList();
                    if (registrationIdAndCalvId == null) {
                        channelCalendarList.setId(TsidCreator.getTsid().toLong());
                    }
                    if (!entry.isDeleted()) {
                        channelCalendarList.setCalvName(entry.getSummary());
                        channelCalendarList.setClientRegistrationId(clientRegistrationId);
                        channelCalendarList.setCanDelete(false);
                        channelCalendarList.setCanEdit(false);
                        channelCalendarList.setPrincipalName(principalName);
                        channelCalendarList.setCalvId(entry.getId());
                        channelCalendarList.setDefault(entry.isPrimary());
                        channelCalendarListRepository.save(channelCalendarList);
                    }
                }
            } catch (IOException e) {
                log.error("saveUserCalendarList:", e);
            }

        }

        if ("microsoft".equals(clientRegistrationId)) {
            SimpleGraphAuthProvider simpleGraphAuthProvider = new SimpleGraphAuthProvider(oAuth2AuthorizedClient.getAccessToken().getTokenValue());
            GraphServiceClient graphClient = new GraphServiceClient(simpleGraphAuthProvider);
            CalendarCollectionResponse calendarCollectionResponse = graphClient.me().calendars().get();
            if (calendarCollectionResponse == null || calendarCollectionResponse.getValue() == null) {
                return;
            }

            for (com.microsoft.graph.models.Calendar calendar : calendarCollectionResponse.getValue()) {
                ChannelCalendarList registrationIdAndCalvId = channelCalendarListRepository.findByClientRegistrationIdAndCalvId(clientRegistrationId, calendar.getId());

                ChannelCalendarList channelCalendarList = registrationIdAndCalvId != null ? registrationIdAndCalvId : new ChannelCalendarList();
                if (registrationIdAndCalvId == null) {
                    channelCalendarList.setId(TsidCreator.getTsid().toLong());
                }
                if (Boolean.FALSE.equals(calendar.getIsRemovable())) {
                    channelCalendarList.setCalvName(calendar.getName());
                    channelCalendarList.setClientRegistrationId(clientRegistrationId);
                    channelCalendarList.setCanDelete(false);
                    channelCalendarList.setCanEdit(false);
                    channelCalendarList.setPrincipalName(principalName);
                    channelCalendarList.setCalvId(calendar.getId());
                    channelCalendarList.setDefault(Boolean.TRUE.equals(calendar.getIsDefaultCalendar()));
                    channelCalendarListRepository.save(channelCalendarList);
                }
            }
        }
    }


    void initChannel(OAuth2AuthorizedClient oAuth2AuthorizedClient) {
        if (Objects.requireNonNull(oAuth2AuthorizedClient.getAccessToken().getExpiresAt()).isBefore(Instant.now())) {
            return;
        }

        String clientRegistrationId = oAuth2AuthorizedClient.getClientRegistration().getRegistrationId();
        String principalName = oAuth2AuthorizedClient.getPrincipalName();

        if ("google".equals(clientRegistrationId)) {

            Calendar service = new Calendar.Builder(
                    new NetHttpTransport(),
                    JSON_FACTORY,
                    new Credential(BearerToken.authorizationHeaderAccessMethod()).setAccessToken(oAuth2AuthorizedClient.getAccessToken().getTokenValue()))
                    .setApplicationName(Apps.GOOGLE.APPLICATION_NAME)
                    .build();

            // 获取用户的默认日历
            CalendarList calendarList = null;
            CalendarListEntry defaultCalendar = null;
            try {
                calendarList = service.calendarList().list().execute();
                defaultCalendar = calendarList.getItems().stream()
                        .filter(CalendarListEntry::isPrimary)
                        .findFirst()
                        .orElse(null);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            if (defaultCalendar == null) {
                return;
            }

            String calvId = defaultCalendar.getId();

            // 检查是否已存在对应的订阅
            SubscriptionsEntity existingSubscription = subscriptionsRepository.findByClientRegistrationIdAndCalvIdAndPrincipalName(
                    clientRegistrationId, calvId, principalName);

            // 如果订阅还没有过期.就不要重复订阅. 它的处理逻辑和outlook的不一样.
            if (existingSubscription != null && existingSubscription.nonExpired()) {
                return;
            }

            try {
                Map<String, String> params = new HashMap<>();
                Channel channel = new Channel()
                        .setId(TsidCreator.getTsid().toString())
                        .setType("web_hook")
                        .setAddress(Apps.GOOGLE.CALL_BACK_URL)
                        .setParams(params)
                        .setExpiration(Instant.now().toEpochMilli() + 7L * 24L * 60L * 60L * 1000L);

                Channel result = service.events().watch(calvId, channel).execute();

                if (result != null && result.getId() != null) {
                    // 保存订阅信息
                    SubscriptionsEntity subscriptionsEntity = new SubscriptionsEntity();
                    if (subscriptionsEntity.getId() == null) {
                        subscriptionsEntity.setId(TsidCreator.getTsid().toLong());
                    }
                    subscriptionsEntity.setPrincipalName(principalName);
                    subscriptionsEntity.setCalvId(defaultCalendar.getId());
                    subscriptionsEntity.setClientRegistrationId(clientRegistrationId);
                    subscriptionsEntity.setNotificationUrl(Apps.GOOGLE.CALL_BACK_URL);
                    subscriptionsEntity.setSubscriptionId(result.getId());
                    subscriptionsEntity.setResourceUri(result.getResourceUri());
                    subscriptionsEntity.setResourceId(result.getResourceId());
                    if (result.getExpiration() != null) {
                        subscriptionsEntity.setExpiresAt(result.getExpiration());
                    }
                    subscriptionsEntity.setRemark("ClientId#" + result.getKind());

                    subscriptionsRepository.save(subscriptionsEntity);
                }
            } catch (IOException exception) {
                log.error("SubscriptionsTask.google().watch:", exception);
            }

        }

        if ("microsoft".equals(clientRegistrationId)) {

            SimpleGraphAuthProvider simpleGraphAuthProvider = new SimpleGraphAuthProvider(oAuth2AuthorizedClient.getAccessToken().getTokenValue());
            GraphServiceClient graphClient = new GraphServiceClient(simpleGraphAuthProvider);
            CalendarCollectionResponse calendarCollectionResponse = graphClient.me().calendars().get();
            if (calendarCollectionResponse == null || calendarCollectionResponse.getValue() == null) {
                return;
            }

            // 获取用户的默认日历
            com.microsoft.graph.models.Calendar defaultCalendar = calendarCollectionResponse.getValue().stream()
                    .filter(calendar -> Boolean.TRUE.equals(calendar.getIsDefaultCalendar()))
                    .findFirst()
                    .orElse(null);

            if (defaultCalendar == null) {
                return;
            }

            // 检查是否已存在对应的订阅
            SubscriptionsEntity existingSubscription = subscriptionsRepository.findByClientRegistrationIdAndCalvIdAndPrincipalName(
                    clientRegistrationId, defaultCalendar.getId(), principalName);

            try {
                // 创建或更新订阅
                Subscription subscription = new Subscription();
                subscription.setChangeType("created,updated,deleted");
                subscription.setNotificationUrl(Apps.OUTLOOK.CALL_BACK_URL);
                subscription.setResource("me/events");
                subscription.setExpirationDateTime(OffsetDateTime.now(ZoneOffset.UTC).plusHours(24 * 3));
                subscription.setClientState(TsidCreator.getTsid().toString());

                Subscription result;
                if (existingSubscription == null) {
                    result = graphClient.subscriptions().post(subscription);
                } else {
                    result = graphClient.subscriptions().bySubscriptionId(existingSubscription.getSubscriptionId()).patch(subscription);
                }

                if (result == null) {
                    return;
                }

                // 保存订阅信息
                SubscriptionsEntity subscriptionsEntity = (existingSubscription == null) ? new SubscriptionsEntity() : existingSubscription;
                if (subscriptionsEntity.getId() == null) {
                    subscriptionsEntity.setId(TsidCreator.getTsid().toLong());
                }

                subscriptionsEntity.setPrincipalName(principalName);
                subscriptionsEntity.setCalvId(defaultCalendar.getId());
                subscriptionsEntity.setClientRegistrationId(clientRegistrationId);
                subscriptionsEntity.setNotificationUrl(Apps.OUTLOOK.CALL_BACK_URL);
                subscriptionsEntity.setSubscriptionId(result.getId());
                subscriptionsEntity.setResourceUri(result.getResource());
                subscriptionsEntity.setResourceId(result.getNotificationUrlAppId());
                if (result.getExpirationDateTime() != null) {
                    subscriptionsEntity.setExpiresAt(result.getExpirationDateTime().toInstant().toEpochMilli());
                }
                subscriptionsEntity.setRemark("ClientId#" + result.getApplicationId());

                subscriptionsRepository.save(subscriptionsEntity);
            } catch (RuntimeException exception) {
                log.error("SubscriptionsTask.microsoft().watch:", exception);
            }
        }

    }

    void cleanChannel() {

        // 已经取得到了某个用户的token数据. (token 数据需要定期刷新)
        OAuth2AuthorizedClient oAuth2AuthorizedClient = oAuth2AuthorizedClientService.loadAuthorizedClient(
                "microsoft", "00000000-0000-0000-1bc9-b513b1153f74");
        if (oAuth2AuthorizedClient == null || oAuth2AuthorizedClient.getAccessToken() == null) {
            return;
        }

        OAuth2AccessToken accessToken = oAuth2AuthorizedClient.getAccessToken();
        SimpleGraphAuthProvider simpleGraphAuthProvider = new SimpleGraphAuthProvider(oAuth2AuthorizedClient.getAccessToken().getTokenValue());
        GraphServiceClient graphClient1 = new GraphServiceClient(simpleGraphAuthProvider);
        CalendarCollectionResponse calendarCollectionResponse1 = graphClient1.me().calendars().get();
        for (com.microsoft.graph.models.Calendar c : calendarCollectionResponse1.getValue()) {
            System.out.println(c.getName());
        }

        System.out.println(accessToken.getTokenValue());

//        final String[] scopes = new String[]{"offline_access",
//                "openid",
//                "email",
//                "profile",
//                "Calendars.Read",
//                "Channel.Create",
//                "Channel.Delete.All",
//                "Channel.ReadBasic.All"};
//        OnBehalfOfCredential credential = new OnBehalfOfCredentialBuilder()
//                .clientId(Apps.OUTLOOK.CLIENT_ID)
//                .clientSecret(Apps.OUTLOOK.CLIENT_SECRET)
//                .tenantId(Apps.OUTLOOK.TENANT_ID)
//                .userAssertion(accessToken.getTokenValue())
//                .build();
//        GraphServiceClient graphClient2 = new GraphServiceClient(credential, scopes);
//        CalendarCollectionResponse calendarCollectionResponse2 = graphClient2.me().calendars().get();
//        for (com.microsoft.graph.models.Calendar c : calendarCollectionResponse2.getValue()) {
//            System.out.println(c.getName());
//        }

    }

}