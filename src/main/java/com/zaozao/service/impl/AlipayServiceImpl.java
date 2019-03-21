package com.zaozao.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.request.*;
import com.alipay.api.response.*;
import com.zaozao.Constant.*;
import com.zaozao.dao.*;
import com.zaozao.dataobject.*;
import com.zaozao.error.BusinessException;
import com.zaozao.error.EmBusinessError;
import com.zaozao.service.AlipayService;
import com.zaozao.util.AlipayConfig;
import com.zaozao.util.OutTradeNoUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import static com.zaozao.Constant.OrderConstant.ALIPAY_MODIFY_FAIL;
import static com.zaozao.Constant.OrderConstant.ALIPAY_MODIFY_SUCCESS;

/**
 * @author Sidney 2019-03-07.
 */
@Service
public class AlipayServiceImpl implements AlipayService {

//    @Autowired
//    AlipayClient alipayClient;

    @Autowired
    private SchoolDOMapper schoolDOMapper;

    @Autowired
    private IsvDOMapper isvDOMapper;

    @Autowired
    private IsvAlipayInfoDOMapper isvAlipayInfoDOMapper;

    @Autowired
    private BillDOMapper billDOMapper;

    @Autowired
    private BillItemDOMapper billItemDOMapper;

    @Autowired
    private AlipayOrderInfoDOMapper alipayOrderInfoDOMapper;

    /**
     * (1）获得学校“第三方应用授权”
     * （2）发送学校信息
     * （3）发送缴费账单
     * （4）用户支付成功，将缴费账单状态更新为“缴费成功”
     * （5）如果发出账单、账单逾期或者已经通过其它渠道完成了支付，将缴费账单状态更新为“关闭”
     * （6）如果给用户退费，调用手机网站支付的退款接口进行退款，同时将缴费账单状态更新为“退费”
     */
    private Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    /**
     * 1-1 商家扫码
     * 根据schoolId-->isvId-->服务商alipay相关配置
     * 授权链接中配置的redirect_uri内容需要与应用中配置的授权回调地址完全一样，否则无法正常授权。
     */ public String authorizeBySchool(Integer schoolId) {
        //拼接规则:
        // https://openauth.alipay.com/oauth2/appToAppAuth.htm?app_id=2015101400446982&redirect_uri=http%3A%2F%2Fexample.com
        SchoolDO schoolDO = schoolDOMapper.selectByPrimaryKey(schoolId);
        Integer isvId = schoolDO.getIsvId();
        IsvAlipayInfoDOWithBLOBs isvAlipayInfoDOWithBLOBs = isvAlipayInfoDOMapper.selectByIsvId(isvId);
        String appIdApplet = isvAlipayInfoDOWithBLOBs.getAppIdApplet();
        String authUri =
                "https://openauth.alipay.com/oauth2/appToAppAuth.htm?app_id=" + appIdApplet + "&redirect_uri"
                        + "=";
        String redirectUri = AlipayConfig.REDIRECT_URI + schoolId;
        // url需要UrlEncode
        String encode = "";
        try {
            encode = URLEncoder.encode(redirectUri, "GBK");
        } catch (UnsupportedEncodingException e) {
            logger.error("[商家扫码,UrlEncode异常:]", e);
        }
        return authUri + encode;
    }

