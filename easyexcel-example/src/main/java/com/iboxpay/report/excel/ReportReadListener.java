package com.iboxpay.report.excel;

import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.iboxpay.report.Report;
import com.iboxpay.report.ReportRepository;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class ReportReadListener extends AnalysisEventListener<ReportExcelModel> {

    private final List<ReportExcelModel> data = new ArrayList<>();

    final ReportRepository reportRepository;

    public ReportReadListener(ReportRepository reportRepository) {
        this.reportRepository = reportRepository;
    }

    @Override
    public void invoke(ReportExcelModel excelModel, AnalysisContext analysisContext) {
        //简单处理一下。1w行以内，问题不大。
        if (excelModel != null && StringUtils.hasText(excelModel.getItemDay())) {
            data.add(excelModel);
        }
    }

    @Override
    public void doAfterAllAnalysed(AnalysisContext analysisContext) {
        //MonthReportExcelModel  --> MonthReport --> saveIt
        data.forEach(excelModel ->{
            Report report = ReportMapper.toMonthReport(excelModel);
            report.setService(excelModel.getService());
            reportRepository.save(report);
        });


    }
}
