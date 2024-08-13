package com.qimiao.social.task;

import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.UserCredentials;
import com.microsoft.aad.msal4j.ClientCredentialFactory;
import com.microsoft.aad.msal4j.ConfidentialClientApplication;
import com.microsoft.aad.msal4j.IAuthenticationResult;
import com.microsoft.aad.msal4j.RefreshTokenParameters;
import com.qimiao.social.calendars.Apps;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientProperties;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.MalformedURLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 第一步: 更新token
 * 一般来说, google这个订阅关系7天过期. 微软订阅关系3天过期
 */
@Slf4j
@Service
class RefreshAccessTokenTask {
    @Resource
    OAuth2AuthorizedClientService oAuth2AuthorizedClientService;

    @Resource
    CustomOAuth2AuthorizedClientRepository customOAuth2AuthorizedClientRepository;

    @Resource
    OAuth2ClientProperties oAuth2ClientProperties;

    @Scheduled(fixedDelay = 1, timeUnit = TimeUnit.MINUTES)
    void doRefreshToken() {
        long startTime = System.currentTimeMillis();
        log.info("doRefreshToken started at {}", LocalDateTime.now());

        Pageable pageable = PageRequest.of(0, 100);
        Page<OAuth2AuthorizedClientEntity> resultPage;
        do {
            resultPage = customOAuth2AuthorizedClientRepository.findAll(pageable);
            refresh(resultPage.getContent());
            pageable = resultPage.nextPageable();
        } while (resultPage.hasNext());

        long endTime = System.currentTimeMillis();
        log.info("doRefreshToken ended at {}", LocalDateTime.now());
        log.info("doRefreshToken took {} milliseconds", endTime - startTime);
    }

    void refresh(List<OAuth2AuthorizedClientEntity> authorizedClientEntities) {
        List<OAuth2AuthorizedClientEntity> googles = authorizedClientEntities.stream()
                .filter(authorizedClient -> authorizedClient.getPrincipal().getClientRegistrationId().equals("google"))
                .toList();
        List<OAuth2AuthorizedClientEntity> microsofts = authorizedClientEntities.stream()
                .filter(authorizedClient -> authorizedClient.getPrincipal().getClientRegistrationId().equals("microsoft"))
                .toList();

        refreshGoogle(googles);
        refreshOutLook(microsofts);
    }

