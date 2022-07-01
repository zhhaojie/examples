package com.iboxpay.report.excel;

import com.iboxpay.report.Lib;
import org.springframework.beans.BeanUtils;

import java.util.Date;

public class LibMapper {

    public static Lib toLib(LibExcelModel excelModel) {
        Lib lib = new Lib();
        BeanUtils.copyProperties(excelModel, lib);
        if (excelModel.getCreateAt() == null) {
            lib.setCreateAt(new Date());
        }
        return lib;
    }

}