    /**
     * 1-2回调获取授权
     * 通过app_auth_code可以换取app_auth_token、授权商户的userId以及授权商户AppId
     * 应用授权的app_auth_code唯一的；
     * app_auth_code使用一次后失效，一天（从生成app_auth_code开始的24小时）未被使用自动过期； app_auth_token永久有效。
     * @param app_auth_code 授权码
     *                      在SDK中带上app_auth_token代码示例:request.putOtherTextParam("app_auth_token",
     *                      "201611BB888ae9acd6e44fec9940d09201abfE16");
     */
    @Override
    public String authCodeToToken(Integer schoolId, String app_id, String app_auth_code) {
        // 根据isvId初始化alipayClient
        SchoolDO schoolDO = schoolDOMapper.selectByPrimaryKey(schoolId);
        Integer isvId = schoolDO.getIsvId();
        AlipayClient alipayClient = AlipayConfig.getAlipayClient(isvId);
        String result = "授权失败";
        AlipayOpenAuthTokenAppRequest request = new AlipayOpenAuthTokenAppRequest();
        JSONObject bizContent = new JSONObject();
        bizContent.put("grant_type", "authorization_code");
        bizContent.put("code", app_auth_code);
        request.setBizContent(bizContent.toString());
        try {
            AlipayOpenAuthTokenAppResponse response = alipayClient.execute(request);
            logger.debug("----response----" + response.getBody());
            if ("10000".equals(response.getCode())) {
                logger.debug("第三方应用授权--调用成功");
                // 商户授权令牌
                String appAuthToken = response.getAppAuthToken();
                // 刷新令牌时使用
                String appRefreshToken = response.getAppRefreshToken();
                // 授权商户的AppId
                String authAppId = response.getAuthAppId();
                // 授权商户的ID,即为签约的学校账户的pid
                String userId = response.getUserId();
                // 商户授权令牌等信息存入数据库
                schoolDO.setAlipayAppAuthToken(appAuthToken);
                schoolDO.setAlipayAppRefreshToken(appRefreshToken);
                schoolDO.setAlipayAuthAppId(authAppId);
                schoolDO.setAlipaySchoolPid(userId);
                int i = schoolDOMapper.insertSelective(schoolDO);
                if (i == 1) {
                    result = "授权成功";
                }
            } else {
                logger.error("第三方应用授权--调用失败");
            }
        } catch (AlipayApiException e) {
            logger.error("第三方应用授权--异常:", e);
        }
        return result;
    }

    @Override
    public String ecoSchoolInfo(Integer schoolId, Integer isvId) throws BusinessException {
        AlipayClient alipayClient = AlipayConfig.getAlipayClient(isvId);
        if (alipayClient==null) {
            throw new BusinessException(EmBusinessError.CLIENT_INIT_ERROR);
        }
        String result = "发送学校信息至支付宝平台失败";
        AlipayEcoEduKtSchoolinfoModifyRequest request = new AlipayEcoEduKtSchoolinfoModifyRequest();
        JSONObject bizContent = new JSONObject();
        // 将参数存入bizContent
        // 获取学校信息
        SchoolDO schoolDO = schoolDOMapper.selectByPrimaryKey(schoolId);
        String schoolName = schoolDO.getSchoolName();
        String schoolType = schoolDO.getSchoolType().toString();
        String provinceCode = schoolDO.getProvinceCode();
        String provinceName = schoolDO.getProvinceName();
        String cityCode = schoolDO.getCityCode();
        String cityName = schoolDO.getCityName();
        String districtCode = schoolDO.getDistrictCode();
        String districtName = schoolDO.getDistrictName();
        String alipaySchoolPid = schoolDO.getAlipaySchoolPid();
        // 获取isv信息
        IsvDO isvDO = isvDOMapper.selectByPrimaryKey(isvId);
        String companyName = isvDO.getCompanyName();
        String companyTel = isvDO.getCompanyTel();
        String isvNotifyUrl = AlipayConfig.ISV_NOTIFY_URL;
        IsvAlipayInfoDOWithBLOBs isvAlipayInfoDOWithBLOBs = isvAlipayInfoDOMapper.selectByIsvId(isvId);
        String isvPid = isvAlipayInfoDOWithBLOBs.getIsvPid();
        bizContent.put("school_name", schoolName);
        bizContent.put("school_type", schoolType);
        bizContent.put("province_code", provinceCode);
        bizContent.put("province_name", provinceName);
        bizContent.put("city_code", cityCode);
        bizContent.put("city_name", cityName);
        bizContent.put("district_code", districtCode);
        bizContent.put("district_name", districtName);
        bizContent.put("isv_name", companyName);
        bizContent.put("isv_notify_url", isvNotifyUrl);
        bizContent.put("isv_pid", isvPid);
        bizContent.put("school_pid", alipaySchoolPid);
        bizContent.put("isv_phone", companyTel);
        //设置业务参数
        request.setBizContent(bizContent.toString());
        AlipayEcoEduKtSchoolinfoModifyResponse response = null;
        try {
            response = alipayClient.execute(request);
            System.out.println("pre return:" + response.getSchoolNo());
            if (response.isSuccess()) {
                System.out.println("发送学校信息--调用成功");
                String status = response.getStatus();
                if ("Y".equals(status)) {
                    String schoolNo = response.getSchoolNo();
                    // 将支付宝学校编号存入学校表,设置学校的同步状态为成功
                    SchoolDO s = new SchoolDO();
                    s.setId(schoolId);
                    s.setAlipaySchoolNo(schoolNo);
                    s.setSchoolStatus(SchoolConstant.SCHOOL_SEND_SUCCESS);
                    int i = schoolDOMapper.updateByPrimaryKeySelective(s);
                    if (i == 1) {
                        result = "发送学校信息至支付宝平台成功";
                    }
                } else {
                    result = "发送学校信息至支付宝平台失败,原因:" + response.getSubMsg();
                    SchoolDO s = new SchoolDO();
                    s.setId(schoolId);
                    s.setSchoolStatus(SchoolConstant.SCHOOL_SEND_FAIL);
                    schoolDOMapper.updateByPrimaryKeySelective(s);
                }
            } else {
                System.out.println("发送学校信息--调用失败");
                SchoolDO s = new SchoolDO();
                s.setId(schoolId);
                s.setSchoolStatus(SchoolConstant.SCHOOL_SEND_FAIL);
                schoolDOMapper.updateByPrimaryKeySelective(s);
            }
        } catch (AlipayApiException e) {
            logger.error("发送学校信息--异常:", e);
            SchoolDO s = new SchoolDO();
            s.setId(schoolId);
            s.setSchoolStatus(SchoolConstant.SCHOOL_SEND_FAIL);
            schoolDOMapper.updateByPrimaryKeySelective(s);
        }
        return result;
    }

