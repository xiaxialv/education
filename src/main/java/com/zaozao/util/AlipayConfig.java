package com.zaozao.util;

import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.zaozao.dao.IsvAlipayInfoDOMapper;
import com.zaozao.dataobject.IsvAlipayInfoDOWithBLOBs;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Sidney 2019-03-07.
 */
@Configuration
public class AlipayConfig {

    @Autowired
    private static IsvAlipayInfoDOMapper isvAlipayInfoDOMapper;

    public static Map<Integer, DefaultAlipayClient> ALIPAY_CLIENT_MAP_ISV_ID = new HashMap<>();
//    public static Map<String, DefaultAlipayClient> ALIPAY_CLIENT_MAP_APP_ID = new HashMap<>();

    // 支付宝网关（固定）
    public static final String URL = "https://openapi.alipay.com/gateway.do";
    // APPID即创建应用后生成
    public static final String APP_ID = "2019030663478347";
    // 开发者私钥，由开发者自己生成
    public static final String APP_PRIVATE_KEY =
            "MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQCO05vtjiID2A8BP25Enxbib+lBqV/VZRycqZg4IdOmwwk8l29/c0XNjCGj1wbfXo+eGZovnEO8Ka/tMYPDkPiTsZ9B7TGgQiXEOMdVYCY+RVt39vsPVtHTFr+5DAES4O2a5dDawdbSqkyEIYBXzOp0U6hSZOEKu9d1tPSczpddCmU+jkYuN4JiSl7qzKdJOYPpqY2u/o3vYEB1PxaVaHdGuO4BgtFaG/1gqgXwrqEJ8butEi2V1BUGFdr5cVwAxZEPGxjPcA+e4jgdUN/bMRBAWFMqr0pWZzgUt8U7jfpc+qDqOCJeTdhAXgTsn3vzYiUxLf60u5LmD/mRl1tp8tYzAgMBAAECggEAHpvQ9ecY+RPrm0vZuNETWXG8XnsK87OYnyZlXdo5/qobp6WYmoq8seFPMEqbyXD9fFdmSL/HcGLKth1/bID8FMLjK4DGut6SM2wro+tYJs48XQhMI6xqiT991Q1yiXY01ZZc3RTfkgQ3I7X+SFK9CDMJhqbh56f789jU92n63FBTGlXUv7oiQqG2Vs7I9POuLRwlvkIfJD/v+TVUAqQwP2w8b4QcWYqCe9AKm05QfF3DJ379w8PaqybGyppLmz6j6Y3D5dkVrSCQNFkRAg2gSj/5SwUazR12JRKHifjdKCeDnI+m2qwapLU4EPXLbv5o5gsE2D2p6wafu4hSXCrmqQKBgQDtygrAMN4R/ahoSBMjvdRlYO11r7GsQ7Ohd7PuetVQpkUSQJE7rprV0XPKK4BA538mm5IfmSjUSplo0Eme3BEluVzb5wAbSU0HmB72gdeUQLGTlFSo9JpE2oh5jm0S8g6802ch4yO9P3wlGuRr8Ml4EapoTlGrN96etwbZ7YqJLwKBgQCZw8jk8VupuMBZPCPfmKcZYTG3UC0yC6OA4GSVhZUoa4xzCxmT0S67I3reg5CfoB59tvaitOEWgAC9TeZICt9XX2jMlMsZRt5+NLeehkSSVEHCnPaVv8ZdiVCMiZi1YkUB9ka0gSl2NyWg7rI3F8wiJrcZcsfYP2qqVuQGQvy6PQKBgQCyEhmW74F9y40v+rmKjH/gI4PwG9BcBp8gjfnK32WbbchP8NZR8WG2OZISlh0HfdT0XpBA5Xtz7yGPIlvfgUG+FF/7tbOYu33Z8AtP4tBED2Zr9CqqE/LrGRQWo5f/FhH/SfjzoZuD/2TCjK7+PmVBa0lHr07tf3tRNBMHZCHYtQKBgCyq4UsQig4tq0LA47Lqh3Ap+eVElzx4gRDJHq8I1SGezHgG+3EjTS3zypelJHgabYEbi14MqW591LkpTgITlnmtL6Zueulm2u9SpRyZ6IWRkGDWdBNgWGGHqCyLNB9M4rsFLdj1xHOg5X9wl7jFYvlwwbPU0hURpwzAnRDsmLoJAoGALX0YtYciYMrbzzHB4gU9lgnTI/Kob6sc3yKahvdZz8jim1mKqf3KKKrhUpZ16WDPpSH6F/VkPhvJEJQM7qBFP9R7x26DukPChzpef3oj755qvexeE2TRRpsK+dcCEgImxrt7SsY8IRGXgh8CimOn9teowxzG2yISRSqGMkx2W4M=";
    // 开发者公钥
    public static final String APP_PUBLIC_KEY =
            "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAjtOb7Y4iA9gPAT9uRJ8W4m/pQalf1WUcnKmYOCHTpsMJPJdvf3NFzYwho9cG316PnhmaL5xDvCmv7TGDw5D4k7GfQe0xoEIlxDjHVWAmPkVbd/b7D1bR0xa/uQwBEuDtmuXQ2sHW0qpMhCGAV8zqdFOoUmThCrvXdbT0nM6XXQplPo5GLjeCYkpe6synSTmD6amNrv6N72BAdT8WlWh3RrjuAYLRWhv9YKoF8K6hCfG7rRItldQVBhXa+XFcAMWRDxsYz3APnuI4HVDf2zEQQFhTKq9KVmc4FLfFO436XPqg6jgiXk3YQF4E7J9782IlMS3+tLuS5g/5kZdbafLWMwIDAQAB";
    // 参数返回格式，只支持json
    public static final String FORMAT = "json";
    // 编码集，支持GBK/UTF-8
    public static final String CHARSET = "UTF-8";
    // 支付宝公钥，由支付宝生成
    public static final String ALIPAY_PUBLIC_KEY =
            "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAhPsFFqBw3L0t/xp2lqH8HggsNjyn6b1rvT3BhH2Wtn8L3fEbsMa0Y0MZIjv0zhiFZjhhMSvBtvaW/6zTBPzPxhJxZAnIrSiVc43Z4Cz0vDsokXE268rFdiNQ4/IKHht/omU0IfW9A45VYy+PdwJwIiCatiRo0wEeQ85HVDzf5EgDHY0Eog2cdxo5tkyJCHkLvePSkxlO0ne1M9ewUE/VpE4uIuPq6nJB8bBTC4HUaw6X0dhDeXAaBeU2U56CUm+CJob6muxxZftImU3QIY/SqjRU2DIof5QivXOGniM0uwIX2IkQ89C1t93PVPfPQtto8avM6iaQ5UgsTLQ/L7iOPQIDAQAB";
    // 商户生成签名字符串所使用的签名算法类型，目前支持RSA2和RSA，推荐使用RSA2
    public static final String SIGN_TYPE = "RSA2";
    // 授权回调地址 TODO CHECK-回调页面平台设置的是不带参数的,测试的时候确认是否ok
    public static final String REDIRECT_URI = "https://hz.zaozaojiaoyu.com/alipay/auth/notify?schoolId=";
    // 用户支付成功后，支付宝异步通知地址
    public static final String ISV_NOTIFY_URL = "https://hz.zaozaojiaoyu.com/alipay/pay/notify";

