package com.qimiao.social.task;

import com.github.f4b6a3.tsid.TsidCreator;
import com.google.api.client.auth.oauth2.BearerToken;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Channel;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class CalvWatchTask {
    private static final String APPLICATION_NAME = "NBExampleApplication";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

    @Resource
    private OAuth2AuthorizedClientService oAuth2AuthorizedClientService;

    @Resource
    private CalvChannelsRepository calvChannelsRepository;

    @lombok.SneakyThrows
    @Scheduled(fixedDelay = 10, timeUnit = TimeUnit.MINUTES)
    void renewChannel() {
        OAuth2AuthorizedClient oAuth2AuthorizedClient = oAuth2AuthorizedClientService.loadAuthorizedClient(
                "google", "115152964495372047642");
        if (oAuth2AuthorizedClient == null || oAuth2AuthorizedClient.getAccessToken() == null) {
            return;
        }

        // 如果token已经失效. 下个周期再尝试订阅
        if (Objects.requireNonNull(oAuth2AuthorizedClient.getAccessToken().getExpiresAt()).isBefore(Instant.now())) {
            return;
        }

        CalvChannelsEntity activeChannel = calvChannelsRepository.findByClientRegistrationIdAndAccountIdAndCalvId("google", "115152964495372047642", "primary");

        if (activeChannel == null || activeChannel.nonExpired()) {
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
                .setAddress("https://96ef-113-104-190-29.ngrok-free.app/notifications")
                .setParams(params)
                .setExpiration(System.currentTimeMillis() + 7L * 24L * 60L * 60L * 1000L);


        try {
            Channel channel1 = calendar.events().watch("primary", channel).execute();
            if (channel1 != null && channel1.getId() != null) {
                System.out.println("Watch Kind : " + channel1.getKind());
                System.out.println("Watch Channel ID: " + channel1.getId());
                System.out.println("Watch Resource ID: " + channel1.getResourceId());

                activeChannel.setChannelId(channel1.getId());
                activeChannel.setAddress("https://96ef-113-104-190-29.ngrok-free.app/notifications");
                activeChannel.setResourceUri(channel1.getResourceUri());
                activeChannel.setResourceId(channel1.getResourceId());
                activeChannel.setExpireAt(channel1.getExpiration());
                activeChannel.setRemark(channel1.getKind());
                calvChannelsRepository.save(activeChannel);
            }

        } catch (Exception e) {
            log.error("calendar.events().watch:", e);
        }

    }


    /**
     * 这个动作由用户授权开始.默认就会产生一个订阅. (cozi 那边的话,与假期日历是相同的处理方法)
     */
    @EventListener(ApplicationReadyEvent.class)
    void initChannel() {
        long count = calvChannelsRepository.count();
        if (count > 0) {
            // 主要是做测试用的
            return;
        }

        OAuth2AuthorizedClient oAuth2AuthorizedClient = oAuth2AuthorizedClientService.loadAuthorizedClient(
                "google", "115152964495372047642");


        if (oAuth2AuthorizedClient == null || oAuth2AuthorizedClient.getAccessToken() == null) {
            return;
        }

        cleanChannels(oAuth2AuthorizedClient.getAccessToken().getTokenValue());

        CalvChannelsEntity calvChannelsEntity = new CalvChannelsEntity();
        calvChannelsEntity.setId(TsidCreator.getTsid().toLong());
        calvChannelsEntity.setAccountId("115152964495372047642");
        calvChannelsEntity.setCalvId("primary");
        calvChannelsEntity.setClientRegistrationId("google");


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
                .setAddress("https://96ef-113-104-190-29.ngrok-free.app/notifications")
                .setParams(params)
                .setExpiration(System.currentTimeMillis() + 7L * 24L * 60L * 60L * 1000L);


        try {
            Channel channel1 = calendar.events().watch("primary", channel).execute();
            if (channel1 != null && channel1.getId() != null) {
                System.out.println("Watch Kind : " + channel1.getKind());
                System.out.println("Watch Channel ID: " + channel1.getId());
                System.out.println("Watch Resource ID: " + channel1.getResourceId());

                calvChannelsEntity.setChannelId(channel1.getId());
                calvChannelsEntity.setAddress("https://96ef-113-104-190-29.ngrok-free.app/notifications");
                calvChannelsEntity.setResourceUri(channel1.getResourceUri());
                calvChannelsEntity.setResourceId(channel1.getResourceId());
                calvChannelsEntity.setExpireAt(channel1.getExpiration());
                calvChannelsEntity.setRemark(channel1.getKind());
                calvChannelsRepository.save(calvChannelsEntity);
            }
        } catch (IOException e) {
            log.error("calendar.events().watch:", e);
        }

    }

    void cleanChannels(String accessToken) {

        Calendar calendar = new Calendar.Builder(
                new NetHttpTransport(),
                JSON_FACTORY,
                new Credential(BearerToken.authorizationHeaderAccessMethod()).setAccessToken(accessToken))
                .setApplicationName(APPLICATION_NAME)
                .build();

        List<String> deletedChannelIds = List.of("467a2bcc-d7c8-4620-8b7b-a7b6bd8f69b6",
                "60b20392-219a-420a-b98d-fda5e75008f6",
                "0ea37cee-163f-40a8-872c-1bd1e06fb648",
                "fccf34dc-fc19-4b58-86c5-f398954bc105",
                "b4659913-3b75-4241-bf21-030907d36dcb"
        );

        deletedChannelIds.forEach(id -> {
            Channel c = new Channel();
            c.setId(id);
            try {
                calendar.channels().stop(c);
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }
        });

    }

}