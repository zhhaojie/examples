package com.iboxpay.report;

import com.alibaba.excel.EasyExcel;
import com.iboxpay.report.excel.LibExcelModel;
import com.iboxpay.report.excel.LibReadListener;
import com.iboxpay.report.excel.ReportExcelModel;
import com.iboxpay.report.excel.ReportReadListener;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

@SpringBootTest
class ApplicationTests {

    @Resource
    ReportRepository reportRepository;

    @Resource
    LibRepository libRepository;


    @Test
    void smsExcel() {
        for (int i = 1; i < 7; i++) {
            String path = "src/main/resources/exceldata/sms-m-" + i + ".xlsx";
            System.out.println(path);
            EasyExcel.read(path, ReportExcelModel.class, new ReportReadListener(reportRepository)).sheet().doRead();
        }
    }

    @Test
    void graphicExcel(){
        for (int i = 1; i < 7; i++) {
            String path1 = "src/main/resources/exceldata/graphic-m-" + i + ".xlsx";
            EasyExcel.read(path1, ReportExcelModel.class, new ReportReadListener(reportRepository)).sheet().doRead();
        }

    }

    @Test
    void libExcel() {
        String path = "src/main/resources/exceldata/lib.xlsx";
        EasyExcel.read(path, LibExcelModel.class, new LibReadListener(libRepository)).sheet().doRead();
    }

}
