package com.qimiao.social.config;

import jakarta.annotation.Resource;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientProperties;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Data
@Lazy
@Component
public class CustomOAuth2ClientProperties {

    @Resource
    OAuth2ClientProperties oAuth2ClientProperties;

    @Value("${spring.application.name:NBApplication}")
    private String application;

    @Value("cf7cc825-b528-4694-879e-c25c0cd91ac7")
    private String tenantMicrosoft;

    private OAuth2ClientProperties.Registration googleReg;

    private OAuth2ClientProperties.Registration microsoftReg;

    private OAuth2ClientProperties.Provider microsoftProvider;

    public OAuth2ClientProperties.Registration getGoogleReg() {
        return oAuth2ClientProperties.getRegistration().get("google");
    }

    public OAuth2ClientProperties.Registration getMicrosoftReg() {
        return oAuth2ClientProperties.getRegistration().get("microsoft");
    }

    public OAuth2ClientProperties.Provider getMicrosoftProvider() {
        return oAuth2ClientProperties.getProvider().get("microsoft");
    }
}