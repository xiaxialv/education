package com.zaozao.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.zaozao.Constant.*;
import com.zaozao.controller.viewobject.WechatPaidBillVO;
import com.zaozao.controller.viewobject.WechatUnpaidBillVO;
import com.zaozao.dao.*;
import com.zaozao.dataobject.*;
import com.zaozao.error.BusinessException;
import com.zaozao.error.EmBusinessError;
import com.zaozao.response.CommonReturnType;
import com.zaozao.service.AlipayService;
import com.zaozao.service.SchoolService;
import com.zaozao.service.model.SchoolModel;
import com.zaozao.util.*;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

import static com.zaozao.Constant.BillConstant.*;

/**
 * @author Sidney 2019-02-16.
 */
@Controller("wechatPayController")
@RequestMapping("/wechat")
@CrossOrigin(allowCredentials = "true", origins = "*")
public class WechatPayController extends BaseController {

    @Autowired
    private RedisOperator redis;

    @Autowired
    private StudentDOMapper studentDOMapper;

    @Autowired
    private BillItemDOMapper billItemDOMapper;

    @Autowired
    private SchoolDOMapper schoolDOMapper;

    @Autowired
    private BillDOMapper billDOMapper;

    @Autowired
    private WechatOrderInfoDOMapper wechatOrderInfoDOMapper;

    @Autowired
    private IsvWechatInfoDOMapper isvWechatInfoDOMapper;

    @Autowired
    private AlipayOrderInfoDOMapper alipayOrderInfoDOMapper;

    @Autowired
    private IsvAlipayInfoDOMapper isvAlipayInfoDOMapper;

    @Autowired
    private SchoolService schoolService;

    @Autowired
    private AlipayService alipayService;

    private Logger logger = LoggerFactory.getLogger(getClass());

    // 微信小程序appId & secret
    private static final String APP_ID = "wx7a46bba257d417a5";
    private static final String SECRET = "49a66cd9ce432c19c68133bcdffcf9dd";

    /**
     * 微信授权登录接口,用于建立小程序内的用户体系
     * 从微信请求头中Referfer中获取小程序的appid,从isv_wechat_info表中查出isv相关微信配置
     *
     * @param code 临时登录凭证
     * @return 用户唯一标识 OpenID
     * @throws Exception 异常
     */
    @RequestMapping(value = "/login", method = {RequestMethod.GET})
    @ResponseBody
    public CommonReturnType login(@RequestParam("code") String code, HttpServletRequest request) {
        // 从request获得referfer-->获取小程序appId
        String referer = request.getHeader("Referer");
        System.out.println("==请求头referfer:" + referer);
        String appIdFromHeader = this.subAppIdFromHeader(referer);
        // 获取小程序appSecret
        IsvWechatInfoDO isvWechatInfo = this.getIsvWechatInfo(appIdFromHeader);
        String appSecretApplet = isvWechatInfo.getAppSecretApplet();
        if (appSecretApplet == null) {
            appSecretApplet = "";
        }
        Map<String, Object> result = new HashMap<>();
        String s = HttpRequestUtil
                .get("https://api.weixin.qq.com/sns/jscode2session?appid=" + appIdFromHeader + "&secret="
                        + appSecretApplet + "&js_code=" + code + "&grant_type=authorization_code");
        JSONObject jsonObject = JSON.parseObject(s);
        System.out.println(s);
        result.put("openid", jsonObject.getString("openid"));
        System.out.println(result);
        // 存入session到redis
        redis.set("user-redis-session-" + appIdFromHeader + ":" + jsonObject.getString("openid"),
                jsonObject.getString("session_key"), 60 * 60 * 24 * 30);
        return CommonReturnType.create(result);
    }

    /**
     * 将微信用户的openId与学生记录进行绑定
     *
     * @param openId      用户唯一标识
     * @param studentName 学生姓名
     * @param parentPhone 家长手机号
     * @return 用户唯一标识 OpenID 和 会话密钥 session_key
     * @throws Exception 异常
     */
    @RequestMapping(value = "/bind", method = {RequestMethod.POST})
    @ResponseBody
    public CommonReturnType bindOpenId(@RequestParam("openid") String openId,
            @RequestParam("studentName") String studentName, @RequestParam("parentPhone") String parentPhone)
            throws Exception {
        // 根据条件查询学生,不含已被学校标记被删除状态的学生
        StudentDO studentDO =
                studentDOMapper.selectByNameAndPhone(studentName, parentPhone, DeleteStatusConstant.DELETE_STATUS_STAY);
        if (studentDO != null) {
            // 将微信用户的openId与学生记录进行绑定
            studentDO.setWechatOpenId(openId);
            studentDOMapper.updateByPrimaryKeySelective(studentDO);
            return CommonReturnType.create("绑定成功");
        } else {
            throw new BusinessException(EmBusinessError.STUDENT_NOT_FIND);
        }
    }

    /**
     * 将微信用户的openId与学生记录进行解绑
     *
     * @param openId      用户唯一标识
     * @param studentName 学生姓名
     * @return 用户唯一标识 OpenID 和 会话密钥 session_key
     * @throws Exception 异常
     */
    @RequestMapping(value = "/unbind", method = {RequestMethod.POST})
    @ResponseBody
    public CommonReturnType unbindOpenId(@RequestParam("openid") String openId,
            @RequestParam("studentName") String studentName) throws Exception {
        // 根据条件查询学生,不含已被学校标记被删除状态的学生
        StudentDO studentDO =
                studentDOMapper.selectByNameAndOpenId(studentName, openId, DeleteStatusConstant.DELETE_STATUS_STAY);
        if (studentDO != null) {
            // 将微信用户的openId与学生记录进行绑定
            studentDO.setWechatOpenId("");
            studentDOMapper.updateByPrimaryKeySelective(studentDO);
            return CommonReturnType.create("解绑成功");
        } else {
            throw new BusinessException(EmBusinessError.STUDENT_NOT_FIND);
        }
    }

    /**
     * 首页返回已绑定学生及未支付账单
     *
     * @param openId 用户唯一标识
     * @return 学生及账单信息
     * @throws Exception 异常
     */
    @RequestMapping(value = "/search/unpaid", method = {RequestMethod.POST})
    @ResponseBody
    public CommonReturnType searchUnpaid(@RequestParam("openid") String openId) throws Exception {
        // 根据条件查询学生,不含已被学校标记被删除状态的学生
        List<StudentDO> studentDOList =
                studentDOMapper.selectByWechatOpenId(openId, DeleteStatusConstant.DELETE_STATUS_STAY);
        List<WechatUnpaidBillVO> wechatUnpaidBillVOList = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        if (studentDOList != null) {
            for (StudentDO studentDO : studentDOList) {
                WechatUnpaidBillVO wechatUnpaidBillVO = new WechatUnpaidBillVO();
                wechatUnpaidBillVO.setStudentName(studentDO.getName());
                // 根据学生查询未缴费账单并封装为VO.BillInfo
                Integer studentId = studentDO.getId();
                List<BillDO> billDOList = billDOMapper
                        .listBillByStudentId(studentId, DeleteStatusConstant.DELETE_STATUS_STAY,
                                BillConstant.BILL_STATUS_UNPAID);
                if (billDOList != null) {
                    List<WechatUnpaidBillVO.BillInfo> billInfoList = billDOList.stream().map(billDO -> {
                        WechatUnpaidBillVO.BillInfo billInfo = new WechatUnpaidBillVO.BillInfo();
                        billInfo.setBillId(billDO.getId());
                        billInfo.setBillName(billDO.getBillName());
                        billInfo.setBillItemName(
                                billItemDOMapper.selectByPrimaryKey(billDO.getBillItemId()).getItemName());
                        billInfo.setBillAmount(new BigDecimal(billDO.getBillAmount()));
                        billInfo.setCreateDate(sdf.format(billDO.getCreateDate()));
                        return billInfo;
                    }).collect(Collectors.toList());
                    wechatUnpaidBillVO.setBillInfoList(billInfoList);
                }
                wechatUnpaidBillVOList.add(wechatUnpaidBillVO);
            }
        }
        return CommonReturnType.create(wechatUnpaidBillVOList);
    }

