package com.zaozao.Constant;

/**
 * @author Sidney 2019-03-14.
 */
public class AlipayTradeStatusConstant {
    /**
     * WAIT_BUYER_PAY	交易创建，等待买家付款
     * TRADE_CLOSED	未付款交易超时关闭，或支付完成后全额退款
     * TRADE_SUCCESS	交易支付成功
     * TRADE_FINISHED	交易结束，不可退款
     */
    // 支付宝账单状态,如:回调中
    public static final String WAIT_BUYER_PAY = "WAIT_BUYER_PAY";
    public static final String TRADE_CLOSED = "TRADE_CLOSED";
    public static final String TRADE_SUCCESS = "TRADE_SUCCESS";
    public static final String TRADE_FINISHED = "TRADE_FINISHED";

    // 支付宝账单状态同步接口status
    public static final byte MODIFY_STATUS_PAYED = 1;
    public static final byte MODIFY_STATUS_CLOSE = 2;
    public static final byte MODIFY_STATUS_REFUND = 3;

    /**
     * 查询接口返回的订单状态常量
     * NOT_PAY 待缴费
     * PAYING 支付中
     * PAY_SUCCESS 支付成功，处理中
     * BILLING_SUCCESS 缴费成功
     * TIMEOUT_CLOSED 逾期关闭账单
     * ISV_CLOSED 账单关闭
     */
    public static final String QUERY_NOT_PAY = "NOT_PAY";
    public static final String QUERY_PAYING = "PAYING";
    public static final String QUERY_PAY_SUCCESS = "PAY_SUCCESS";
    public static final String QUERY_BILLING_SUCCESS = "BILLING_SUCCESS";
    public static final String QUERY_TIMEOUT_CLOSED = "TIMEOUT_CLOSED";
    public static final String QUERY_ISV_CLOSED = "ISV_CLOSED";

}
