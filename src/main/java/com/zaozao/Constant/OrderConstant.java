package com.zaozao.Constant;

/**
 * @author Sidney 2019-03-05.
 */
public class OrderConstant {
    // 微信订单状态-0新订单NEW/1完结订单FINISH/2取消订单CANCEL
    public static final byte WECHAT_ORDER__NEW = 0;
    public static final byte WECHAT_ORDER_FINISH = 1;
    public static final byte WECHAT_ORDER_CANCEL = 2;

    // 微信支付状态-0等待支付/1正在支付中/2支付成功/3支付失败
    public static final byte WECHAT_PAY_WAIT = 0;
    public static final byte WECHAT_PAY_PAYING = 1;
    public static final byte WECHAT_PAY_PAID = 2;
    public static final byte WECHAT_PAY_FAIL = 3;

    // 支付宝订单状态--未同步0,已同步1,同步失败2,已关闭3,已支付4
    // 回调校验中应该对传入的out_trade_no进行判空后状态校验必须是1已同步才可以进入业务修改
    public static final byte ALIPAY_ORDER_UNSYN = 0;
    public static final byte ALIPAY_ORDER_SYN = 1;
    public static final byte ALIPAY_ORDER_SYN_FAIL = 2;
    public static final byte ALIPAY_ORDER_CLOSED = 3;
    public static final byte ALIPAY_ORDER_PAYED = 4;

    // 支付宝订单修改状态--记录支付宝ecoBillModify接口返回的状态:0未发起修改,1修改成功,2修改失败
    public static final byte ALIPAY_MODIFY_NONE = 0;
    public static final byte ALIPAY_MODIFY_SUCCESS = 1;
    public static final byte ALIPAY_MODIFY_FAIL = 2;
}