    /**
     * 账单发送
     * @param idList 需同步的账单id集合
     */
    @Override
//    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
    public void ecoBillSend(List<Integer> idList) throws AlipayApiException, BusinessException {
        // 将传入的账单id集合-->符合发送账单接口的信息<--返回order_no
        for (Integer id : idList) {
            // 先在订单表查询是否该笔订单存在同步失败的记录
            AlipayOrderInfoDO alipayOrderInfoDOExist =
                    alipayOrderInfoDOMapper.selectByBillIdAndOrderStatus(id, OrderConstant.ALIPAY_ORDER_SYN_FAIL);
            if (alipayOrderInfoDOExist != null) {
                // 如果存在则继续使用该outTradeNo
                String outTradeNo = alipayOrderInfoDOExist.getOutTradeNo();
                alipayOrderInfoDOExist.setSendDate(new Date());
                BillDO billDO = billDOMapper.selectByPrimaryKey(id);
                // 已删除,已同步,已支付,无须同步的账单不支持同步到支付宝平台,同步失败?
                if (!DeleteStatusConstant.DELETE_STATUS_STAY.equals(billDO.getDeleteStatus())
                        || BillConstant.BILL_SYN_STATUS_SYN == billDO.getSynStatus() || BillConstant.BILL_STATUS_PAID == billDO
                        .getBillStatus()|| BillConstant.BILL_SYN_STATUS_SYN_NEEDLESS == billDO
                        .getBillStatus()) {
                    throw new BusinessException(EmBusinessError.BILL_STATUS_ERROR);
                }
                Integer schoolId = billDO.getSchoolId();
                Integer isvId = billDO.getIsvId();
                // 落库;此处获得alipayClient
                AlipayClient alipayClient = AlipayConfig.getAlipayClient(isvId);
                if (alipayClient==null) {
                    throw new BusinessException(EmBusinessError.CLIENT_INIT_ERROR);
                }
                SchoolDO schoolDO = schoolDOMapper.selectByPrimaryKey(schoolId);
                IsvAlipayInfoDOWithBLOBs isvAlipayInfoDOWithBLOBs = isvAlipayInfoDOMapper.selectByIsvId(isvId);
                Integer billItemId = billDO.getBillItemId();
                BillItemDO billItemDO = billItemDOMapper.selectByPrimaryKey(billItemId);
                // 转化成符合发送账单接口的信息
                String alipaySchoolPid = schoolDO.getAlipaySchoolPid();
                String alipaySchoolNo = schoolDO.getAlipaySchoolNo();
                String studentName = billDO.getStudentName();
                Integer classNum = billDO.getClassNum();
                String studentIdentity = billDO.getStudentIdentity();
                String billName = billDO.getBillName();
                String itemName = billItemDO.getItemName();
                Double billAmount = billDO.getBillAmount();
                Date endDate = billDO.getEndDate();
                String gmtEnd = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(endDate);
                String isvPid = isvAlipayInfoDOWithBLOBs.getIsvPid();
                // 以上参数-->存入alipay_order_info
                AlipayOrderInfoDO alipayOrderInfoDO = new AlipayOrderInfoDO();
                alipayOrderInfoDO.setBillId(id);
                alipayOrderInfoDO.setOutTradeNo(outTradeNo);
                alipayOrderInfoDO.setBillAmount(billAmount);
                alipayOrderInfoDO.setEndDate(endDate);
                //            alipayOrderInfoDOMapper.insertSelective(alipayOrderInfoDO);
                // 入参
                AlipayEcoEduKtBillingSendRequest request = new AlipayEcoEduKtBillingSendRequest();
                JSONObject bizContent = new JSONObject();
                bizContent.put("school_pid", alipaySchoolPid);
                bizContent.put("school_no", alipaySchoolNo);
                bizContent.put("child_name", studentName);
                bizContent.put("class_in", classNum);
                bizContent.put("student_identify", studentIdentity);
                bizContent.put("out_trade_no", outTradeNo);
                bizContent.put("charge_bill_title", billName);
                // TODO CHECK-缴费项-不可选,查看效果
                bizContent.put("charge_type", "N");
                // json里套对象-缴费明细
                ChargeItemDO chargeItemDO = new ChargeItemDO();
                chargeItemDO.setItemName(itemName);
                chargeItemDO.setItemPrice(new BigDecimal(billAmount));
                JSONObject chargeItems = new JSONObject();
                chargeItems.put("item_name", chargeItemDO.getItemName());
                chargeItems.put("item_price", chargeItemDO.getItemPrice());
                bizContent.put("charge_item", chargeItems.toJSONString());
                // 入参
                bizContent.put("amount", new BigDecimal(billAmount));
                bizContent.put("gmt_end", gmtEnd);
                // Y为gmt_end生效，用户过期后，不能再缴费
                bizContent.put("end_enable", "Y");
                // isvPid
                bizContent.put("partner_id", isvPid);
                request.setBizContent(bizContent.toString());
                AlipayEcoEduKtBillingSendResponse response = alipayClient.execute(request);
                System.out.println("pre return:" + response.getOrderNo());
                if ("10000".equals(response.getCode())) {
                    System.out.println("调用成功");
                    String orderNo = response.getOrderNo();
                    // 将支付宝账单编号存入数据库
                    alipayOrderInfoDO.setOrderNo(orderNo);
                    alipayOrderInfoDO.setOrderStatus(OrderConstant.ALIPAY_ORDER_SYN);
                    int i = alipayOrderInfoDOMapper.updateByPrimaryKeySelective(alipayOrderInfoDO);
                    if (i == 1) {
                        billDO.setSynStatus(BillConstant.BILL_SYN_STATUS_SYN);
                        billDOMapper.updateByPrimaryKeySelective(billDO);
                    }
                } else {
                    // 同步再次失败,update两表:有些重复?
                    System.out.println("调用失败");
                    logger.info("发送账单至支付宝平台失败,原因为:" + response.getSubMsg());
                    alipayOrderInfoDO.setOrderStatus(OrderConstant.ALIPAY_ORDER_SYN_FAIL);
                    int i = alipayOrderInfoDOMapper.updateByPrimaryKeySelective(alipayOrderInfoDO);
                    if (i == 1) {
                        billDO.setSynStatus(BillConstant.BILL_SYN_STATUS_SYN_FAIL);
                        billDOMapper.updateByPrimaryKeySelective(billDO);
                    }
                }
            } else {
                // 不存在同步失败--新账单
                BillDO billDO = billDOMapper.selectByPrimaryKey(id);
                // 已删除,已同步,已支付的账单不支持同步到支付宝平台,同步失败?
                if (!DeleteStatusConstant.DELETE_STATUS_STAY.equals(billDO.getDeleteStatus())
                        || BillConstant.BILL_SYN_STATUS_SYN == billDO.getSynStatus() || BillConstant.BILL_STATUS_PAID == billDO
                        .getBillStatus()|| BillConstant.BILL_SYN_STATUS_SYN_NEEDLESS == billDO
                        .getBillStatus()) {
                    throw new BusinessException(EmBusinessError.BILL_STATUS_ERROR);
                }
                Integer schoolId = billDO.getSchoolId();
                Integer isvId = billDO.getIsvId();
                // 落库;此处获得alipayClient
                AlipayClient alipayClient = AlipayConfig.getAlipayClient(isvId);
                SchoolDO schoolDO = schoolDOMapper.selectByPrimaryKey(schoolId);
                IsvAlipayInfoDOWithBLOBs isvAlipayInfoDOWithBLOBs = isvAlipayInfoDOMapper.selectByIsvId(isvId);
                Integer billItemId = billDO.getBillItemId();
                BillItemDO billItemDO = billItemDOMapper.selectByPrimaryKey(billItemId);
                // 转化成符合发送账单接口的信息
                String alipaySchoolPid = schoolDO.getAlipaySchoolPid();
                String alipaySchoolNo = schoolDO.getAlipaySchoolNo();
                String studentName = billDO.getStudentName();
                Integer classNum = billDO.getClassNum();
                String studentIdentity = billDO.getStudentIdentity();
                // 生成out_trade_no(规则:时间+随机数+账单id)-->存入alipay_order_info
                // 教育缴费平台使用入参数中的partner_pid和out_trade_no来唯一标识缴费账单。当重复传入缴费账单时，平台不会新建账单，会返回原账单编号
                // 教育缴费平台--账单状态同步接口只允许修改账单状态
                // 对于账单记录而言,billId是不变的,生成out_trade_no后,如在后台对该账单记录修改:修改金额,需要在同步接口将该账单关闭;再生成新的out_trade_no发送账单
                // 在alipay-order_info表:一个billId可能对应多个out_trade_no;新增orderStatus:标记订单状态--未同步0已同步1已关闭2已支付3 退款?
                // 回调校验中应该对传入的out_trade_no进行判空后状态校验必须是1已同步才可以进入业务修改
                // TODO CHECK-流程写完再检查是否已经保证一笔账单对应唯一的编号:在初次发送账单的时候生成out_trade_no;后台修改后生成新的out_trade_no
                String outTradeNo = OutTradeNoUtil.getOutTradeNo(id);
                String billName = billDO.getBillName();
                String itemName = billItemDO.getItemName();
                Double billAmount = billDO.getBillAmount();
                Date endDate = billDO.getEndDate();
                String gmtEnd = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(endDate);
                String isvPid = isvAlipayInfoDOWithBLOBs.getIsvPid();
                // 以上参数-->存入alipay_order_info
                AlipayOrderInfoDO alipayOrderInfoDO = new AlipayOrderInfoDO();
                alipayOrderInfoDO.setBillId(id);
                alipayOrderInfoDO.setOutTradeNo(outTradeNo);
                alipayOrderInfoDO.setBillAmount(billAmount);
                alipayOrderInfoDO.setSendDate(new Date());
                alipayOrderInfoDO.setEndDate(endDate);
                //            alipayOrderInfoDOMapper.insertSelective(alipayOrderInfoDO);
                // 入参
                AlipayEcoEduKtBillingSendRequest request = new AlipayEcoEduKtBillingSendRequest();
                JSONObject bizContent = new JSONObject();
                bizContent.put("school_pid", alipaySchoolPid);
                bizContent.put("school_no", alipaySchoolNo);
                bizContent.put("child_name", studentName);
                bizContent.put("class_in", classNum);
                bizContent.put("student_identify", studentIdentity);
                bizContent.put("out_trade_no", outTradeNo);
                bizContent.put("charge_bill_title", billName);
                // TODO CHECK-缴费项-不可选,查看效果
                bizContent.put("charge_type", "N");
                // json里套对象-缴费明细
                ChargeItemDO chargeItemDO = new ChargeItemDO();
                chargeItemDO.setItemName(itemName);
                chargeItemDO.setItemPrice(new BigDecimal(billAmount));
                JSONObject chargeItems = new JSONObject();
                chargeItems.put("item_name", chargeItemDO.getItemName());
                chargeItems.put("item_price", chargeItemDO.getItemPrice());
                bizContent.put("charge_item", chargeItems.toJSONString());
                // 入参
                bizContent.put("amount", new BigDecimal(billAmount));
                bizContent.put("gmt_end", gmtEnd);
                // Y为gmt_end生效，用户过期后，不能再缴费
                bizContent.put("end_enable", "Y");
                // isvPid
                bizContent.put("partner_id", isvPid);
                request.setBizContent(bizContent.toString());
                AlipayEcoEduKtBillingSendResponse response = alipayClient.execute(request);
                System.out.println("pre return:" + response.getOrderNo());
                if ("10000".equals(response.getCode())) {
                    System.out.println("调用成功");
                    String orderNo = response.getOrderNo();
                    // 将支付宝账单编号存入数据库
                    alipayOrderInfoDO.setOrderNo(orderNo);
                    alipayOrderInfoDO.setOrderStatus(OrderConstant.ALIPAY_ORDER_SYN);
                    int i = alipayOrderInfoDOMapper.insertSelective(alipayOrderInfoDO);
                    if (i == 1) {
                        billDO.setSynStatus(BillConstant.BILL_SYN_STATUS_SYN);
                        billDOMapper.updateByPrimaryKeySelective(billDO);
                    }
                } else {
                    // 同步失败,如直接抛错,方法就中断了-->订单表落库,记录该笔账单的状态是同步失败(日志+数据库);
                    // 对于在订单表中记录为同步失败的订单,当用户再次发起同步的时候,继续对该记录进行操作,即使用原来的outTradeNo
                    System.out.println("调用失败");
                    logger.info( "发送账单至支付宝平台失败,原因为:" + response.getSubMsg());
                    alipayOrderInfoDO.setOrderStatus(OrderConstant.ALIPAY_ORDER_SYN_FAIL);
                    int i = alipayOrderInfoDOMapper.insertSelective(alipayOrderInfoDO);
                    if (i == 1) {
                        billDO.setSynStatus(BillConstant.BILL_SYN_STATUS_SYN_FAIL);
                        billDOMapper.updateByPrimaryKeySelective(billDO);
                    }
                    //                throw new BusinessException(EmBusinessError.SEND_BILL_ERROR,
                    //                        "发送账单至支付宝平台失败,原因为:" + response.getSubMsg());
                }
            }
        }
    }