    /**
     * 查询该微信用户历史支付账单
     *
     * @param openId 用户唯一标识
     * @return 账单记录
     * @throws Exception 异常
     * 代码精华!
     */
    @RequestMapping(value = "/search/paid", method = {RequestMethod.POST})
    @ResponseBody
    public CommonReturnType searchPaid(@RequestParam("openid") String openId) throws Exception {
        // 根据openid查询该微信用户所有的缴费的账单
        List<BillDO> billDOList = billDOMapper
                .listBillByOpenIdAndBillStatus(openId, DeleteStatusConstant.DELETE_STATUS_STAY,
                        BillConstant.BILL_STATUS_PAID);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        // 将查询出来的账单对象转化成VO
        if (billDOList != null) {
            List<WechatPaidBillVO> wechatPaidBillVOList = billDOList.stream().map(billDO -> {
                WechatPaidBillVO wechatPaidBillVO = new WechatPaidBillVO();
                BeanUtils.copyProperties(billDO, wechatPaidBillVO);
                wechatPaidBillVO.setBillId(billDO.getId());
                wechatPaidBillVO
                        .setBillItemName(billItemDOMapper.selectByPrimaryKey(billDO.getBillItemId()).getItemName());
                wechatPaidBillVO.setBillAmount(new BigDecimal(billDO.getBillAmount().toString()));
                wechatPaidBillVO.setTimeEnd(sdf.format(billDO.getPayDate()));
                wechatPaidBillVO.setTransactionId(billDO.getSerialNum());
                return wechatPaidBillVO;
            }).collect(Collectors.toList());
            // 将查询出来的账单对象根据学生姓名分组
            Map<String, List<WechatPaidBillVO>> collect =
                    wechatPaidBillVOList.stream().collect(Collectors.groupingBy(WechatPaidBillVO::getStudentName));
            return CommonReturnType.create(collect);
        } else {
            return CommonReturnType.create(null);
        }
    }

