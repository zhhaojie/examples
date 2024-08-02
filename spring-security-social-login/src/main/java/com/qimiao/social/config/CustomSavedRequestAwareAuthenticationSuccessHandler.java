package com.qimiao.social.config;

import jakarta.annotation.Resource;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.minidev.json.JSONObject;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

public class CustomSavedRequestAwareAuthenticationSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

    private final OAuth2AuthorizedClientService oAuth2AuthorizedClientService;

    public CustomSavedRequestAwareAuthenticationSuccessHandler(OAuth2AuthorizedClientService oAuth2AuthorizedClientService) {
        this.oAuth2AuthorizedClientService = oAuth2AuthorizedClientService;
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
                System.out.println("Access Token: " + accessToken.getTokenValue());
                System.out.println("Access Token Issued At: " + accessToken.getIssuedAt());
                System.out.println("Access Token Expires At: " + accessToken.getExpiresAt());
                System.out.println("Access Token Scopes: " + accessToken.getScopes());

                OAuth2RefreshToken refreshToken = authorizedClient.getRefreshToken();
                if (refreshToken != null) {
                    System.out.println("Refresh Token: " + refreshToken.getTokenValue());
                    System.out.println("Refresh Token Issued At: " + refreshToken.getIssuedAt());
                }
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