    // 账单状态同步-收到支付成功的情况下,收到异步通知后返回支付宝该笔账单状态,支付宝收到返回Y,否则应该调用查询接口查看状态
    @Override
    public void ecoBillModify(Integer isvId, String isvOrderNo, String tradeNo, Byte tradeStatus) {
        AlipayClient alipayClient = AlipayConfig.getAlipayClient(isvId);
        // TODO 对alipayClient的判空?
        if (alipayClient == null) {
            return;
        }
        AlipayEcoEduKtBillingModifyRequest request = new AlipayEcoEduKtBillingModifyRequest();
        JSONObject bizContent = new JSONObject();
        // 入参
        bizContent.put("trade_no", tradeNo);
        bizContent.put("out_trade_no", isvOrderNo);
        bizContent.put("status", tradeStatus.toString());
        request.setBizContent(bizContent.toString());
        AlipayEcoEduKtBillingModifyResponse response;
        // 同步返回值落库
        try {
            response = alipayClient.execute(request);
            if (response.isSuccess()) {
                System.out.println("调用成功");
                if ("10000".equals(response.getCode())) {
                    // 同步成功记入数据库
                    AlipayOrderInfoDO alipayOrderInfoDO = alipayOrderInfoDOMapper.selectByOutTradeNo(isvOrderNo);
                    alipayOrderInfoDO.setModifyStatus(ALIPAY_MODIFY_SUCCESS);
                    alipayOrderInfoDOMapper.updateByPrimaryKeySelective(alipayOrderInfoDO);
                } else {
                    AlipayOrderInfoDO alipayOrderInfoDO = alipayOrderInfoDOMapper.selectByOutTradeNo(isvOrderNo);
                    alipayOrderInfoDO.setModifyStatus(ALIPAY_MODIFY_FAIL);
                    alipayOrderInfoDOMapper.updateByPrimaryKeySelective(alipayOrderInfoDO);
                    System.out.println("同步失败");
                    logger.info("同步失败,原因为:"+response.getSubMsg());
                }
            } else {
                AlipayOrderInfoDO alipayOrderInfoDO = alipayOrderInfoDOMapper.selectByOutTradeNo(isvOrderNo);
                alipayOrderInfoDO.setModifyStatus(ALIPAY_MODIFY_FAIL);
                alipayOrderInfoDOMapper.updateByPrimaryKeySelective(alipayOrderInfoDO);
                System.out.println("调用失败");
                logger.info("调用失败,原因为:"+response.getSubMsg());
            }
        } catch (AlipayApiException e) {
            AlipayOrderInfoDO alipayOrderInfoDO = alipayOrderInfoDOMapper.selectByOutTradeNo(isvOrderNo);
            alipayOrderInfoDO.setModifyStatus(ALIPAY_MODIFY_FAIL);
            alipayOrderInfoDOMapper.updateByPrimaryKeySelective(alipayOrderInfoDO);
            logger.error("同步账单状态异常:", e);
        }
    }

