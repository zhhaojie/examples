package com.qimiao.social.task;

import com.google.api.client.auth.oauth2.BearerToken;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.Events;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * google 或outlook 进行回调.
 * 这个入口必须稳定, 上线之后就不允许随意变更.
 */
@Controller
@Slf4j
public class EventNotifications {

    @Resource
    private OAuth2AuthorizedClientService oAuth2AuthorizedClientService;

    @Resource
    private CalvChannelsRepository calvChannelsRepository;

    private final static Map<String, String> nextSyncToken4Resources = new HashMap<>();

    @PostMapping("/notifications/google")
    public ResponseEntity<Void> handleGoogleNotification(HttpServletRequest request) {
        System.out.println("handleGoogleNotification");

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

        syncEvents(resourceUri, resourceId);

        return ResponseEntity.ok().build();
    }

    @PostMapping("/notifications/outlook")
    public ResponseEntity<?> handleOutLookNotification() {
        return null;
    }

    @Async
    public void syncEvents(String channelId, String resourceId) {
        CalvChannelsEntity channelsEntity = calvChannelsRepository.findByChannelIdAndResourceId(channelId, resourceId);
        if (channelsEntity == null) {
            return;
        }
        try {
            OAuth2AuthorizedClient oAuth2AuthorizedClient = oAuth2AuthorizedClientService.loadAuthorizedClient(channelsEntity.getClientRegistrationId(), channelsEntity.getAccountId());
            if (oAuth2AuthorizedClient == null || oAuth2AuthorizedClient.getAccessToken() == null) {
                return;
            }
            OAuth2AccessToken accessToken = oAuth2AuthorizedClient.getAccessToken();
            Instant expirationTime = Objects.requireNonNull(accessToken.getExpiresAt());
            if (expirationTime.isBefore(Instant.now())) {
                log.warn("账号:{} token 已经超时. 等待token更新", channelsEntity.getAccountId());
                return;
            }
            // Build the Calendar service
            Calendar service = new Calendar.Builder(
                    GoogleNetHttpTransport.newTrustedTransport(),
                    GsonFactory.getDefaultInstance(),
                    new Credential(BearerToken.authorizationHeaderAccessMethod()).setAccessToken(accessToken.getTokenValue()))
                    .setApplicationName("NBExampleApplication")
                    .build();

            String calendarId = "primary";
            String nextPageToken = null;
            String nextSyncToken = nextSyncToken4Resources.get(resourceId);

            do {
                Events events;
                if (nextSyncToken != null) {
                    events = service.events().list(calendarId)
                            .setSyncToken(nextSyncToken)
                            .setPageToken(nextPageToken)
                            .setSingleEvents(false)
                            .execute();
                } else {
                    events = service.events().list(calendarId)
                            .setMaxResults(10)
                            .setSingleEvents(false)
                            .setTimeMin(new DateTime(System.currentTimeMillis() - 2L * 24L * 60L * 60L * 1000L)) // 过去一年的数据
                            .setTimeMax(new DateTime(System.currentTimeMillis() + 30L * 24L * 60L * 60L * 1000L)) // 下周的数据
                            .setPageToken(nextPageToken)
                            .execute();
                }

                for (Event event : events.getItems()) {
                    System.out.printf("Event: %s (%s)\n", event.getSummary(), event.getStart().getDateTime());
                }

                nextPageToken = events.getNextPageToken();
                nextSyncToken = events.getNextSyncToken();
            } while (nextPageToken != null);
            if (nextSyncToken != null) {
                channelsEntity.setNextSyncToken(nextSyncToken);
                calvChannelsRepository.save(channelsEntity);
            }

        } catch (IOException | GeneralSecurityException e) {
            log.error("syncEvents", e);
        }
    }
}