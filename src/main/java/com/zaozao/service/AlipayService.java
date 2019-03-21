package com.zaozao.service;

import com.alipay.api.AlipayApiException;
import com.zaozao.error.BusinessException;

import java.util.List;

/**
 * @author Sidney 2019-03-07.
 */
public interface AlipayService {
    /**
     *  (1）获得学校“第三方应用授权”
     * （2）发送学校信息
     * （3）发送缴费账单
     * （4）用户支付成功，将缴费账单状态更新为“缴费成功”
     * （5）如果发出账单、账单逾期或者已经通过其它渠道完成了支付，将缴费账单状态更新为“关闭”
     * （6）如果给用户退费，调用手机网站支付的退款接口进行退款，同时将缴费账单状态更新为“退费”
     */
    // 1-1商家扫码
    public String authorizeBySchool(Integer schoolId);

    // 1-2回调获取授权
    public String authCodeToToken(Integer schoolId, String app_id, String app_auth_code);

    // 2-1发送学校信息
    public String ecoSchoolInfo(Integer schoolId, Integer isvId) throws BusinessException;

    // 3-1账单发送
    public void ecoBillSend(List<Integer> idList) throws AlipayApiException, BusinessException;

    // 账单状态同步-支付成功(才有tradeNo参数)
    void ecoBillModify(Integer isvId, String isvOrderNo, String tradeNo, Byte tradeStatus);

    // 账单状态同步-关闭(不含tradeNo参数)
    void ecoBillModify(Integer isvId, String isvOrderNo, Byte tradeStatus);

    // 账单状态查询,收到异步通知后返回支付宝该笔账单状态,支付宝收到返回Y,否则应该调用查询接口查看状态
    String ecoBillQuery(Integer isvId,String isvPid,String schoolPid,String outTradeNo);

}
