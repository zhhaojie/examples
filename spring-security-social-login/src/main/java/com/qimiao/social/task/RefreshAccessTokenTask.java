package com.qimiao.social.task;

import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.UserCredentials;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
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
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 第一步: 更新token
 */
@Slf4j
@Service
class RefreshAccessTokenTask {

    @Resource
    OAuth2AuthorizedClientService oAuth2AuthorizedClientService;

    @Resource
    CustomOAuth2AuthorizedClientRepository customOAuth2AuthorizedClientRepository;

    @Scheduled(fixedDelay = 30, timeUnit = TimeUnit.SECONDS)
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
        List<OAuth2AuthorizedClientEntity> azures = authorizedClientEntities.stream()
                .filter(authorizedClient -> authorizedClient.getPrincipal().getClientRegistrationId().equals("azure"))
                .toList();

        refreshGoogle(googles);
        refreshOutLook(azures);
    }

    // 可能需要虚拟线程来做
    void refreshGoogle(List<OAuth2AuthorizedClientEntity> authorizedClientEntities) {
        for (OAuth2AuthorizedClientEntity entity : authorizedClientEntities) {

            OAuth2AuthorizedClient oAuth2AuthorizedClient =
                    oAuth2AuthorizedClientService.loadAuthorizedClient(entity.getPrincipal().getClientRegistrationId(), entity.getPrincipal().getPrincipalName());

            if (oAuth2AuthorizedClient == null || oAuth2AuthorizedClient.getAccessToken() == null || oAuth2AuthorizedClient.getRefreshToken() == null) {
                return;
            }

            OAuth2AccessToken accessToken = oAuth2AuthorizedClient.getAccessToken();
            OAuth2RefreshToken refreshToken = oAuth2AuthorizedClient.getRefreshToken();

            // 提前十分钟更新
            Instant expirationTime = Objects.requireNonNull(accessToken.getExpiresAt());
            if (expirationTime.isAfter(Instant.now())) {
                return;
            }

            try {
                UserCredentials credentials = UserCredentials.newBuilder()
                        .setClientId("768944951916-ik6spf8bdt6f9jk9l96u2f1bos12upgl.apps.googleusercontent.com")
                        .setClientSecret("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")
                        .setRefreshToken(refreshToken.getTokenValue())
                        .build();

                credentials.refreshIfExpired();
                AccessToken newToken = credentials.getAccessToken();
                if (newToken == null || newToken.getExpirationTime() == null) {
                    return;
                }

                Instant expirationInstant = newToken.getExpirationTime().toInstant();

                // 获取 oAuth2AuthorizedClient 的到期时间，并确保它不为 null
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

    void refreshOutLook(List<OAuth2AuthorizedClientEntity> authorizedClientEntities) {
    }

}
