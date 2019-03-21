package com.zaozao.controller.viewobject;

import java.math.BigDecimal;
import java.util.List;

/**
 * @author Sidney 2019-01-21.
 */
public class WechatUnpaidBillVO {
    // 学生姓名
    private String studentName;
    private List<BillInfo> billInfoList;

    public static class BillInfo{
        // 账单编号
        private Integer billId;
        // 账单名称
        private String billName;
        // 缴费类型名称
        private String billItemName;
        // 缴费金额
        private BigDecimal billAmount;
        // 账单创建时间
        private String createDate;

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

        public String getCreateDate() {
            return createDate;
        }

        public void setCreateDate(String createDate) {
            this.createDate = createDate;
        }
    }
    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public List<BillInfo> getBillInfoList() {
        return billInfoList;
    }

    public void setBillInfoList(List<BillInfo> billInfoList) {
        this.billInfoList = billInfoList;
    }
}