    //    @Bean
    //    public AlipayClient getAlipayClient() {
    //        return new DefaultAlipayClient(URL, AlipayConfig.APP_ID, AlipayConfig.APP_PRIVATE_KEY, AlipayConfig.FORMAT,
    //                AlipayConfig.CHARSET, AlipayConfig.ALIPAY_PUBLIC_KEY, AlipayConfig.SIGN_TYPE);
    //    }

    public static AlipayClient getAlipayClient(Integer isvId) {
        DefaultAlipayClient defaultAlipayClient = ALIPAY_CLIENT_MAP_ISV_ID.get(isvId);
        if (defaultAlipayClient == null) {
            // 通过isvId 去mysql 查询到用户的 配置信息,返回初始化后的alipayClient
            IsvAlipayInfoDOWithBLOBs isvAlipayInfoDOWithBLOBs = isvAlipayInfoDOMapper.selectByIsvId(isvId);
            if (isvAlipayInfoDOWithBLOBs != null) {
                // TODO CHECK-参数不做判空,前端控制新建该对象的时候所有字段都必传
                String appIdApplet = isvAlipayInfoDOWithBLOBs.getAppIdApplet();
                String alipayPublicKey = isvAlipayInfoDOWithBLOBs.getAlipayPublicKey();
                String appPrivateKey = isvAlipayInfoDOWithBLOBs.getAppPrivateKey();
                defaultAlipayClient =
                        new DefaultAlipayClient(URL, appIdApplet, appPrivateKey, AlipayConfig.FORMAT, AlipayConfig.CHARSET,
                                alipayPublicKey, AlipayConfig.SIGN_TYPE);
                ALIPAY_CLIENT_MAP_ISV_ID.put(isvId, defaultAlipayClient);
            }else {
                return null;
            }
        }
        return defaultAlipayClient;
    }

    // 内存刷新在支付宝配置信息被修改后使用AOP切面执行After方法
    public static void refresh(){
        ALIPAY_CLIENT_MAP_ISV_ID = new HashMap<>();
//        ALIPAY_CLIENT_MAP_APP_ID = new HashMap<>();
    }

//    public static AlipayClient getAlipayClient(String appIdApplet) {
//        // 先从内存Map中取是否有值
//        DefaultAlipayClient defaultAlipayClient = ALIPAY_CLIENT_MAP_APP_ID.get(appIdApplet);
//        if (defaultAlipayClient == null) {
//            // 通过appIdApplet 去mysql 查询到用户的 配置信息,返回初始化后的alipayClient
//            IsvAlipayInfoDOWithBLOBs isvAlipayInfoDOWithBLOBs = isvAlipayInfoDOMapper.selectByAppIdApplet(appIdApplet);
//            if (isvAlipayInfoDOWithBLOBs != null) {
//                String alipayPublicKey = isvAlipayInfoDOWithBLOBs.getAlipayPublicKey();
//                String appPrivateKey = isvAlipayInfoDOWithBLOBs.getAppPrivateKey();
//                defaultAlipayClient = new DefaultAlipayClient(URL, appIdApplet, appPrivateKey, AlipayConfig.FORMAT,
//                        AlipayConfig.CHARSET, alipayPublicKey, AlipayConfig.SIGN_TYPE);
//                ALIPAY_CLIENT_MAP_APP_ID.put(appIdApplet, defaultAlipayClient);
//            } else {
//                return null;
//            }
//        }
//        return defaultAlipayClient;
//    }


}
