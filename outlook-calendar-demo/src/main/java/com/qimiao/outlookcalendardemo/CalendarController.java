package com.qimiao.outlookcalendardemo;

import com.microsoft.graph.models.Event;
import com.microsoft.graph.serviceclient.GraphServiceClient;
import com.microsoft.kiota.RequestInformation;
import com.microsoft.kiota.authentication.AuthenticationProvider;
import okhttp3.Request;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.Map;

@Controller
public class CalendarController {

    private final ClientRegistrationRepository clientRegistrationRepository;

    public CalendarController(ClientRegistrationRepository clientRegistrationRepository) {
        this.clientRegistrationRepository = clientRegistrationRepository;
    }

    @GetMapping("/callback")
    public String getCalendarEvents(@RegisteredOAuth2AuthorizedClient("azure") OAuth2AuthorizedClient authorizedClient,
                                    @AuthenticationPrincipal OidcUser oidcUser,
                                    Model model) {

        String accessToken = authorizedClient.getAccessToken().getTokenValue();

        AuthenticationProvider authProvider = new SimpleAuthProvider(accessToken);
        GraphServiceClient graphClient = new GraphServiceClient(authProvider);
        List<Event> events = graphClient.me().calendar().events().get().getValue();

        model.addAttribute("events", events);


        return "calendar";
    }

    public static class SimpleAuthProvider implements AuthenticationProvider {
        private final String accessToken;

        public SimpleAuthProvider(String accessToken) {
            this.accessToken = accessToken;
        }

        @Override
        public void authenticateRequest(@NotNull RequestInformation request, @Nullable Map<String, Object> additionalAuthenticationContext) {
            request.headers.add("Authorization", "Bearer " + accessToken);
        }
    }
}