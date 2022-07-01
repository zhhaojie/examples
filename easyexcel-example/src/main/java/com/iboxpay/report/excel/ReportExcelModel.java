package com.iboxpay.report.excel;


import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.format.DateTimeFormat;
import lombok.Data;

import java.util.Date;

@Data
public class ReportExcelModel {

    /**
     * 服务名
     */
    @ExcelProperty("service_name")
    private String service;

    /**
     * 产品线
     */
    @ExcelProperty("product_line")
    private String productLine;

    /**
     * 产品
     */
    @ExcelProperty("product")
    private String product;

    /**
     * 统计科目
     */
    @ExcelProperty("item")
    private String item;

    /**
     * 统计科目
     */
    @ExcelProperty("item_day")
    private String itemDay;

    /**
     * 数值
     */
    @ExcelProperty("cnt")
    private Double cnt;

    /**
     * 制表时间
     */
    @DateTimeFormat(value = "yyyy-MM-dd")
    @ExcelProperty(value = "create_at")
    private Date createAt;

    /**
     * 制表人
     */
    @ExcelProperty("create_by")
    private String createBy;
}
