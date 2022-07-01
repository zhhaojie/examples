package com.iboxpay.report.excel;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.format.DateTimeFormat;
import lombok.Data;

import java.util.Date;

@Data
public class LibExcelModel {

    /**
     * 产品线(如钱盒线)
     */
    @ExcelProperty("product_line")
    private String productLine;

    /**
     * 产品（快收付系统）
     */
    @ExcelProperty("product")
    private String product;

    /**
     * org.springframework.boot
     */
    @ExcelProperty("dep_group_id")
    private String groupId;

    /**
     * spring-boot-starter-web
     */
    @ExcelProperty("dep_artifact_id")
    private String artifactId;

    /**
     * 2.5.0
     */
    @ExcelProperty("dep_version")
    private String version;

    /**
     * compile || runtime
     */
    @ExcelProperty("dep_scope")
    private String scope;

    /**
     * 制表时间(20201212)
     */
    @ExcelProperty("create_at")
    @DateTimeFormat(value = "yyyy-MM-dd")
    private Date createAt;

    /**
     * 制表人
     */
    @ExcelProperty("create_by")
    private String createBy;
}
