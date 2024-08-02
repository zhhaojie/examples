package com.qimiao.social.task;

import com.google.api.client.auth.oauth2.BearerToken;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.Events;

import java.io.IOException;

public class GoogleCalendarService {

    private static final String APPLICATION_NAME = "NBExampleApplication";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final HttpTransport HTTP_TRANSPORT = new NetHttpTransport();

    public Calendar getCalendarService(String accessToken) {
        Credential credential = new Credential.Builder(BearerToken.authorizationHeaderAccessMethod())
            .setTransport(HTTP_TRANSPORT)
            .setJsonFactory(JSON_FACTORY)
            .build()
            .setAccessToken(accessToken);

        return new Calendar.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
            .setApplicationName(APPLICATION_NAME)
            .build();
    }

    public void listEvents(String accessToken) throws IOException {
        Calendar service = getCalendarService(accessToken);
        String calendarId = "primary"; // Use "primary" for the primary calendar

        Events events = service.events().list(calendarId)
            .setMaxResults(10)
            .setOrderBy("startTime")
            .setSingleEvents(true)
            .execute();

        for (Event event : events.getItems()) {
            System.out.printf("Event: %s (%s)\n", event.getSummary(), event.getStart().getDate());
        }
    }

}