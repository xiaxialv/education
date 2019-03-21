package com.zaozao.controller.viewobject;

import java.math.BigDecimal;

/**
 * @author Sidney 2019-01-21.
 */
public class WechatPaidBillVO {
    // 学生姓名
    private String studentName;
    // 账单编号
    private Integer billId;
    // 账单名称
    private String billName;
    // 缴费类型名称
    private String billItemName;
    // 缴费金额
    private BigDecimal billAmount;
    // 账单支付时间
    private String timeEnd;
    // 微信支付订单号
    private String transactionId;

    public Integer getBillId() {
        return billId;
    }

    public void setBillId(Integer billId) {
        this.billId = billId;
    }

    public String getBillName() {
        return billName;
    }

    public void setBillName(String billName) {
        this.billName = billName;
    }

    public String getBillItemName() {
        return billItemName;
    }

    public void setBillItemName(String billItemName) {
        this.billItemName = billItemName;
    }

    public BigDecimal getBillAmount() {
        return billAmount;
    }

    public void setBillAmount(BigDecimal billAmount) {
        this.billAmount = billAmount;
    }

    public String getTimeEnd() {
        return timeEnd;
    }

    public void setTimeEnd(String timeEnd) {
        this.timeEnd = timeEnd;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

}
