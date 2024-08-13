package com.qimiao.social.task;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.Comment;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.Instant;

/**
 * 同一个账号下面的日历资源.都有可能会生产多次订阅(channelId).
 * 每一个订阅都有可能会过期.
 * 目前而言,一个账号下面只能绑定一个google日历,一个outlook日历.
 */
@Getter
@Setter
@ToString
@RequiredArgsConstructor
@Entity
@Table(name = "tb_calv_subscriptions")
public class SubscriptionsEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("主键ID")
    private Long id;

    @Comment("第三方服务商")
    @Column(nullable = false)
    private String clientRegistrationId;

    @Comment("calvId")
    @Column(nullable = false)
    private String calvId;

    @Comment("calvName")
    @Column(nullable = false)
    private String calvName;

    @Comment("resourceUri")
    private String resourceUri;

    @Comment("resourceId")
    private String resourceId;

    @Comment("subscriptionId")
    @Column(nullable = false)
    private String subscriptionId;

    @Comment("expiresAt")
    @Column(nullable = false)
    private Long expiresAt;

    @Comment("remark")
    private String remark;

    @Comment("principalName")
    @Column(nullable = false)
    private String principalName;

    @Comment("notificationUrl")
    @Column(nullable = false)
    private String notificationUrl;

    @Comment("在每次同步后，获取 nextSyncToken 并存储，以便在下一次同步时使用")
    private String nextSyncToken;

    @LastModifiedDate
    @JsonIgnore
    private Instant lastSyncAt;

    public boolean nonExpired() {
        return expiresAt > Instant.now().toEpochMilli();
    }

}