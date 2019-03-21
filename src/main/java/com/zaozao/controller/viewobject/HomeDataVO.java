package com.zaozao.controller.viewobject;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * @author Sidney 2019-02-20.
 */
public class HomeDataVO {
    // 应收账款
    private BigDecimal totalAmount;
    // 已收账款
    private BigDecimal receivedAmount;
    // 未收账款
    private BigDecimal unreceivedAmount;
    // 学生数量
    private Integer studentAmount;
    // 已缴费用{学校id-name,[类型名称+小计,,,]}
    private Map<String, List<BillDetail>> paidBillDetail;

    // 未缴费用{学校id-name,[类型名称+小计,,,]}

    private Map<String, List<BillDetail>> unpaidBillDetail;

    // 内部类,账单明细实体类
    public static class BillDetail {
        private String billItemName;
        private BigDecimal billAmountByItem;

        public String getBillItemName() {
            return billItemName;
        }

        public void setBillItemName(String billItemName) {
            this.billItemName = billItemName;
        }

        public BigDecimal getBillAmountByItem() {
            return billAmountByItem;
        }

        public void setBillAmountByItem(BigDecimal billAmountByItem) {
            this.billAmountByItem = billAmountByItem;
        }
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public BigDecimal getReceivedAmount() {
        return receivedAmount;
    }

    public void setReceivedAmount(BigDecimal receivedAmount) {
        this.receivedAmount = receivedAmount;
    }

    public BigDecimal getUnreceivedAmount() {
        return unreceivedAmount;
    }

    public void setUnreceivedAmount(BigDecimal unreceivedAmount) {
        this.unreceivedAmount = unreceivedAmount;
    }

    public Integer getStudentAmount() {
        return studentAmount;
    }

    public void setStudentAmount(Integer studentAmount) {
        this.studentAmount = studentAmount;
    }

    public Map<String, List<BillDetail>> getPaidBillDetail() {
        return paidBillDetail;
    }

    public void setPaidBillDetail(Map<String, List<BillDetail>> paidBillDetail) {
        this.paidBillDetail = paidBillDetail;
    }

    public Map<String, List<BillDetail>> getUnpaidBillDetail() {
        return unpaidBillDetail;
    }

    public void setUnpaidBillDetail(Map<String, List<BillDetail>> unpaidBillDetail) {
        this.unpaidBillDetail = unpaidBillDetail;
    }
}
