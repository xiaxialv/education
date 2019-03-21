package com.zaozao.Constant;

/**
 * @author Sidney 2019-01-18.
 */
public class PayTypeConstant {
    // 未支付
    public static final byte UN_PAY = 0;
    // 使用支付宝支付
    public static final byte PAY_BY_ALIPAY = 1;
    // 使用微信支付
    public static final byte PAY_BY_WECHAT = 2;
    // 使用现金支付
    public static final byte PAY_BY_CASH = 3;
    // 使用pos机收款
    public static final byte PAY_BY_POS = 4;
    // 使用其他方式支付
    public static final byte PAY_BY_OTHER = 5;

    // 异常支付,用于微信小程序支付订单金额和账单金额不一致时,需核查
    public static final byte PAY_ERROR = 99;

}
