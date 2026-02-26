package com.fy20047.susan.service;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class SheetVisibilityRow {

    @ExcelProperty("分頁名稱")
    private String sheetName;

    @ExcelProperty("顯示")
    private String visible;
}
