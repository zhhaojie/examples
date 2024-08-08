package com.qimiao.social.task;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.Type;

import java.time.Instant;

/**
 * 第三方社交账号会话表（任何一个邮件在具体的第三方是唯一的）
 */
@Getter
@Setter
@ToString
@RequiredArgsConstructor
@Entity
@Table(name = "oauth2_authorized_client")
public class OAuth2AuthorizedClientEntity {

    @EmbeddedId
    private ClientPrincipal principal;

    @Column(name = "access_token_type", nullable = false)
    private String accessTokenType;

    @Column(name = "access_token_value", nullable = false, columnDefinition = "TEXT")
    private String accessTokenValue;

    @Column(name = "access_token_issued_at", nullable = false)
    private Instant accessTokenIssuedAt;

    @Column(name = "access_token_expires_at", nullable = false)
    private Instant accessTokenExpiresAt;

    @Column(name = "access_token_scopes", length = 500)
    private String accessTokenScopes;

    @Column(name = "refresh_token_value", columnDefinition = "TEXT")
    private String refreshTokenValue;

    @Column(name = "refresh_token_issued_at")
    private Instant refreshTokenIssuedAt;

    @Column(name = "created_at")
    private Instant createdAt;

}


