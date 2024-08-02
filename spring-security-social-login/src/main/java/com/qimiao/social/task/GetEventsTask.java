package com.qimiao.social.task;

import com.google.api.client.auth.oauth2.BearerToken;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Events;
import jakarta.annotation.Resource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class GetEventsTask {

    private static final String APPLICATION_NAME = "Your Application Name";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

    @Resource
    OAuth2AuthorizedClientService oAuth2AuthorizedClientService;

    @lombok.SneakyThrows
    @Scheduled(initialDelay = 1, fixedDelay = 3, timeUnit = TimeUnit.SECONDS)
    void getEvents() {

        OAuth2AuthorizedClient oAuth2AuthorizedClient = oAuth2AuthorizedClientService.loadAuthorizedClient("google", "115152964495372047642");
        if (oAuth2AuthorizedClient == null || oAuth2AuthorizedClient.getAccessToken() == null) {
            return;
        }
        String principalName = oAuth2AuthorizedClient.getPrincipalName();
        String accessToken = oAuth2AuthorizedClient.getAccessToken().getTokenValue();

        // Build the calendar service
        Calendar service = new Calendar.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                JSON_FACTORY,
                new Credential(BearerToken.authorizationHeaderAccessMethod()).setAccessToken(accessToken))
                .setApplicationName(APPLICATION_NAME)
                .build();

        // Fetch the calendar events
        String calendarId = "primary";
        Events events = service.events().list(calendarId)
                .setMaxResults(10)
                .setOrderBy("startTime")
                .setSingleEvents(true)
                .execute();

        events.getItems().forEach(event -> {
            System.out.printf("Event: %s (%s)\n", event.getSummary(), event.getStart().getDateTime());
        });
    }

}