    /**
     * 统一下单接口
     * 订单表记录值插入,create_time为prepay_id生成的时间
     *
     * @param openid  微信用户唯一标识
     * @param request request
     * @Description: 发起微信支付, 将参数返回小程序服务端最终发起微信支付, doWxPay
     */
    @RequestMapping(value = "/pay", method = {RequestMethod.POST})
    @ResponseBody
    public CommonReturnType wechatPay(@RequestParam("openid") String openid, @RequestParam("billId") Integer billId,
            HttpServletRequest request) throws BusinessException {
        // 在调用统一下单接口之前先判断该账单(如已同步支付宝)在支付宝是非支付相关状态的才允许在微信端下单
        // 否则就是未同步/同步失败/无须同步;后者禁止微信下单,前者正常微信下单
        BillDO billDO = billDOMapper.selectByPrimaryKey(billId);
        if (billDO == null) {
            throw new BusinessException(EmBusinessError.BILL_UNEXIT_ERROR);
        }
        Byte synStatus = billDO.getSynStatus();
        Integer isvId = billDO.getIsvId();
        if (synStatus == BILL_SYN_STATUS_SYN) {
            AlipayOrderInfoDO alipayOrderInfoDOExist =
                    alipayOrderInfoDOMapper.selectByBillIdAndOrderStatus(billId, OrderConstant.ALIPAY_ORDER_SYN);
            if (alipayOrderInfoDOExist != null) {
                // 调用查询接口确认要被删除的账单处于NOT_PAY 待缴费才能通过其他途径缴费,该订单方可被关闭,否则抛账单状态异常
                IsvAlipayInfoDOWithBLOBs isvAlipayInfoDOWithBLOBs = isvAlipayInfoDOMapper.selectByIsvId(isvId);
                String isvPid = isvAlipayInfoDOWithBLOBs.getIsvPid();
                SchoolModel schoolById = schoolService.getSchoolById(billDO.getSchoolId());
                String query = alipayService.ecoBillQuery(isvId, isvPid, schoolById.getAlipaySchoolPid(),
                        alipayOrderInfoDOExist.getOutTradeNo());
                if (AlipayTradeStatusConstant.QUERY_NOT_PAY.equals(query)) {
                    // 微信下单业务
                    // 从request中获取referfer
                    String referer = request.getHeader("Referer");
                    System.out.println("==请求头referfer:" + referer);
                    // 判断订单表中是否已有该账单的记录,支付状态为0/1
                    WechatOrderInfoDO wechatOrderInfoDOWait = wechatOrderInfoDOMapper
                            .selectByBillAndOpenIdExist(billId, openid, OrderConstant.WECHAT_ORDER__NEW,
                                    OrderConstant.WECHAT_PAY_WAIT);
                    WechatOrderInfoDO wechatOrderInfoDOPaying = wechatOrderInfoDOMapper
                            .selectByBillAndOpenIdExist(billId, openid, OrderConstant.WECHAT_ORDER__NEW,
                                    OrderConstant.WECHAT_PAY_PAYING);
                    logger.info("第一步:请求/pay接口--订单表:wechatOrderInfoDOWait={},wechatOrderInfoDOPaying={}",
                            wechatOrderInfoDOWait, wechatOrderInfoDOPaying);
                    if (wechatOrderInfoDOWait == null && wechatOrderInfoDOPaying == null) {
                        System.out.println("===========================新订单========================");
                        // 新生成的订单,生成商户订单号等业务处理,最终返回给前端需要的调起支付的参数
                        try {
                            IsvWechatInfoDO isvWechatInfoDO = isvWechatInfoDOMapper.selectByIsvId(isvId);
                            if (isvWechatInfoDO == null) {
                                throw new BusinessException(EmBusinessError.ISV_CONFIG_ERROR);
                            }
                            String itemName = billItemDOMapper.selectByPrimaryKey(billDO.getBillItemId()).getItemName();
                            // 商品描述(详看规范)
                            String body = billDO.getBillName() + ":" + itemName;
                            // 生成的随机字符串
                            String nonce_str = StringUtils.getRandomStringByLength(32);
                            // 商户订单号,自定义生成唯一,判断订单表中是否已有该订单且为未支付并且未到失效时间,获取其商户订单号用于预下单,如果没有则新生成一条订单记录
                            String today = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
                            String code = PayUtil.createCode(8);
                            // 商户订单号,存入订单表--支付成功后存入账单数据库,用于支付完成后找到账单并修改状态
                            String orderNo = isvWechatInfoDO.getMchId() + today + code;
                            // 支付金额，单位：分，这边需要转成字符串类型，否则后面的签名会失败
                            Double billAmount = billDO.getBillAmount();
                            String money = getMoney(billAmount.toString());
                            // 获取本机的ip地址
                            String spbill_create_ip = IpUtils.getIpAddr(request);
                            Map<String, String> packageParams = new HashMap<String, String>();
                            // 微信分配的公众账号ID
                            packageParams.put("appid", isvWechatInfoDO.getAppIdOfficialAccount());
                            // 微信支付分配的商户号
                            packageParams.put("mch_id", isvWechatInfoDO.getMchId());
                            // 小程序APPID
                            packageParams.put("sub_appid", isvWechatInfoDO.getAppIdApplet());
                            // 子商户号,数据库中微信子商户号
                            Integer schoolId = billDO.getSchoolId();
                            SchoolDO schoolDO = schoolDOMapper.selectByPrimaryKey(schoolId);
                            // 暂空填写为杭州西开发物业管理发展有限公司余杭分公司
                            String subMchId = schoolDO.getSubMchId();
                            if (subMchId == null) {
                                throw new BusinessException(EmBusinessError.ISV_CONFIG_ERROR);
                            }
                            //                subMchId = "1521888701";
                            packageParams.put("sub_mch_id", subMchId);
                            // 随机字符串
                            packageParams.put("nonce_str", nonce_str);
                            // 商品描述
                            packageParams.put("body", body);
                            // 商户订单号
                            packageParams.put("out_trade_no", orderNo);
                            // 支付金额，这边需要转成字符串类型，否则后面的签名会失败
                            packageParams.put("total_fee", money);
                            // 本机的ip地址
                            packageParams.put("spbill_create_ip", spbill_create_ip);
                            // 接收微信支付异步通知回调地址
                            packageParams.put("notify_url", isvWechatInfoDO.getNotifyUrl());
                            // 交易类型
                            packageParams.put("trade_type", WechatPayConfig.trade_type);
                            // 用户标识
                            packageParams.put("sub_openid", openid);
                            // 除去数组中的空值和签名参数
                            packageParams = PayUtil.paraFilter(packageParams);
                            // 把数组所有元素，按照“参数=参数值”的模式用“&”字符拼接成字符串
                            String prestr = PayUtil.createLinkString(packageParams);
                            // 签名,MD5运算生成签名，这里是第一次签名，用于调用统一下单接口
                            String mySign = PayUtil.sign(prestr, isvWechatInfoDO.getMchKey(), "utf-8").toUpperCase();
                            logger.info("=======================第一次签名：" + mySign + "=====================");
                            // 拼接统一下单接口使用的xml数据，要将上一步生成的签名一起拼接进去
                            String xml = "<xml>" + "<appid>" + isvWechatInfoDO.getAppIdOfficialAccount() + "</appid>"
                                    + "<sub_appid>" + isvWechatInfoDO.getAppIdApplet() + "</sub_appid>" + "<sub_mch_id>"
                                    + subMchId + "</sub_mch_id>" + "<body><![CDATA[" + body + "]]></body>" + "<mch_id>"
                                    + isvWechatInfoDO.getMchId() + "</mch_id>" + "<nonce_str>" + nonce_str
                                    + "</nonce_str>" + "<notify_url>" + isvWechatInfoDO.getNotifyUrl() + "</notify_url>"
                                    + "<sub_openid>" + openid + "</sub_openid>" + "<out_trade_no>" + orderNo
                                    + "</out_trade_no>" + "<spbill_create_ip>" + spbill_create_ip
                                    + "</spbill_create_ip>" + "<total_fee>" + money + "</total_fee>" + "<trade_type>"
                                    + WechatPayConfig.trade_type + "</trade_type>" + "<sign>" + mySign + "</sign>"
                                    + "</xml>";
                            System.out.println("调试模式_统一下单接口 请求XML数据：" + xml);
                            // 生成新的订单记录
                            WechatOrderInfoDO newOrder = new WechatOrderInfoDO();
                            newOrder.setBillId(billId);
                            newOrder.setOpenId(openid);
                            newOrder.setOutTradeNo(orderNo);
                            newOrder.setBillAmount(billAmount);
                            wechatOrderInfoDOMapper.insertSelective(newOrder);
                            Long orderId = newOrder.getOrderId();
                            System.out.println("首次生成订单记录:orderId" + orderId);
                            // 调用统一下单接口，并接受返回的结果
                            String result = PayUtil.httpRequest(WechatPayConfig.pay_url, "POST", xml);
                            System.out.println("调试模式_统一下单接口 返回XML数据：" + result);
                            // 将解析结果存储在HashMap中
                            Map map = PayUtil.doXMLParse(result);
                            //返回状态码
                            String return_code = (String) map.get("return_code");
                            //返回给移动端需要的参数
                            Map<String, Object> response = new HashMap<String, Object>();
                            if (return_code == "SUCCESS" || return_code.equals(return_code)) {
                                // 业务结果
                                // 返回的预付单信息,该值有效期2小时
                                String prepay_id = (String) map.get("prepay_id");
                                // 将预订单id存入订单表,并且记录其过期时间,设为115分钟;
                                // 此时代表成功获取prepay_id,修改支付状态为正在支付,正在支付中的订单无法发起二次支付(如何判断?)
                                // 5分钟为,prepay_id-2小时过期预留的冗余时间,即到1小时55分钟后便将prepay_id设为过期
                                newOrder.setPrepayId(prepay_id);
                                Date expireTime = new Date(System.currentTimeMillis() + (115 * 60 * 1000));
                                newOrder.setTimeExpire(expireTime);
                                newOrder.setPayStatus(OrderConstant.WECHAT_PAY_PAYING);
                                System.out.println("成功取得prepay_id:订单内容为==" + newOrder);
                                response.put("nonceStr", nonce_str);
                                response.put("package", "prepay_id=" + prepay_id);
                                Long timeStamp = System.currentTimeMillis() / 1000;
                                // 这边要将返回的时间戳转化成字符串，不然小程序端调用wx.requestPayment方法会报签名错误
                                response.put("timeStamp", timeStamp + "");
                                // 此处为小程序AppID
                                String stringSignTemp =
                                        "appId=" + isvWechatInfoDO.getAppIdApplet() + "&nonceStr=" + nonce_str
                                                + "&package=prepay_id=" + prepay_id + "&signType="
                                                + WechatPayConfig.sign_type + "&timeStamp=" + timeStamp;
                                System.out.println("stringSignTemp:" + stringSignTemp);
                                // 再次签名，这个签名用于小程序端调用wx.requesetPayment方法
                                String paySign = PayUtil.sign(stringSignTemp, isvWechatInfoDO.getMchKey(), "utf-8")
                                        .toUpperCase();
                                logger.info("=======================第二次签名：" + paySign + "=====================");
                                response.put("paySign", paySign);
                                // 更新订单信息
                                // 业务逻辑代码-订单表
                                wechatOrderInfoDOMapper.updateByPrimaryKeySelective(newOrder);
                            }
                            response.put("appid", isvWechatInfoDO.getAppIdOfficialAccount());
                            return CommonReturnType.create(response);
                        } catch (Exception e) {
                            e.printStackTrace();
                            return CommonReturnType.create(null, "false");
                        }
                    } else if (wechatOrderInfoDOPaying != null) {
                        // 已有订单,获取当前时间-判断是否已经失效,失效则更新该条记录状态为cancel并且标记支付失败,并生成新的一条订单记录新的商户订单号,
                        System.out.println("===========================该账单正在支付中========================");
                        DateTime now = new DateTime();
                        Date date = now.toDate();
                        Date timeExpire = wechatOrderInfoDOPaying.getTimeExpire();
                        if (date.before(timeExpire)) {
                            // 未失效则继续使用该条订单记录,获取其商户订单号继续业务处理
                            System.out.println("===========================该账单正在支付中:且未失效========================");
                            try {
                                IsvWechatInfoDO isvWechatInfoDO = isvWechatInfoDOMapper.selectByIsvId(isvId);
                                if (isvWechatInfoDO == null) {
                                    throw new BusinessException(EmBusinessError.ISV_CONFIG_ERROR);
                                }
                                String itemName =
                                        billItemDOMapper.selectByPrimaryKey(billDO.getBillItemId()).getItemName();
                                // 商品描述(详看规范)
                                String body = billDO.getBillName() + ":" + itemName;
                                // 生成的随机字符串
                                String nonce_str = StringUtils.getRandomStringByLength(32);
                                // 商户订单号
                                String orderNo = wechatOrderInfoDOPaying.getOutTradeNo();
                                // 支付金额，单位：分，这边需要转成字符串类型，否则后面的签名会失败
                                Double billAmount = billDO.getBillAmount();
                                String money = getMoney(billAmount.toString());
                                // 获取本机的ip地址
                                String spbill_create_ip = IpUtils.getIpAddr(request);
                                Map<String, String> packageParams = new HashMap<String, String>();
                                // 微信分配的公众账号ID
                                packageParams.put("appid", isvWechatInfoDO.getAppIdOfficialAccount());
                                // 微信支付分配的商户号
                                packageParams.put("mch_id", isvWechatInfoDO.getMchId());
                                // 小程序APPID
                                packageParams.put("sub_appid", isvWechatInfoDO.getAppIdApplet());

                                // 子商户号,数据库中微信子商户号
                                Integer schoolId = billDO.getSchoolId();
                                SchoolDO schoolDO = schoolDOMapper.selectByPrimaryKey(schoolId);
                                // 暂空填写为杭州西开发物业管理发展有限公司余杭分公司
                                String subMchId = schoolDO.getSubMchId();
                                if (subMchId == null) {
                                    throw new BusinessException(EmBusinessError.ISV_CONFIG_ERROR);
                                }
                                //                subMchId = "1521888701";
                                packageParams.put("sub_mch_id", subMchId);
                                // 随机字符串
                                packageParams.put("nonce_str", nonce_str);
                                // 商品描述
                                packageParams.put("body", body);
                                // 商户订单号
                                packageParams.put("out_trade_no", orderNo);
                                // 支付金额，这边需要转成字符串类型，否则后面的签名会失败
                                packageParams.put("total_fee", money);
                                // 本机的ip地址
                                packageParams.put("spbill_create_ip", spbill_create_ip);
                                // 接收微信支付异步通知回调地址
                                packageParams.put("notify_url", isvWechatInfoDO.getNotifyUrl());
                                // 交易类型
                                packageParams.put("trade_type", WechatPayConfig.trade_type);
                                // 用户标识
                                packageParams.put("sub_openid", openid);
                                // 除去数组中的空值和签名参数
                                packageParams = PayUtil.paraFilter(packageParams);
                                // 把数组所有元素，按照“参数=参数值”的模式用“&”字符拼接成字符串
                                String prestr = PayUtil.createLinkString(packageParams);
                                // 签名,MD5运算生成签名，这里是第一次签名，用于调用统一下单接口
                                String mySign =
                                        PayUtil.sign(prestr, isvWechatInfoDO.getMchKey(), "utf-8").toUpperCase();
                                logger.info("=======================第一次签名：" + mySign + "=====================");
                                // 拼接统一下单接口使用的xml数据，要将上一步生成的签名一起拼接进去
                                String xml =
                                        "<xml>" + "<appid>" + isvWechatInfoDO.getAppIdOfficialAccount() + "</appid>"
                                                + "<sub_appid>" + isvWechatInfoDO.getAppIdApplet() + "</sub_appid>"
                                                + "<sub_mch_id>" + subMchId + "</sub_mch_id>" + "<body><![CDATA[" + body
                                                + "]]></body>" + "<mch_id>" + isvWechatInfoDO.getMchId() + "</mch_id>"
                                                + "<nonce_str>" + nonce_str + "</nonce_str>" + "<notify_url>"
                                                + isvWechatInfoDO.getNotifyUrl() + "</notify_url>" + "<sub_openid>"
                                                + openid + "</sub_openid>" + "<out_trade_no>" + orderNo
                                                + "</out_trade_no>" + "<spbill_create_ip>" + spbill_create_ip
                                                + "</spbill_create_ip>" + "<total_fee>" + money + "</total_fee>"
                                                + "<trade_type>" + WechatPayConfig.trade_type + "</trade_type>"
                                                + "<sign>" + mySign + "</sign>" + "</xml>";
                                System.out.println("调试模式_统一下单接口 请求XML数据：" + xml);
                                // 调用统一下单接口，并接受返回的结果
                                String result = PayUtil.httpRequest(WechatPayConfig.pay_url, "POST", xml);
                                System.out.println("调试模式_统一下单接口 返回XML数据：" + result);
                                // 将解析结果存储在HashMap中
                                Map map = PayUtil.doXMLParse(result);
                                //返回状态码
                                String return_code = (String) map.get("return_code");
                                String result_code = (String) map.get("result_code");
                                //返回给移动端需要的参数
                                Map<String, Object> response = new HashMap<String, Object>();
                                if ("SUCCESS".equals(return_code)) {
                                    if ("SUCCESS".equals(result_code)) {
                                        // 业务结果
                                        String prepay_id = (String) map.get("prepay_id");
                                        response.put("nonceStr", nonce_str);
                                        response.put("package", "prepay_id=" + prepay_id);
                                        Long timeStamp = System.currentTimeMillis() / 1000;
                                        // 这边要将返回的时间戳转化成字符串，不然小程序端调用wx.requestPayment方法会报签名错误
                                        response.put("timeStamp", timeStamp + "");
                                        // 此处为小程序AppID
                                        String stringSignTemp =
                                                "appId=" + isvWechatInfoDO.getAppIdApplet() + "&nonceStr=" + nonce_str
                                                        + "&package=prepay_id=" + prepay_id + "&signType="
                                                        + WechatPayConfig.sign_type + "&timeStamp=" + timeStamp;
                                        System.out.println("stringSignTemp:" + stringSignTemp);
                                        // 再次签名，这个签名用于小程序端调用wx.requesetPayment方法
                                        String paySign =
                                                PayUtil.sign(stringSignTemp, isvWechatInfoDO.getMchKey(), "utf-8")
                                                        .toUpperCase();
                                        logger.info(
                                                "=======================第二次签名：" + paySign + "=====================");
                                        response.put("paySign", paySign);
                                        // TODO CHECK-此处有何业务?貌似没有,投入生产前再check
                                        response.put("appid", isvWechatInfoDO.getAppIdOfficialAccount());
                                        return CommonReturnType.create(response);
                                    } else {
                                        // 发生情景:如订单已经支付
                                        String err_code = (String) map.get("err_code");
                                        String err_code_des = (String) map.get("err_code_des");
                                        System.out.println("错误码:" + err_code + ";错误信息:" + err_code_des);
                                        return CommonReturnType.create(err_code + err_code_des, "false");
                                    }
                                }

                            } catch (Exception e) {
                                e.printStackTrace();
                                return CommonReturnType.create(null, "false");
                            }
                        } else {
                            System.out.println("===========================该账单正在支付中:已超过有效时间========================");
                            // 已失效标记该条订单状态为cancel,新生成一条该账单对应的订单,重复part1
                            wechatOrderInfoDOPaying.setOrderStatus(OrderConstant.WECHAT_ORDER_CANCEL);
                            wechatOrderInfoDOPaying.setPayStatus(OrderConstant.WECHAT_PAY_FAIL);
                            int i = wechatOrderInfoDOMapper.updateByPrimaryKeySelective(wechatOrderInfoDOPaying);
                            logger.info("原订单标记为失效,支付失败,更新该订单的操作结果:i={},wechatOrderInfoDOPaying={}", i,
                                    wechatOrderInfoDOPaying);
                            // 此处返回微信错误的返回格式,用户首次点击由于参数错误不会进入支付页面,再次点击则会生成一个新订单走第一part逻辑
                            // 与前端交互,提示"请稍后重试"
                            return CommonReturnType.create("请稍后重试", "false");
                        }
                    }
                    return CommonReturnType.create(null, "false");
                } else {
                    return CommonReturnType.create(null, "false");
                }
            }
        } else if (synStatus == BILL_SYN_STATUS_UNSYN || synStatus == BILL_SYN_STATUS_SYN_FAIL) {
            // 微信下单业务
            // 从request中获取referfer
            String referer = request.getHeader("Referer");
            System.out.println("==请求头referfer:" + referer);
            // 判断订单表中是否已有该账单的记录,支付状态为0/1
            WechatOrderInfoDO wechatOrderInfoDOWait = wechatOrderInfoDOMapper
                    .selectByBillAndOpenIdExist(billId, openid, OrderConstant.WECHAT_ORDER__NEW,
                            OrderConstant.WECHAT_PAY_WAIT);
            WechatOrderInfoDO wechatOrderInfoDOPaying = wechatOrderInfoDOMapper
                    .selectByBillAndOpenIdExist(billId, openid, OrderConstant.WECHAT_ORDER__NEW,
                            OrderConstant.WECHAT_PAY_PAYING);
            logger.info("第一步:请求/pay接口--订单表:wechatOrderInfoDOWait={},wechatOrderInfoDOPaying={}", wechatOrderInfoDOWait,
                    wechatOrderInfoDOPaying);
            if (wechatOrderInfoDOWait == null && wechatOrderInfoDOPaying == null) {
                System.out.println("===========================新订单========================");
                // 新生成的订单,生成商户订单号等业务处理,最终返回给前端需要的调起支付的参数
                try {
                    IsvWechatInfoDO isvWechatInfoDO = isvWechatInfoDOMapper.selectByIsvId(isvId);
                    if (isvWechatInfoDO == null) {
                        throw new BusinessException(EmBusinessError.ISV_CONFIG_ERROR);
                    }
                    String itemName = billItemDOMapper.selectByPrimaryKey(billDO.getBillItemId()).getItemName();
                    // 商品描述(详看规范)
                    String body = billDO.getBillName() + ":" + itemName;
                    // 生成的随机字符串
                    String nonce_str = StringUtils.getRandomStringByLength(32);
                    // 商户订单号,自定义生成唯一,判断订单表中是否已有该订单且为未支付并且未到失效时间,获取其商户订单号用于预下单,如果没有则新生成一条订单记录
                    String today = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
                    String code = PayUtil.createCode(8);
                    // 商户订单号,存入订单表--支付成功后存入账单数据库,用于支付完成后找到账单并修改状态
                    String orderNo = isvWechatInfoDO.getMchId() + today + code;
                    // 支付金额，单位：分，这边需要转成字符串类型，否则后面的签名会失败
                    Double billAmount = billDO.getBillAmount();
                    String money = getMoney(billAmount.toString());
                    // 获取本机的ip地址
                    String spbill_create_ip = IpUtils.getIpAddr(request);
                    Map<String, String> packageParams = new HashMap<String, String>();
                    // 微信分配的公众账号ID
                    packageParams.put("appid", isvWechatInfoDO.getAppIdOfficialAccount());
                    // 微信支付分配的商户号
                    packageParams.put("mch_id", isvWechatInfoDO.getMchId());
                    // 小程序APPID
                    packageParams.put("sub_appid", isvWechatInfoDO.getAppIdApplet());
                    // 子商户号,数据库中微信子商户号
                    Integer schoolId = billDO.getSchoolId();
                    SchoolDO schoolDO = schoolDOMapper.selectByPrimaryKey(schoolId);
                    // 暂空填写为杭州西开发物业管理发展有限公司余杭分公司
                    String subMchId = schoolDO.getSubMchId();
                    if (subMchId == null) {
                        throw new BusinessException(EmBusinessError.ISV_CONFIG_ERROR);
                    }
                    //                subMchId = "1521888701";
                    packageParams.put("sub_mch_id", subMchId);
                    // 随机字符串
                    packageParams.put("nonce_str", nonce_str);
                    // 商品描述
                    packageParams.put("body", body);
                    // 商户订单号
                    packageParams.put("out_trade_no", orderNo);
                    // 支付金额，这边需要转成字符串类型，否则后面的签名会失败
                    packageParams.put("total_fee", money);
                    // 本机的ip地址
                    packageParams.put("spbill_create_ip", spbill_create_ip);
                    // 接收微信支付异步通知回调地址
                    packageParams.put("notify_url", isvWechatInfoDO.getNotifyUrl());
                    // 交易类型
                    packageParams.put("trade_type", WechatPayConfig.trade_type);
                    // 用户标识
                    packageParams.put("sub_openid", openid);
                    // 除去数组中的空值和签名参数
                    packageParams = PayUtil.paraFilter(packageParams);
                    // 把数组所有元素，按照“参数=参数值”的模式用“&”字符拼接成字符串
                    String prestr = PayUtil.createLinkString(packageParams);
                    // 签名,MD5运算生成签名，这里是第一次签名，用于调用统一下单接口
                    String mySign = PayUtil.sign(prestr, isvWechatInfoDO.getMchKey(), "utf-8").toUpperCase();
                    logger.info("=======================第一次签名：" + mySign + "=====================");
                    // 拼接统一下单接口使用的xml数据，要将上一步生成的签名一起拼接进去
                    String xml =
                            "<xml>" + "<appid>" + isvWechatInfoDO.getAppIdOfficialAccount() + "</appid>" + "<sub_appid>"
                                    + isvWechatInfoDO.getAppIdApplet() + "</sub_appid>" + "<sub_mch_id>" + subMchId
                                    + "</sub_mch_id>" + "<body><![CDATA[" + body + "]]></body>" + "<mch_id>"
                                    + isvWechatInfoDO.getMchId() + "</mch_id>" + "<nonce_str>" + nonce_str
                                    + "</nonce_str>" + "<notify_url>" + isvWechatInfoDO.getNotifyUrl() + "</notify_url>"
                                    + "<sub_openid>" + openid + "</sub_openid>" + "<out_trade_no>" + orderNo
                                    + "</out_trade_no>" + "<spbill_create_ip>" + spbill_create_ip
                                    + "</spbill_create_ip>" + "<total_fee>" + money + "</total_fee>" + "<trade_type>"
                                    + WechatPayConfig.trade_type + "</trade_type>" + "<sign>" + mySign + "</sign>"
                                    + "</xml>";
                    System.out.println("调试模式_统一下单接口 请求XML数据：" + xml);
                    // 生成新的订单记录
                    WechatOrderInfoDO newOrder = new WechatOrderInfoDO();
                    newOrder.setBillId(billId);
                    newOrder.setOpenId(openid);
                    newOrder.setOutTradeNo(orderNo);
                    newOrder.setBillAmount(billAmount);
                    wechatOrderInfoDOMapper.insertSelective(newOrder);
                    Long orderId = newOrder.getOrderId();
                    System.out.println("首次生成订单记录:orderId" + orderId);
                    // 调用统一下单接口，并接受返回的结果
                    String result = PayUtil.httpRequest(WechatPayConfig.pay_url, "POST", xml);
                    System.out.println("调试模式_统一下单接口 返回XML数据：" + result);
                    // 将解析结果存储在HashMap中
                    Map map = PayUtil.doXMLParse(result);
                    //返回状态码
                    String return_code = (String) map.get("return_code");
                    //返回给移动端需要的参数
                    Map<String, Object> response = new HashMap<String, Object>();
                    if (return_code == "SUCCESS" || return_code.equals(return_code)) {
                        // 业务结果
                        // 返回的预付单信息,该值有效期2小时
                        String prepay_id = (String) map.get("prepay_id");
                        // 将预订单id存入订单表,并且记录其过期时间,设为115分钟;
                        // 此时代表成功获取prepay_id,修改支付状态为正在支付,正在支付中的订单无法发起二次支付(如何判断?)
                        // 5分钟为,prepay_id-2小时过期预留的冗余时间,即到1小时55分钟后便将prepay_id设为过期
                        newOrder.setPrepayId(prepay_id);
                        Date expireTime = new Date(System.currentTimeMillis() + (115 * 60 * 1000));
                        newOrder.setTimeExpire(expireTime);
                        newOrder.setPayStatus(OrderConstant.WECHAT_PAY_PAYING);
                        System.out.println("成功取得prepay_id:订单内容为==" + newOrder);
                        response.put("nonceStr", nonce_str);
                        response.put("package", "prepay_id=" + prepay_id);
                        Long timeStamp = System.currentTimeMillis() / 1000;
                        // 这边要将返回的时间戳转化成字符串，不然小程序端调用wx.requestPayment方法会报签名错误
                        response.put("timeStamp", timeStamp + "");
                        // 此处为小程序AppID
                        String stringSignTemp = "appId=" + isvWechatInfoDO.getAppIdApplet() + "&nonceStr=" + nonce_str
                                + "&package=prepay_id=" + prepay_id + "&signType=" + WechatPayConfig.sign_type
                                + "&timeStamp=" + timeStamp;
                        System.out.println("stringSignTemp:" + stringSignTemp);
                        // 再次签名，这个签名用于小程序端调用wx.requesetPayment方法
                        String paySign =
                                PayUtil.sign(stringSignTemp, isvWechatInfoDO.getMchKey(), "utf-8").toUpperCase();
                        logger.info("=======================第二次签名：" + paySign + "=====================");
                        response.put("paySign", paySign);
                        // 更新订单信息
                        // 业务逻辑代码-订单表
                        wechatOrderInfoDOMapper.updateByPrimaryKeySelective(newOrder);
                    }
                    response.put("appid", isvWechatInfoDO.getAppIdOfficialAccount());
                    return CommonReturnType.create(response);
                } catch (Exception e) {
                    e.printStackTrace();
                    return CommonReturnType.create(null, "false");
                }
            } else if (wechatOrderInfoDOPaying != null) {
                // 已有订单,获取当前时间-判断是否已经失效,失效则更新该条记录状态为cancel并且标记支付失败,并生成新的一条订单记录新的商户订单号,
                System.out.println("===========================该账单正在支付中========================");
                DateTime now = new DateTime();
                Date date = now.toDate();
                Date timeExpire = wechatOrderInfoDOPaying.getTimeExpire();
                if (date.before(timeExpire)) {
                    // 未失效则继续使用该条订单记录,获取其商户订单号继续业务处理
                    System.out.println("===========================该账单正在支付中:且未失效========================");
                    try {
                        IsvWechatInfoDO isvWechatInfoDO = isvWechatInfoDOMapper.selectByIsvId(isvId);
                        if (isvWechatInfoDO == null) {
                            throw new BusinessException(EmBusinessError.ISV_CONFIG_ERROR);
                        }
                        String itemName = billItemDOMapper.selectByPrimaryKey(billDO.getBillItemId()).getItemName();
                        // 商品描述(详看规范)
                        String body = billDO.getBillName() + ":" + itemName;
                        // 生成的随机字符串
                        String nonce_str = StringUtils.getRandomStringByLength(32);
                        // 商户订单号
                        String orderNo = wechatOrderInfoDOPaying.getOutTradeNo();
                        // 支付金额，单位：分，这边需要转成字符串类型，否则后面的签名会失败
                        Double billAmount = billDO.getBillAmount();
                        String money = getMoney(billAmount.toString());
                        // 获取本机的ip地址
                        String spbill_create_ip = IpUtils.getIpAddr(request);
                        Map<String, String> packageParams = new HashMap<String, String>();
                        // 微信分配的公众账号ID
                        packageParams.put("appid", isvWechatInfoDO.getAppIdOfficialAccount());
                        // 微信支付分配的商户号
                        packageParams.put("mch_id", isvWechatInfoDO.getMchId());
                        // 小程序APPID
                        packageParams.put("sub_appid", isvWechatInfoDO.getAppIdApplet());

                        // 子商户号,数据库中微信子商户号
                        Integer schoolId = billDO.getSchoolId();
                        SchoolDO schoolDO = schoolDOMapper.selectByPrimaryKey(schoolId);
                        // 暂空填写为杭州西开发物业管理发展有限公司余杭分公司
                        String subMchId = schoolDO.getSubMchId();
                        if (subMchId == null) {
                            throw new BusinessException(EmBusinessError.ISV_CONFIG_ERROR);
                        }
                        //                subMchId = "1521888701";
                        packageParams.put("sub_mch_id", subMchId);
                        // 随机字符串
                        packageParams.put("nonce_str", nonce_str);
                        // 商品描述
                        packageParams.put("body", body);
                        // 商户订单号
                        packageParams.put("out_trade_no", orderNo);
                        // 支付金额，这边需要转成字符串类型，否则后面的签名会失败
                        packageParams.put("total_fee", money);
                        // 本机的ip地址
                        packageParams.put("spbill_create_ip", spbill_create_ip);
                        // 接收微信支付异步通知回调地址
                        packageParams.put("notify_url", isvWechatInfoDO.getNotifyUrl());
                        // 交易类型
                        packageParams.put("trade_type", WechatPayConfig.trade_type);
                        // 用户标识
                        packageParams.put("sub_openid", openid);
                        // 除去数组中的空值和签名参数
                        packageParams = PayUtil.paraFilter(packageParams);
                        // 把数组所有元素，按照“参数=参数值”的模式用“&”字符拼接成字符串
                        String prestr = PayUtil.createLinkString(packageParams);
                        // 签名,MD5运算生成签名，这里是第一次签名，用于调用统一下单接口
                        String mySign = PayUtil.sign(prestr, isvWechatInfoDO.getMchKey(), "utf-8").toUpperCase();
                        logger.info("=======================第一次签名：" + mySign + "=====================");
                        // 拼接统一下单接口使用的xml数据，要将上一步生成的签名一起拼接进去
                        String xml = "<xml>" + "<appid>" + isvWechatInfoDO.getAppIdOfficialAccount() + "</appid>"
                                + "<sub_appid>" + isvWechatInfoDO.getAppIdApplet() + "</sub_appid>" + "<sub_mch_id>"
                                + subMchId + "</sub_mch_id>" + "<body><![CDATA[" + body + "]]></body>" + "<mch_id>"
                                + isvWechatInfoDO.getMchId() + "</mch_id>" + "<nonce_str>" + nonce_str + "</nonce_str>"
                                + "<notify_url>" + isvWechatInfoDO.getNotifyUrl() + "</notify_url>" + "<sub_openid>"
                                + openid + "</sub_openid>" + "<out_trade_no>" + orderNo + "</out_trade_no>"
                                + "<spbill_create_ip>" + spbill_create_ip + "</spbill_create_ip>" + "<total_fee>"
                                + money + "</total_fee>" + "<trade_type>" + WechatPayConfig.trade_type + "</trade_type>"
                                + "<sign>" + mySign + "</sign>" + "</xml>";
                        System.out.println("调试模式_统一下单接口 请求XML数据：" + xml);
                        // 调用统一下单接口，并接受返回的结果
                        String result = PayUtil.httpRequest(WechatPayConfig.pay_url, "POST", xml);
                        System.out.println("调试模式_统一下单接口 返回XML数据：" + result);
                        // 将解析结果存储在HashMap中
                        Map map = PayUtil.doXMLParse(result);
                        //返回状态码
                        String return_code = (String) map.get("return_code");
                        String result_code = (String) map.get("result_code");
                        //返回给移动端需要的参数
                        Map<String, Object> response = new HashMap<String, Object>();
                        if ("SUCCESS".equals(return_code)) {
                            if ("SUCCESS".equals(result_code)) {
                                // 业务结果
                                String prepay_id = (String) map.get("prepay_id");
                                response.put("nonceStr", nonce_str);
                                response.put("package", "prepay_id=" + prepay_id);
                                Long timeStamp = System.currentTimeMillis() / 1000;
                                // 这边要将返回的时间戳转化成字符串，不然小程序端调用wx.requestPayment方法会报签名错误
                                response.put("timeStamp", timeStamp + "");
                                // 此处为小程序AppID
                                String stringSignTemp =
                                        "appId=" + isvWechatInfoDO.getAppIdApplet() + "&nonceStr=" + nonce_str
                                                + "&package=prepay_id=" + prepay_id + "&signType="
                                                + WechatPayConfig.sign_type + "&timeStamp=" + timeStamp;
                                System.out.println("stringSignTemp:" + stringSignTemp);
                                // 再次签名，这个签名用于小程序端调用wx.requesetPayment方法
                                String paySign = PayUtil.sign(stringSignTemp, isvWechatInfoDO.getMchKey(), "utf-8")
                                        .toUpperCase();
                                logger.info("=======================第二次签名：" + paySign + "=====================");
                                response.put("paySign", paySign);
                                // TODO CHECK-此处有何业务?貌似没有,投入生产前再check
                                response.put("appid", isvWechatInfoDO.getAppIdOfficialAccount());
                                return CommonReturnType.create(response);
                            } else {
                                // 发生情景:如订单已经支付
                                String err_code = (String) map.get("err_code");
                                String err_code_des = (String) map.get("err_code_des");
                                System.out.println("错误码:" + err_code + ";错误信息:" + err_code_des);
                                return CommonReturnType.create(err_code + err_code_des, "false");
                            }
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                        return CommonReturnType.create(null, "false");
                    }
                } else {
                    System.out.println("===========================该账单正在支付中:已超过有效时间========================");
                    // 已失效标记该条订单状态为cancel,新生成一条该账单对应的订单,重复part1
                    wechatOrderInfoDOPaying.setOrderStatus(OrderConstant.WECHAT_ORDER_CANCEL);
                    wechatOrderInfoDOPaying.setPayStatus(OrderConstant.WECHAT_PAY_FAIL);
                    int i = wechatOrderInfoDOMapper.updateByPrimaryKeySelective(wechatOrderInfoDOPaying);
                    logger.info("原订单标记为失效,支付失败,更新该订单的操作结果:i={},wechatOrderInfoDOPaying={}", i, wechatOrderInfoDOPaying);
                    // 此处返回微信错误的返回格式,用户首次点击由于参数错误不会进入支付页面,再次点击则会生成一个新订单走第一part逻辑
                    // 与前端交互,提示"请稍后重试"
                    return CommonReturnType.create("请稍后重试", "false");
                }
            }
            return CommonReturnType.create(null, "false");
        } else {
            throw new BusinessException(EmBusinessError.BILL_STATUS_ERROR);
        }
        return CommonReturnType.create(null, "false");
    }

    /**
     * @throws Exception 异常
     * @Description: 微信支付
     * @author hxx
     * @date 2016年12月2日
     */
    @RequestMapping(value = "/notify")
    @ResponseBody
    public void wechatNotify(HttpServletRequest request, HttpServletResponse response) throws Exception {
        BufferedReader br = new BufferedReader(new InputStreamReader((ServletInputStream) request.getInputStream()));
        String line = null;
        StringBuilder sb = new StringBuilder();
        while ((line = br.readLine()) != null) {
            sb.append(line);
        }
        br.close();
        // sb为微信返回的xml
        String notifyXml = sb.toString();
        String resXml = "";
        System.out.println("接收到的报文：" + notifyXml);
        Map map = PayUtil.doXMLParse(notifyXml);
        String returnCode = (String) map.get("return_code");
        if ("SUCCESS".equals(returnCode)) {
            System.out.println("第1次校验if条件通知");
            //此处是对除sign以外的参数进行签名再与sign比较,相同则说明是微信官方发送的通知,不相同说明通知地址被黑了
            String sign = (String) map.get("sign");
            System.out.println("报文sign:" + sign);
            map.remove("sign");
            System.out.println("除去sign报文map:" + map);
            String linkString = PayUtil.createLinkString(map);
            System.out.println("拼接key:" + linkString);
            // 获取报文中的小程序appid-->sub_appid
            String subAppId = (String) map.get("sub_appid");
            IsvWechatInfoDO isvWechatInfoDO = isvWechatInfoDOMapper.selectByAppIdApplet(subAppId);
            if (isvWechatInfoDO == null) {
                throw new BusinessException(EmBusinessError.ISV_CONFIG_ERROR);
            }
            //验证签名是否正确,根据微信官网的介绍，此处不仅对回调的参数进行验签，还需要对返回的金额与系统订单的金额进行比对等(X)
            if (PayUtil.verify(linkString, sign, isvWechatInfoDO.getMchKey(), "utf-8")) {
                // result_code SUCCESS/FAIL 业务结果
                System.out.println("第2次校验if条件验签通过");
                String resultCode = (String) map.get("result_code");
                if ("SUCCESS".equals(resultCode)) {
                    // resultCode-SUCCESS代表用户支付成功
                    System.out.println("第3次校验if条件支付成功");
                    /**此处添加自己的业务逻辑代码start**/
                    // 获取总金额total_fee单位为:分
                    String total_fee = (String) map.get("total_fee");
                    BigDecimal billAmount = new BigDecimal(total_fee);
                    // 获取微信支付订单号transaction_id
                    String transaction_id = (String) map.get("transaction_id");
                    // 商户订单号out_trade_no
                    String out_trade_no = (String) map.get("out_trade_no");
                    // 支付完成时间time_end
                    String time_end = (String) map.get("time_end");
                    String reg = "(\\d{4})(\\d{2})(\\d{2})(\\d{2})(\\d{2})(\\d{2})";
                    time_end = time_end.replaceAll(reg, "$1-$2-$3 $4:$5:$6");
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    Date payDate = sdf.parse(time_end);
                    // 记录支付账单的微信小程序用户openId
                    // map中openid为公众号下的,此处只记录小程序下用户openid
                    String openid = (String) map.get("sub_openid");
                    // 记录子商户号
                    String sub_mch_id = (String) map.get("sub_mch_id");
                    // 根据微信支付商户订单号从订单表中-获取账单id,并修改订单-账单状态
                    WechatOrderInfoDO wechatOrderInfoDO = wechatOrderInfoDOMapper.selectByOutTradeNo(out_trade_no);
                    Integer billId = wechatOrderInfoDO.getBillId();
                    BillDO billDO = billDOMapper.selectByPrimaryKey(billId);
                    if (billDO != null) {
                        System.out.println("查询数据库中账单:" + billDO);
                        wechatOrderInfoDO.setOrderStatus(OrderConstant.WECHAT_ORDER_FINISH);
                        wechatOrderInfoDO.setOpenId(openid);
                        wechatOrderInfoDO.setPayStatus(OrderConstant.WECHAT_PAY_PAID);
                        wechatOrderInfoDO.setBillAmount(billDO.getBillAmount());
                        wechatOrderInfoDO.setPayTime(payDate);
                        wechatOrderInfoDO.setSubMchId(sub_mch_id);
                        if (billAmount.equals(new BigDecimal(billDO.getBillAmount() * 100))) {
                            System.out.println("金额一致");
                            wechatOrderInfoDO.setOrderAmount(billAmount.divide(BigDecimal.valueOf(100)).doubleValue());
                            // 金额一致进入后台账单状态修改
                            billDO.setPayType(PayTypeConstant.PAY_BY_WECHAT);
                            billDO.setSerialNum(transaction_id);
                            billDO.setWechatOpenId(openid);
                            billDO.setPayDate(payDate);
                            billDO.setBillStatus(BillConstant.BILL_STATUS_PAID);
                            billDO.setOutTradeNo(out_trade_no);
                        } else {
                            logger.error("微信支付回调:订单金额与账单金额不一致");
                            System.out.println("订单金额与账单金额不一致");
                            wechatOrderInfoDO.setOrderAmount(billAmount.divide(BigDecimal.valueOf(100)).doubleValue());
                            billDO.setPayType(PayTypeConstant.PAY_ERROR);
                        }
                        wechatOrderInfoDOMapper.updateByPrimaryKeySelective(wechatOrderInfoDO);
                        billDOMapper.updateByPrimaryKeySelective(billDO);
                        // 支付业务修改成功后,同步支付宝平台将该账单(若有)关闭(且必须要是待缴费状态)
                        Byte synStatus = billDO.getSynStatus();
                        if (synStatus == BILL_SYN_STATUS_SYN) {
                            // 在订单表查询是否该笔订单存在同步成功的记录,不存在说明账单表和订单表不同步,后端获取日志处理
                            AlipayOrderInfoDO alipayOrderInfoDOExist = alipayOrderInfoDOMapper
                                    .selectByBillIdAndOrderStatus(billId, OrderConstant.ALIPAY_ORDER_SYN);
                            if (alipayOrderInfoDOExist != null) {
                                Integer isvId = billDO.getIsvId();
                                // 调用查询接口确认账单处于NOT_PAY 待缴费才能通过其他途径缴费,该订单方可被关闭,否则抛账单状态异常
                                IsvAlipayInfoDOWithBLOBs isvAlipayInfoDOWithBLOBs =
                                        isvAlipayInfoDOMapper.selectByIsvId(isvId);
                                String isvPid = isvAlipayInfoDOWithBLOBs.getIsvPid();
                                SchoolModel schoolById = schoolService.getSchoolById(billDO.getSchoolId());
                                String query = alipayService
                                        .ecoBillQuery(isvId, isvPid, schoolById.getAlipaySchoolPid(),
                                                alipayOrderInfoDOExist.getOutTradeNo());
                                if (AlipayTradeStatusConstant.QUERY_NOT_PAY.equals(query)) {
                                    // 修改订单表
                                    alipayOrderInfoDOExist.setOrderStatus(OrderConstant.ALIPAY_ORDER_CLOSED);
                                    alipayOrderInfoDOMapper.updateByPrimaryKeySelective(alipayOrderInfoDOExist);
                                    // 同步支付宝平台
                                    String outTradeNo = alipayOrderInfoDOExist.getOutTradeNo();
                                    alipayService.ecoBillModify(isvId, outTradeNo,
                                            AlipayTradeStatusConstant.MODIFY_STATUS_CLOSE);
                                } else {
                                    throw new BusinessException(EmBusinessError.BILL_STATUS_ERROR);
                                }
                            } else {
                                logger.info("修改账单同步时异常:订单表中无此账单已同步的记录,账单记录为billDO={}", billDO);
                            }
                        }

                    } else {
                        logger.error("微信支付回调:数据库中该账单为空");
                        System.out.println("微信支付回调:数据库中该账单为空");
                    }
                    /**此处添加自己的业务逻辑代码end**/
                    // 通知微信服务器已经支付成功
                    resXml = "<xml>" + "<return_code><![CDATA[SUCCESS]]></return_code>"
                            + "<return_msg><![CDATA[OK]]></return_msg>" + "</xml> ";
                }
            } else {
                System.out.println("第2次校验if条件验签不通过");
            }
        } else {
            resXml = "<xml>" + "<return_code><![CDATA[FAIL]]></return_code>"
                    + "<return_msg><![CDATA[报文为空]]></return_msg>" + "</xml> ";
        }
        System.out.println("通知微信的报文:" + resXml);
        System.out.println("微信支付回调数据结束");
        BufferedOutputStream out = new BufferedOutputStream(response.getOutputStream());
        out.write(resXml.getBytes());
        out.flush();
        out.close();
    }


    /**
     * 元转换成分
     *
     * @param amount
     * @return
     */
    public static String getMoney(String amount) {
        if (amount == null) {
            return "";
        }
        // 金额转化为分为单位
        // 处理包含, ￥ 或者$的金额
        String currency = amount.replaceAll("\\$|\\￥|\\,", "");
        int index = currency.indexOf(".");
        int length = currency.length();
        Long amLong = 0l;
        if (index == -1) {
            amLong = Long.valueOf(currency + "00");
        } else if (length - index >= 3) {
            amLong = Long.valueOf((currency.substring(0, index + 3)).replace(".", ""));
        } else if (length - index == 2) {
            amLong = Long.valueOf((currency.substring(0, index + 2)).replace(".", "") + 0);
        } else {
            amLong = Long.valueOf((currency.substring(0, index + 1)).replace(".", "") + "00");
        }
        return amLong.toString();
    }

    @RequestMapping(value = "/test/close")
    @ResponseBody
    public CommonReturnType test() {
        // 存入session到redis
        redis.set("test1", "test1", 60 * 60 * 24 * 30);
        return CommonReturnType.create(null);
    }

    /**
     * 字符串拼接获得请求头中的appid
     *
     * @param referer 请求头链接字符串
     * @return addId
     */
    private String subAppIdFromHeader(String referer) {
        //==请求头referfer:https://servicewechat.com/wx7a46bba257d417a5/2/page-frame.html
        String wechatHeader = "https://servicewechat.com/";
        String replace = referer.replace(wechatHeader, "");
        return replace.substring(0, replace.indexOf("/"));
    }

    /**
     * 根据小程序AppID获取对应服务商该小程序的信息
     *
     * @param appIdApplet 微信小程序AppID
     * @return 服务商微信配置对象
     */
    private IsvWechatInfoDO getIsvWechatInfo(String appIdApplet) {
        IsvWechatInfoDO isvWechatInfoDO = isvWechatInfoDOMapper.selectByAppIdApplet(appIdApplet);
        if (isvWechatInfoDO == null) {
            return null;
        } else {
            return isvWechatInfoDO;
        }
    }
}
