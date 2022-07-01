package com.iboxpay.report;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.Date;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "project_libs")
public class Lib {
    /**
     * GenerationType.IDENTITY 保证每个表ID的值都是递增的。
     * GenerationType.AUTO 多个表ID值共同一个全局ID生成器。
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 产品线(如钱盒线)
     */
    @Column(name = "product_line")
    private String productLine;

    /**
     * 产品（快收付系统）
     */
    @Column(name = "product",nullable = false)
    private String product;

    /**
     * org.springframework.boot
     */
    @Column(name = "dep_group_id")
    private String groupId;

    /**
     * spring-boot-starter-web
     */
    @Column(name = "dep_artifact_id",nullable = false)
    private String artifactId;

    /**
     * 2.5.0
     */
    @Column(name = "dep_version",nullable = false)
    private String version;

    /**
     * compile || runtime
     */
    @Column(name = "dep_scope")
    private String scope;

    /**
     * 制表时间(20201212)
     */
    @Column(name = "create_at", nullable = false)
    private Date createAt;

    /**
     * 制表人
     */
    @Column(name = "create_by", nullable = false)
    private String createBy;
}
