package com.iboxpay.report.excel;

import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.iboxpay.report.Lib;
import com.iboxpay.report.LibRepository;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class LibReadListener extends AnalysisEventListener<LibExcelModel> {

    private final List<LibExcelModel> data = new ArrayList<>();

    private final LibRepository libRepository;

    public LibReadListener(LibRepository libRepository) {
        this.libRepository = libRepository;
    }

    @Override
    public void invoke(LibExcelModel excelModel, AnalysisContext analysisContext) {
        //简单处理一下。1w行以内，问题不大。
        if (excelModel != null && StringUtils.hasText(excelModel.getCreateBy())
                && StringUtils.hasText(excelModel.getArtifactId())
                && StringUtils.hasText(excelModel.getVersion())) {
            data.add(excelModel);
        }
    }

    @Override
    public void doAfterAllAnalysed(AnalysisContext analysisContext) {
        //MonthReportExcelModel  --> MonthReport --> saveIt
        data.forEach(excelModel ->{
            Lib lib = LibMapper.toLib(excelModel);
            libRepository.save(lib);
        });
    }
}
