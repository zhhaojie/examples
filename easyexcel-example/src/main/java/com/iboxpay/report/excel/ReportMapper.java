package com.iboxpay.report.excel;

import com.iboxpay.report.Report;
import org.springframework.beans.BeanUtils;

import java.util.Date;

public class ReportMapper {

    public static Report toMonthReport(ReportExcelModel excelModel) {
        Report report = new Report();
        BeanUtils.copyProperties(excelModel, report);
        if (excelModel.getCreateAt() == null) {
            report.setCreateAt(new Date());
        }
        return report;
    }

}
