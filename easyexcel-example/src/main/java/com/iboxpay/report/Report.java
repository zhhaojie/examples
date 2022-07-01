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
@Table(name = "data_report")
public class Report {

    /**
     * GenerationType.IDENTITY 保证每个表ID的值都是递增的。
     * GenerationType.AUTO 多个表ID值共同一个全局ID生成器。
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 我方服务名
     */
    @Column(name = "service_name",nullable = false)
    private String service;

    /**
     * 产品线（谁用）
     */
    @Column(name = "product_line")
    private String productLine;

    /**
     * 产品（谁用）
     */
    @Column(name = "product")
    private String product;

    /**
     * 统计科目
     */
    @Column(name = "item", nullable = false)
    private String item;

    /**
     * 统计日期（年或月或日）
     */
    @Column(name = "item_day", nullable = false)
    private String itemDay;

    /**
     * 数值
     */
    @Column(name = "cnt", nullable = false)
    private Double cnt;

    /**
     * 第三方服务提供商（新增）
     */
    @Column(name = "channel")
    private String channel;

    /**
     * 制表时间
     */
    @Column(name = "create_at", nullable = false)
    private Date createAt;

    /**
     * 制表人
     */
    @Column(name = "create_by", nullable = false)
    private String createBy;

}
