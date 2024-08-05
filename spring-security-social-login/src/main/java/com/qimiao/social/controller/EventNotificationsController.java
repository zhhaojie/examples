package com.qimiao.social.controller;

import com.google.api.services.calendar.model.Event;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Map;

@Controller
public class EventNotificationsController {

    @PostMapping("/notifications")
    public ResponseEntity<?> handleGoogleNotification(HttpServletRequest request) {
        StringBuilder sb = new StringBuilder();
        String line;
        try (BufferedReader reader = request.getReader()) {
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        } catch (IOException ignored) {
        }
        String rawPayload = sb.toString();
        System.out.println("rawPayload>>" + rawPayload);
        // Parse the rawPayload if needed and proceed with your logic
        return ResponseEntity.ok().build();
    }

    private Event fetchUpdatedEventFromGoogleCalendar(String eventId, String userId) {
        System.out.println(eventId);
        // Implementation to fetch updated event using Google Calendar API
        return null;
    }

    private void updateSharedStorage(Event updatedEvent, String userId) {
    }

    @PostMapping("/outlook")
    public ResponseEntity<?> handleOutLookNotification(@RequestBody Map<String, Object> notification) {
        String eventId = (String) notification.get("eventId");
        String userId = (String) notification.get("userId");

        return ResponseEntity.ok().build();
    }

}
