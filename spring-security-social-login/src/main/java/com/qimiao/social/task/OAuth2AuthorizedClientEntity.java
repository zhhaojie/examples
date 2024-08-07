package com.qimiao.social.task;


import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.Instant;

/**
 * 第三方社交账号会话表（任何一个邮件在具体的第三方是唯一的）
 */
@Entity
@Table(name = "oauth2_authorized_client")
@Data
public class OAuth2AuthorizedClientEntity {

    @EmbeddedId
    private ClientPrincipal principal;

    private String accessTokenType;

    private String accessTokenValue;

    private Instant accessTokenIssuedAt;

    private Instant accessTokenExpiresAt;

    private String accessTokenScopes;

    private String refreshTokenValue;

    private Instant refreshTokenIssuedAt;

    private Instant createdAt;

}
