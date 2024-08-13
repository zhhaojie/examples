package com.qimiao.social.task;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.Comment;


/**
 * 谁的第三方日历列表(左侧的日历列表)
 */
@Getter
@Setter
@ToString
@RequiredArgsConstructor
@Entity
@Table(name = "tb_calvList")
public class ChannelCalendarList {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("主键ID")
    private Long id;

    @Comment("第三方用户ID")
    @Column(nullable = false)
    private String principalName;

    @Comment("第三方服务商")
    @Column(nullable = false)
    private String clientRegistrationId;

    @Comment("第三方日历ID")
    @Column(nullable = false)
    private String calvId;

    @Comment("第三方日历NAME")
    @Column(nullable = false)
    private String calvName;

    @Comment("可编辑")
    @Column(nullable = false)
    private boolean canEdit;

    @Comment("可删除")
    @Column(nullable = false)
    private boolean canDelete;

    @Comment("默认日历")
    @Column(nullable = false)
    private boolean isDefault;

    @Comment("是否订阅(为真时,Subscriptions有数据)")
    @Column(nullable = false)
    private boolean subscribe;

}
