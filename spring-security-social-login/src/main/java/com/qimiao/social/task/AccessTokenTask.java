package com.qimiao.social.task;

import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.UserCredentials;
import jakarta.annotation.Resource;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

@Service
public class AccessTokenTask {

    @Resource
    OAuth2AuthorizedClientService oAuth2AuthorizedClientService;

  //  @Scheduled(initialDelay = 1, fixedDelay = 10, timeUnit = TimeUnit.SECONDS)
    void doRefreshToken() {
        OAuth2AuthorizedClient oAuth2AuthorizedClient = oAuth2AuthorizedClientService.loadAuthorizedClient(
                "google", "115152964495372047642");

        if (oAuth2AuthorizedClient == null || oAuth2AuthorizedClient.getRefreshToken() == null) {
            return;
        }

        try {
            UserCredentials credentials = UserCredentials.newBuilder()
                    .setClientId("768944951916-ik6spf8bdt6f9jk9l96u2f1bos12upgl.apps.googleusercontent.com")
                    .setClientSecret("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")
                    .setRefreshToken(oAuth2AuthorizedClient.getRefreshToken().getTokenValue())
                    .build();

            credentials.refreshIfExpired();
            AccessToken token = credentials.getAccessToken();
            // 有可能用户收回了授权, 从而无法拿到最新的数据.
            if (token == null) {

                return;
            }

            OAuth2AccessToken updatedAccessToken = new OAuth2AccessToken(
                    oAuth2AuthorizedClient.getAccessToken().getTokenType(),
                    token.getTokenValue(),
                    token.getExpirationTime().toInstant().minusMillis(token.getExpirationTime().getTime() - Objects.requireNonNull(oAuth2AuthorizedClient.getAccessToken().getExpiresAt()).toEpochMilli()),
                    token.getExpirationTime().toInstant()
            );

            OAuth2AuthorizedClient updatedClient = new OAuth2AuthorizedClient(
                    oAuth2AuthorizedClient.getClientRegistration(),
                    oAuth2AuthorizedClient.getPrincipalName(),
                    updatedAccessToken,
                    oAuth2AuthorizedClient.getRefreshToken()
            );

            oAuth2AuthorizedClientService.saveAuthorizedClient(updatedClient, new Authentication() {
                @Override
                public Collection<? extends GrantedAuthority> getAuthorities() {
                    return List.of();
                }

                @Override
                public Object getCredentials() {
                    return null;
                }

                @Override
                public Object getDetails() {
                    return null;
                }

                @Override
                public Object getPrincipal() {
                    return oAuth2AuthorizedClient.getPrincipalName();
                }

                @Override
                public boolean isAuthenticated() {
                    return true;
                }

                @Override
                public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {

                }

                @Override
                public String getName() {
                    return oAuth2AuthorizedClient.getPrincipalName();
                }
            });

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }


}
