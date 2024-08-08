package com.qimiao.social.task;

import com.microsoft.graph.models.Event;
import com.microsoft.graph.models.EventCollectionResponse;
import com.microsoft.graph.serviceclient.GraphServiceClient;
import com.microsoft.kiota.authentication.AuthenticationProvider;
import com.qimiao.social.calendars.MsGraphClientHelper;
import com.qimiao.social.calendars.SimpleAuthProvider;
import jakarta.annotation.Resource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
public class XX {

    @Resource
    CustomOAuth2AuthorizedClientRepository oAuth2AuthorizedClientRepository;

    @Resource
    OAuth2AuthorizedClientService oAuth2AuthorizedClientService;

    @Scheduled(fixedRate = 1000)
    void outlook() {
        List<OAuth2AuthorizedClientEntity> authorizedClientEntities = oAuth2AuthorizedClientRepository.findAll();
        List<OAuth2AuthorizedClientEntity> azures = authorizedClientEntities.stream().filter(oo -> oo.getPrincipal().getClientRegistrationId().equals("azure")).toList();

        for (OAuth2AuthorizedClientEntity authorizedClientEntity : azures) {
            OAuth2AuthorizedClient oAuth2AuthorizedClient = oAuth2AuthorizedClientService.loadAuthorizedClient(authorizedClientEntity.getPrincipal().getClientRegistrationId(), authorizedClientEntity.getPrincipal().getPrincipalName());

            SimpleAuthProvider simpleAuthProvider = new SimpleAuthProvider(oAuth2AuthorizedClient.getAccessToken().getTokenValue());
            GraphServiceClient graphClient = new GraphServiceClient(simpleAuthProvider);

            EventCollectionResponse eventCollectionResponse = graphClient.me().calendar().events().get();
            assert eventCollectionResponse != null;
            for (Event event : Objects.requireNonNull(eventCollectionResponse.getValue())) {
                processEvent(event);
            }

        }

    }

    private void processEvent(Event event) {
        final String RESET = "\u001B[0m";
        final String CYAN = "\u001B[36m";
        System.out.printf(CYAN + "EventId: %s : Title: %s (%s)\n" + RESET, event.getId(), event.getSubject(), event.getICalUId());
    }
}
