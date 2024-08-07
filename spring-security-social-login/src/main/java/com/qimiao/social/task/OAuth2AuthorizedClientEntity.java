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
    private ClientPrincipalId id;

    @Column(name = "access_token_type")
    private String accessTokenType;

    @Column(name = "access_token_value")
    private String accessTokenValue;

    @Column(name = "access_token_issued_at")
    private Instant accessTokenIssuedAt;

    @Column(name = "access_token_expires_at")
    private Instant accessTokenExpiresAt;

    @Column(name = "access_token_scopes")
    private String accessTokenScopes;

    @Column(name = "refresh_token_value")
    private String refreshTokenValue;

    @Column(name = "refresh_token_issued_at")
    private Instant refreshTokenIssuedAt;

    @Column(name = "created_at")
    private Instant createdAt;

}
