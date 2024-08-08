package com.qimiao.social.calendars;

import com.microsoft.graph.serviceclient.GraphServiceClient;
import org.springframework.lang.NonNull;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;

public class GraphClientHelper {

    /**
     * private constructor to hide the implicit public one
     */
    private GraphClientHelper() {
        throw new IllegalStateException("Static class");
    }


    /**
     * @param oauthClient the authorized OAuth2 client to authenticate Graph requests with
     * @return A Graph client object that uses the provided OAuth2 client for access tokens
     */
    public static GraphServiceClient getGraphClient(
            @NonNull final OAuth2AuthorizedClient oauthClient) {
        final var authProvider = new SpringOAuth2AuthProvider(oauthClient);

        return new GraphServiceClient(authProvider);
    }
}
