package com.zaozao.Constant;

/**
 * @author Sidney 2019-01-11.
 */
public class BillConstant {
    // 账单支付状态0未缴费
    public static final byte BILL_STATUS_UNPAID = 0;
    // 账单支付状态1已缴费
    public static final byte BILL_STATUS_PAID = 1;


    // 账单同步状态0未同步
    public static final byte BILL_SYN_STATUS_UNSYN = 0;
    // 账单同步状态1已同步
    public static final byte BILL_SYN_STATUS_SYN = 1;
    // 账单同步状态2同步失败
    public static final byte BILL_SYN_STATUS_SYN_FAIL = 2;
    // 账单同步状态3无须同步:对于其他渠道支付或者删除的账单显示为无须同步状态
    public static final byte BILL_SYN_STATUS_SYN_NEEDLESS = 3;
}
