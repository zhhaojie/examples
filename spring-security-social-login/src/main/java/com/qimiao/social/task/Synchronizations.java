package com.qimiao.social.task;

import com.google.api.client.auth.oauth2.BearerToken;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.Events;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Event数据增量同步实现.该方法由google或outlook进行回调.
 * 这个入口必须稳定,上线之后不允许随意变更.
 */
@Controller
@Slf4j
public class Synchronizations {

    @Resource
    private OAuth2AuthorizedClientService oAuth2AuthorizedClientService;

    @Resource
    private CalvChannelsRepository calvChannelsRepository;

    @PostMapping("/notifications/google")
    public ResponseEntity<Void> google(HttpServletRequest request) {
        System.out.println("google callback");

        String channelId = request.getHeader("X-Goog-Channel-ID");
        String resourceId = request.getHeader("X-Goog-Resource-ID");
        String resourceState = request.getHeader("X-Goog-Resource-State");
        String resourceUri = request.getHeader("X-Goog-Resource-URI");
        String messageNumber = request.getHeader("X-Goog-Message-Number");

        System.out.println("channelId: " + channelId);
        System.out.println("resourceId: " + resourceId);
        System.out.println("resourceState: " + resourceState);
        System.out.println("messageNumber: " + messageNumber);
        System.out.println("-----------");

        syncEvents(channelId, resourceId);

        return ResponseEntity.ok().build();
    }

    @PostMapping("/notifications/outlook")
    public ResponseEntity<?> outlook() {
        System.out.println("outlook callback");
        return null;
    }

    public void syncEvents(String channelId, String resourceId) {
        CalvChannelsEntity channelsEntity = calvChannelsRepository.findByChannelIdAndResourceId(channelId, resourceId);
        if (channelsEntity == null) {
            return;
        }
        try {
            OAuth2AuthorizedClient oAuth2AuthorizedClient = getOAuth2AuthorizedClient(channelsEntity);
            if (oAuth2AuthorizedClient == null) {
                log.warn("未能加载到授权的客户端, accountId: {}", channelsEntity.getAccountId());
                return;
            }
            String accessToken = oAuth2AuthorizedClient.getAccessToken().getTokenValue();

            Calendar service = new Calendar.Builder(
                    GoogleNetHttpTransport.newTrustedTransport(),
                    GsonFactory.getDefaultInstance(),
                    new Credential(BearerToken.authorizationHeaderAccessMethod()).setAccessToken(accessToken))
                    .setApplicationName("NBExampleApplication")
                    .build();

            String calendarId = "primary";
            String nextPageToken = null;
            String nextSyncToken = channelsEntity.getNextSyncToken();

            do {
                Events events;
                try {
                    if (nextSyncToken != null) {
                        events = service.events().list(calendarId)
                                .setSyncToken(nextSyncToken)
                                .setPageToken(nextPageToken)
                                .setSingleEvents(false)
                                .execute();
                    } else {
                        events = service.events().list(calendarId)
                                .setMaxResults(250)
                                .setSingleEvents(false)
                                .setTimeMin(new DateTime(System.currentTimeMillis() - 365L * 24L * 60L * 60L * 1000L)) // 过去一年的数据
                                .setTimeMax(new DateTime(System.currentTimeMillis() + 7L * 24L * 60L * 60L * 1000L)) // 下周的数据
                                .setPageToken(nextPageToken)
                                .execute();
                    }

                    for (Event event : events.getItems()) {
                        processEvent(event);
                    }

                    nextPageToken = events.getNextPageToken();
                    if (events.getNextSyncToken() != null) {
                        nextSyncToken = events.getNextSyncToken();
                    }

                } catch (GoogleJsonResponseException e) {
                    if (e.getStatusCode() == 410) {
                        log.warn("Invalid sync token for channelId: {}, resourceId: {}, clearing event store and re-syncing.", channelId, resourceId);
                        nextSyncToken = null;
                        channelsEntity.setNextSyncToken(null);
                        calvChannelsRepository.save(channelsEntity);
                    } else {
                        log.error("Error syncing events for channelId: {}, resourceId: {}: {}", channelId, resourceId, e.getMessage());
                        throw e;  // 重新抛出异常以便于上层处理
                    }
                }
            } while (nextPageToken != null);

            if (nextSyncToken != null) {
                channelsEntity.setNextSyncToken(nextSyncToken);
                calvChannelsRepository.save(channelsEntity);
            }

        } catch (IOException | GeneralSecurityException e) {
            log.error("syncEvents error for channelId: {}, resourceId: {}", channelId, resourceId, e);
        }
    }

    private OAuth2AuthorizedClient getOAuth2AuthorizedClient(CalvChannelsEntity channelsEntity) {
        OAuth2AuthorizedClient oAuth2AuthorizedClient = oAuth2AuthorizedClientService.loadAuthorizedClient(channelsEntity.getClientRegistrationId(), channelsEntity.getAccountId());
        if (oAuth2AuthorizedClient != null && oAuth2AuthorizedClient.getAccessToken() != null) {
            Instant expirationTime = oAuth2AuthorizedClient.getAccessToken().getExpiresAt();
            if (expirationTime != null && expirationTime.isAfter(Instant.now())) {
                return oAuth2AuthorizedClient;
            } else {
                log.warn("Token for accountId: {} 已过期", channelsEntity.getAccountId());
            }
        }
        return null;
    }

    private void processEvent(Event event) {
        final String RESET = "\u001B[0m";
        final String CYAN = "\u001B[36m";
        System.out.printf(CYAN + "EventId: %s : Title: %s (%s)\n" + RESET, event.getId(), event.getSummary(), event.getStatus());
    }
}