    // 账单状态同步-对已同步账单:关闭的情况下
    @Override
    public void ecoBillModify(Integer isvId, String isvOrderNo, Byte tradeStatus) {
        AlipayClient alipayClient = AlipayConfig.getAlipayClient(isvId);
        // TODO 对alipayClient的判空?
        if (alipayClient == null) {
            return;
        }
        AlipayEcoEduKtBillingModifyRequest request = new AlipayEcoEduKtBillingModifyRequest();
        JSONObject bizContent = new JSONObject();
        // 入参
        bizContent.put("out_trade_no", isvOrderNo);
        bizContent.put("status", tradeStatus.toString());
        request.setBizContent(bizContent.toString());
        AlipayEcoEduKtBillingModifyResponse response;
        // 同步返回值落库
        try {
            response = alipayClient.execute(request);
            if (response.isSuccess()) {
                System.out.println("调用成功");
                if ("10000".equals(response.getCode())) {
                    // 同步成功记入数据库
                    AlipayOrderInfoDO alipayOrderInfoDO = alipayOrderInfoDOMapper.selectByOutTradeNo(isvOrderNo);
                    alipayOrderInfoDO.setModifyStatus(ALIPAY_MODIFY_SUCCESS);
                    alipayOrderInfoDOMapper.updateByPrimaryKeySelective(alipayOrderInfoDO);
                } else {
                    AlipayOrderInfoDO alipayOrderInfoDO = alipayOrderInfoDOMapper.selectByOutTradeNo(isvOrderNo);
                    alipayOrderInfoDO.setModifyStatus(ALIPAY_MODIFY_FAIL);
                    alipayOrderInfoDOMapper.updateByPrimaryKeySelective(alipayOrderInfoDO);
                    System.out.println("同步失败");
                    logger.info("同步失败,原因为:"+response.getSubMsg());
                }
            } else {
                AlipayOrderInfoDO alipayOrderInfoDO = alipayOrderInfoDOMapper.selectByOutTradeNo(isvOrderNo);
                alipayOrderInfoDO.setModifyStatus(ALIPAY_MODIFY_FAIL);
                alipayOrderInfoDOMapper.updateByPrimaryKeySelective(alipayOrderInfoDO);
                System.out.println("调用失败");
                logger.info("调用失败,原因为:"+response.getSubMsg());
            }
        } catch (AlipayApiException e) {
            AlipayOrderInfoDO alipayOrderInfoDO = alipayOrderInfoDOMapper.selectByOutTradeNo(isvOrderNo);
            alipayOrderInfoDO.setModifyStatus(ALIPAY_MODIFY_FAIL);
            alipayOrderInfoDOMapper.updateByPrimaryKeySelective(alipayOrderInfoDO);
            logger.error("同步账单状态异常:", e);
        }
    }


