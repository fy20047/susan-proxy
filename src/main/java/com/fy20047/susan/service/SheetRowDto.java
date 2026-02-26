package com.fy20047.susan.service;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class SheetRowDto {

    @ExcelProperty("尾款日")
    private String balanceDueDate;

    @ExcelProperty("付定日")
    private String depositPaidDate;

    @ExcelProperty("對帳")
    private String reconciled;

    @ExcelProperty("定金80%")
    private Integer depositAmount;

    @ExcelProperty("尾款20%")
    private Integer balanceAmount;

    @ExcelProperty("購買總額")
    private Integer totalAmount;

    @ExcelProperty("團友")
    private String buyerNickname;

    @ExcelProperty("報到")
    private String checkedIn;

    @ExcelProperty("喊單序")
    private String orderSn;

    @ExcelProperty("順位")
    private String orderRank;

    @ExcelProperty("是否排到")
    private String queued;

    @ExcelProperty("品項")
    private String itemName;

    @ExcelProperty("數量")
    private Integer quantity;

    @ExcelProperty("台幣單價")
    private Integer twdPrice;

    @ExcelProperty("日幣原價")
    private Integer jpyPrice;

    @ExcelProperty("購買地點")
    private String purchaseLocation;

    @ExcelProperty("IP")
    private String ip;

    @ExcelProperty("已採購")
    private String purchased;

    @ExcelProperty("出貨狀態")
    private String shipped;

    @ExcelProperty("特典")
    private String bonus;

    @ExcelProperty("對")
    private String checkMark;

    @ExcelProperty("喊單日")
    private String orderDate;
}
