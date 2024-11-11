package com.qimiao.social.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import net.minidev.json.JSONObject;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;

import java.io.IOException;

@Slf4j
public class CustomAuthenticationSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

    private final OAuth2AuthorizedClientService oAuth2AuthorizedClientService;

    private final ApplicationEventPublisher eventPublisher;

    public CustomAuthenticationSuccessHandler(OAuth2AuthorizedClientService oAuth2AuthorizedClientService, ApplicationEventPublisher eventPublisher) {
        this.oAuth2AuthorizedClientService = oAuth2AuthorizedClientService;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws ServletException, IOException {
        if (authentication instanceof OAuth2AuthenticationToken oauthToken) {
            OAuth2User oauthUser = oauthToken.getPrincipal();

            String authorizedClientRegistrationId = oauthToken.getAuthorizedClientRegistrationId();
            OAuth2AuthorizedClient authorizedClient = oAuth2AuthorizedClientService.loadAuthorizedClient(
                    authorizedClientRegistrationId, oauthToken.getName());

            if (authorizedClient != null) {
                OAuth2AccessToken accessToken = authorizedClient.getAccessToken();
                log.info("Principal: {}", oauthUser.getName());
                log.info("Access Token: {}", accessToken.getTokenValue());
                log.info("Access Token Issued At: {}", accessToken.getIssuedAt());
                log.info("Access Token Expires At: {}", accessToken.getExpiresAt());
                log.info("Access Token Scopes: {}", accessToken.getScopes());

                OAuth2RefreshToken refreshToken = authorizedClient.getRefreshToken();
                if (refreshToken != null) {
                    log.info("Refresh Token: {}", refreshToken.getTokenValue());
                    log.info("Refresh Token Issued At: {}", refreshToken.getIssuedAt());
                    log.info("Refresh Token Expires At: {}", refreshToken.getExpiresAt());
                }

                eventPublisher.publishEvent(authorizedClient);
            }

            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");

            JSONObject object = new JSONObject();
            object.put("code", 0);
            object.put("message", "ok");
            object.put("username", oauthUser.getName());
            object.put("email", oauthUser.getAttribute("email"));

            response.getWriter().write(object.toJSONString());
            response.getWriter().flush();
        }
    }
}