    // 查询接口查看状态
    @Override
    /**
     * status:
     * NOT_PAY 待缴费
     * PAYING 支付中
     * PAY_SUCCESS 支付成功，处理中
     * BILLING_SUCCESS 缴费成功
     * TIMEOUT_CLOSED 逾期关闭账单
     * ISV_CLOSED 账单关闭
     */ public String ecoBillQuery(Integer isvId,String isvPid,String schoolPid,String outTradeNo) {
        AlipayClient alipayClient = AlipayConfig.getAlipayClient(isvId);
        // TODO 对alipayClient的判空?
        if (alipayClient == null) {
            return null;
        }
        AlipayEcoEduKtBillingQueryRequest request = new AlipayEcoEduKtBillingQueryRequest();
        JSONObject bizContent = new JSONObject();
        // 入参
        bizContent.put("isv_pid", isvPid);
        bizContent.put("school_pid", schoolPid);
        bizContent.put("out_trade_no", outTradeNo);
        request.setBizContent(bizContent.toString());
        try {
            AlipayEcoEduKtBillingQueryResponse response = alipayClient.execute(request);
            if (response.isSuccess()) {
                System.out.println("调用成功");
                return response.getOrderStatus();
            } else {
                System.out.println("调用失败");
            }
        } catch (AlipayApiException e) {
            logger.error("调用查询支付宝平台账单查询接口异常", e);
        }
        return null;
    }



}
