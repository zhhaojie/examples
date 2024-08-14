package com.qimiao.social.task;

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
    void extendSubscriptions() {
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
        subscriptionsEntities.forEach(this::extendSingleChannel);
    }

    void extendSingleChannel(SubscriptionsEntity subscriptionsEntity) {
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
                .setExpiration(Instant.now().toEpochMilli() + 7L * 24L * 60L * 60L * 1000L);

        try {
            Channel result = calendar.events().watch(subscriptionsEntity.getCalvId(), channel).execute();
            if (result != null) {
                System.out.println("Watch Kind : " + result.getKind());
                System.out.println("Watch Channel ID: " + result.getId());
                System.out.println("Watch Resource ID: " + result.getResourceId());

                subscriptionsEntity.setSubscriptionId(result.getId());
                subscriptionsEntity.setEndpoint(Apps.GOOGLE.CALL_BACK_URL);
                subscriptionsEntity.setResourceUri(result.getResourceUri());
                subscriptionsEntity.setResourceId(result.getResourceId());
                subscriptionsEntity.setExpiresAt(result.getExpiration());
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
                subscriptionsEntity.setEndpoint(Apps.OUTLOOK.CALL_BACK_URL);
                subscriptionsEntity.setExpiresAt(Objects.requireNonNull(result.getExpirationDateTime()).toInstant().toEpochMilli());
                subscriptionsEntity.setResourceUri(result.getNotificationUrlAppId());
                subscriptionsEntity.setResourceId(result.getResource());
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


}