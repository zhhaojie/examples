package com.qimiao.social.task;

import com.google.api.client.auth.oauth2.BearerToken;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Channel;
import jakarta.annotation.Resource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class ChannelWatchTask {
    @Resource
    OAuth2AuthorizedClientService oAuth2AuthorizedClientService;
    private static final String APPLICATION_NAME = "NBExampleApplication";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

    private static final Map<String, Channel> channels = new HashMap<>();


    @lombok.SneakyThrows
    @Scheduled(fixedDelay = 10, timeUnit = TimeUnit.SECONDS)
    void doChannelWatch() {
        OAuth2AuthorizedClient oAuth2AuthorizedClient = oAuth2AuthorizedClientService.loadAuthorizedClient(
                "google", "115152964495372047642");

        if (oAuth2AuthorizedClient == null || oAuth2AuthorizedClient.getAccessToken() == null) {
            return;
        }
        // 判断上一次的订阅是否要到期了.如果接近到期,自行给用户续预
        String accessToken = oAuth2AuthorizedClient.getAccessToken().getTokenValue();

        if (!channels.containsKey("115152964495372047642")) {
            Calendar calendar = new Calendar.Builder(
                    new NetHttpTransport(),
                    JSON_FACTORY,
                    new Credential(BearerToken.authorizationHeaderAccessMethod()).setAccessToken(accessToken))
                    .setApplicationName(APPLICATION_NAME)
                    .build();

            Channel channel = new Channel()
                    .setId(UUID.randomUUID().toString()) // Unique identifier for the channel
                    .setType("web_hook")
                    .setAddress("https://96ef-113-104-190-29.ngrok-free.app/notifications")
                    .setExpiration(System.currentTimeMillis() + 7L * 24L * 60L * 60L * 1000L);

            Channel responseChannel = calendar.events().watch("primary", channel).execute();
            if (!responseChannel.isEmpty()) {
                channels.put("115152964495372047642", responseChannel);
                System.out.println("Watch Channel ID: " + responseChannel.getId());
            }
        }
    }

}