    // 可能需要虚拟线程来做
    void refreshGoogle(List<OAuth2AuthorizedClientEntity> authorizedClientEntities) {
        for (OAuth2AuthorizedClientEntity entity : authorizedClientEntities) {

            OAuth2AuthorizedClient oAuth2AuthorizedClient =
                    oAuth2AuthorizedClientService.loadAuthorizedClient(entity.getPrincipal().getClientRegistrationId(), entity.getPrincipal().getPrincipalName());

            if (oAuth2AuthorizedClient == null || oAuth2AuthorizedClient.getAccessToken() == null
                    || oAuth2AuthorizedClient.getRefreshToken() == null || oAuth2AuthorizedClient.getAccessToken().getExpiresAt() == null) {
                log.warn("OAuth2AuthorizedClient@GOOGLE 数据非法, 无法更新Token");
                return;
            }

            OAuth2AccessToken accessToken = oAuth2AuthorizedClient.getAccessToken();
            OAuth2RefreshToken refreshToken = oAuth2AuthorizedClient.getRefreshToken();

            // 提前十分钟更新
            Instant expirationTime = accessToken.getExpiresAt();
            if (expirationTime.isAfter(Instant.now())) {
                return;
            }

            try {
                UserCredentials credentials = UserCredentials.newBuilder()
                        .setClientId(Apps.GOOGLE.CLIENT_ID)
                        .setClientSecret(Apps.GOOGLE.CLIENT_SECRET)
                        .setRefreshToken(refreshToken.getTokenValue())
                        .build();

                credentials.refreshIfExpired();
                AccessToken newToken = credentials.getAccessToken();
                if (newToken == null || newToken.getExpirationTime() == null) {
                    log.error("请求Google数据失败.无法更新.");
                    return;
                }

                Instant expirationInstant = newToken.getExpirationTime().toInstant();
                Instant oAuth2ExpirationInstant = Objects.requireNonNull(accessToken.getExpiresAt());

                OAuth2AccessToken updatedAccessToken = new OAuth2AccessToken(
                        accessToken.getTokenType(),
                        newToken.getTokenValue(),
                        expirationInstant.minusMillis(newToken.getExpirationTime().getTime() - oAuth2ExpirationInstant.toEpochMilli()),
                        expirationInstant
                );

                OAuth2AuthorizedClient updatedClient = new OAuth2AuthorizedClient(
                        oAuth2AuthorizedClient.getClientRegistration(),
                        oAuth2AuthorizedClient.getPrincipalName(),
                        updatedAccessToken,
                        refreshToken
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
                    public String getName() {
                        return oAuth2AuthorizedClient.getPrincipalName();
                    }

                    @Override
                    public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {

                    }


                });

            } catch (IOException exception) {
                log.error("refreshGoogle:", exception);
            }
        }
    }

    void refreshOutLook(List<OAuth2AuthorizedClientEntity> authorizedClientEntities) {
        for (OAuth2AuthorizedClientEntity entity : authorizedClientEntities) {
            OAuth2AuthorizedClient oAuth2AuthorizedClient =
                    oAuth2AuthorizedClientService.loadAuthorizedClient(entity.getPrincipal().getClientRegistrationId(), entity.getPrincipal().getPrincipalName());

            if (oAuth2AuthorizedClient == null || oAuth2AuthorizedClient.getAccessToken() == null
                    || oAuth2AuthorizedClient.getRefreshToken() == null || oAuth2AuthorizedClient.getAccessToken().getExpiresAt() == null) {
                log.warn("OAuth2AuthorizedClient@OUTLOOK 数据非法, 无法更新Token");
                return;
            }

            // 提前十分钟更新
            Instant expirationTime = Objects.requireNonNull(oAuth2AuthorizedClient.getAccessToken().getExpiresAt());
            if (expirationTime.isAfter(Instant.now().minus(10, ChronoUnit.MINUTES))) {
                return;
            }

            // 手动刷新令牌
            String clientId = Apps.OUTLOOK.CLIENT_ID;
            String clientSecret = Apps.OUTLOOK.CLIENT_SECRET;
            String tenantMicrosoft = Apps.OUTLOOK.TENANT_ID;
            String authorizationUri = oAuth2ClientProperties.getProvider().get("microsoft").getAuthorizationUri();
            Set<String> scopes = oAuth2ClientProperties.getRegistration().get("microsoft").getScope();

            OAuth2RefreshToken refreshToken = oAuth2AuthorizedClient.getRefreshToken();
            OAuth2AccessToken accessToken = oAuth2AuthorizedClient.getAccessToken();

            assert authorizationUri != null;
            assert Objects.requireNonNull(refreshToken).getTokenValue() != null;

            ConfidentialClientApplication app;
            try {
                app = ConfidentialClientApplication.builder(clientId, ClientCredentialFactory.createFromSecret(clientSecret))
                        .authority(authorizationUri)
                        .build();

                RefreshTokenParameters parameters = RefreshTokenParameters.builder(scopes, refreshToken.getTokenValue())
                        .tenant(tenantMicrosoft)
                        .build();

                IAuthenticationResult authenticationResult = app.acquireToken(parameters).join();

                if (authenticationResult == null) {
                    log.error("请求Graph数据失败.无法更新.");
                    return;
                }

                String newAccessToken = authenticationResult.accessToken();
                Instant expiresDate = authenticationResult.expiresOnDate().toInstant();

                OAuth2AccessToken updatedAccessToken = new OAuth2AccessToken(
                        accessToken.getTokenType(),
                        newAccessToken,
                        expiresDate.minusMillis(expiresDate.toEpochMilli() - Objects.requireNonNull(accessToken.getExpiresAt()).toEpochMilli()),
                        expiresDate
                );

                OAuth2AuthorizedClient updatedClient = new OAuth2AuthorizedClient(
                        oAuth2AuthorizedClient.getClientRegistration(),
                        oAuth2AuthorizedClient.getPrincipalName(),
                        updatedAccessToken,
                        refreshToken
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
                    public String getName() {
                        return oAuth2AuthorizedClient.getPrincipalName();
                    }

                    @Override
                    public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {

                    }


                });

            } catch (MalformedURLException exception) {
                log.error("refreshOutLook:", exception);
            }

        }
    }


}
