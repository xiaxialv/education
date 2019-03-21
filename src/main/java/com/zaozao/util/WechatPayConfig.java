package com.zaozao.util;

/**
 * 封装支付参数实体
 * @author Sidney 2019-02-16.
 */
public class WechatPayConfig {
    //公众号appid:wx352362f198d9c56b
    public static final String appid = "";
    //公众号appsecret:891f9c8031c893848960d665e218f9ce
    public static final String secret = "";
    //微信支付的商户id:1484539292
    public static final String mch_id = "";
    //微信支付的子商户id
    public static final String sub_mch_id = "";
    //服务商模式的场景appid，在小程序中拉起支付时该字段必传,即为小程序appId(特约服务商页面需手动配置)
    public static final String sub_appid = "";
    //微信支付的商户密钥asdfaASDFASDFASFasfsadf6546A84SA
    public static final String key = "";
    //支付成功后的服务器回调url,https://hz.zaozaojiaoyu.com/wechat/notify
    public static final String notify_url = "";
    //签名方式
    public static final String sign_type = "MD5";
    //交易类型
    public static final String trade_type = "JSAPI";
    //微信统一下单接口地址
    public static final String pay_url = "https://api.mch.weixin.qq.com/pay/unifiedorder";

}
