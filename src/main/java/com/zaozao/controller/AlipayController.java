package com.zaozao.controller;

import com.alibaba.fastjson.JSON;
import com.alipay.api.AlipayApiException;
import com.alipay.api.internal.util.AlipaySignature;
import com.zaozao.Constant.*;
import com.zaozao.controller.viewobject.AdminVO;
import com.zaozao.dao.AlipayOrderInfoDOMapper;
import com.zaozao.dao.BillDOMapper;
import com.zaozao.dao.IsvAlipayInfoDOMapper;
import com.zaozao.dataobject.AlipayOrderInfoDO;
import com.zaozao.dataobject.BillDO;
import com.zaozao.dataobject.IsvAlipayInfoDOWithBLOBs;
import com.zaozao.error.BusinessException;
import com.zaozao.error.EmBusinessError;
import com.zaozao.response.CommonReturnType;
import com.zaozao.service.AlipayService;
import com.zaozao.service.model.AlipayNotifyParamModel;
import com.zaozao.util.AlipayConfig;
import com.zaozao.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;



/**
 * @author Sidney 2019-02-16.
 */
@Controller("alipayController")
@RequestMapping("/alipay")
@CrossOrigin(allowCredentials = "true", origins = "*")
public class AlipayController extends BaseController {

    @Autowired
    private HttpServletRequest httpServletRequest;

    @Autowired
    private AlipayService alipayService;

    @Autowired
    private IsvAlipayInfoDOMapper isvAlipayInfoDOMapper;

    @Autowired
    private AlipayOrderInfoDOMapper alipayOrderInfoDOMapper;

    @Autowired
    private BillDOMapper billDOMapper;

    private Logger logger = LoggerFactory.getLogger(getClass());

    private ExecutorService executorService =
            new ThreadPoolExecutor(20, 100, 0, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(100), r -> {
                Thread thread = new Thread(r);
                thread.setName("alipay executor");
                return thread;
            });

    /**
     * 学校商户使用此应用授权链接在PC端/手机端进行应用授权
     * 学校管理员才可以去打开链接进行扫码授权;该账号需要与isv应用AppID签约.
     * 返回前端url,新窗口打开
     * 授权链接中配置的redirect_uri内容需要与应用中配置的授权回调地址完全一样，否则无法正常授权。
     * @return 应用授权URL, 示例如下:
     * https://openauth.alipay.com/oauth2/appToAppAuth.htm?app_id=2019030663478347&redirect_uri=https%3A%2F%2Fhz.zaozaojiaoyu.com%2Falipay%2Fauth%2Fnotify%3FschoolId%3D1
     */
    @RequestMapping(value = "/auth/school", method = {RequestMethod.GET})
    @ResponseBody
    public CommonReturnType authorizeBySchool() throws BusinessException {
        Boolean isLogin = (Boolean) httpServletRequest.getSession().getAttribute("IS_LOGIN");
        AdminVO loginUser =
                JSON.parseObject((String) httpServletRequest.getSession().getAttribute("LOGIN_USER"), AdminVO.class);
        if (isLogin == null || !isLogin) {
            throw new BusinessException(EmBusinessError.USER_NOT_LOGIN);
        } else if (loginUser.getAccountRole() != AccountRoleConstant.ACCOUNT_ROLE_SCHOOL) {
            throw new BusinessException(EmBusinessError.ACCESS_VIOLATION_ERROR);
        } else {
            List<Integer> schoolIdList = loginUser.getSchoolId();
            if (CollectionUtils.isEmpty(schoolIdList)) {
                throw new BusinessException(EmBusinessError.SCHOOL_INFO_ERROR);
            }
            // 获得session中的schoolId
            Integer schoolId = schoolIdList.get(0);
            String authUri = alipayService.authorizeBySchool(schoolId);
            return CommonReturnType.create(authUri);
        }
    }

    /**
     * 支付宝授权回调接口,用于接收app_auth_code,学校id传入,便于知道是哪个商户授权
     * 使用app_auth_code换取app_auth_token
     *
     * @param app_id        开发者应用的appId
     * @param app_auth_code 当次授权的授权码
     * @return 商户相关信息
     * @throws Exception 异常
     */
    @RequestMapping(value = "/auth/notify", method = {RequestMethod.GET})
    @ResponseBody
    public CommonReturnType getAuthCode(@RequestParam("schoolId") Integer schoolId,
            @RequestParam("app_id") String app_id, @RequestParam("app_auth_code") String app_auth_code) {
        // 传入schoolId
        String result = alipayService.authCodeToToken(schoolId, app_id, app_auth_code);
        return CommonReturnType.create(null, result);
    }

    /**
     * 发送学校信息,允许isv服务商发送其下属学校的信息
     * @return 学校在支付宝的唯一编号
     */
    @RequestMapping(value = "/eco/schoolInfo", method = {RequestMethod.GET})
    @ResponseBody
    public CommonReturnType ecoSchoolInfo(@RequestParam("schoolId") Integer schoolId) throws BusinessException {
        Boolean isLogin = (Boolean) httpServletRequest.getSession().getAttribute("IS_LOGIN");
        AdminVO loginUser =
                JSON.parseObject((String) httpServletRequest.getSession().getAttribute("LOGIN_USER"), AdminVO.class);
        if (isLogin == null || !isLogin) {
            throw new BusinessException(EmBusinessError.USER_NOT_LOGIN);
        } else if (AccountRoleConstant.ACCOUNT_ROLE_ISV == loginUser.getAccountRole()) {
            Integer isvId = loginUser.getIsvId();
            List<Integer> schoolIdList = loginUser.getSchoolId();
            if (isvId == null) {
                throw new BusinessException(EmBusinessError.ISV_INFO_ERROR);
            } else if (CollectionUtils.isEmpty(schoolIdList)) {
                throw new BusinessException(EmBusinessError.SCHOOL_INFO_ERROR);
            } else if (!schoolIdList.contains(schoolId)) {
                throw new BusinessException(EmBusinessError.ACCESS_VIOLATION_ERROR);
            } else {
                String result = alipayService.ecoSchoolInfo(schoolId, isvId);
                return CommonReturnType.create(null, result);
            }
        } else {
            throw new BusinessException(EmBusinessError.ACCESS_VIOLATION_ERROR);
        }
    }

    /**
     * 同步账单到支付宝,多选;学校管理员及其下属学校财务账号可以发送账单到支付宝
     * @param ids       账单ids
     * @param schoolIds 学校ids
     * @return 通用类
     * @throws Exception Business异常
     */
    @RequestMapping(value = "/eco/bill/send", method = {RequestMethod.POST}, consumes = {CONTENT_TYPE_FORMED})
    @ResponseBody
    public CommonReturnType ecoBillSend(@RequestParam("ids") String ids, @RequestParam("schoolIds") String schoolIds)
            throws Exception {
        Boolean isLogin = (Boolean) httpServletRequest.getSession().getAttribute("IS_LOGIN");
        AdminVO loginUser =
                JSON.parseObject((String) httpServletRequest.getSession().getAttribute("LOGIN_USER"), AdminVO.class);
        if (isLogin == null || !isLogin) {
            throw new BusinessException(EmBusinessError.USER_NOT_LOGIN);
        }
        Byte accountRole = loginUser.getAccountRole();
        if (AccountRoleConstant.ACCOUNT_ROLE_SCHOOL == accountRole
                || AccountRoleConstant.ACCOUNT_ROLE_SCHOOL_FINACIAL == accountRole) {
            List<Integer> schoolIdList = loginUser.getSchoolId();
            // 将账单对应的学校ids进行转化并去重
            List<Integer> listSchool = this.splitString(schoolIds);
            List list = this.removeDuplicate(listSchool);
            if (CollectionUtils.isEmpty(schoolIdList)) {
                throw new BusinessException(EmBusinessError.SCHOOL_INFO_ERROR);
            } else if (list.size() > 1) {
                throw new BusinessException(EmBusinessError.ACCESS_VIOLATION_ERROR);
            } else {
                List<Integer> listId = this.splitString(ids);
                alipayService.ecoBillSend(listId);
            }
        } else {
            throw new BusinessException(EmBusinessError.ACCESS_VIOLATION_ERROR);
        }
        return CommonReturnType.create(null);
    }

    /**
     * <pre>
     * 第一步:验证签名,签名通过后进行第二步
     * 第二步:按一下步骤进行验证
     * 1、商户需要验证该通知数据中的out_trade_no是否为商户系统中创建的订单号，
     * 2、判断total_amount是否确实为该订单的实际金额（即商户订单创建时的金额），
     * 3、校验通知中的seller_id（或者seller_email) 是否为out_trade_no这笔单据的对应的操作方（有的时候，一个商户可能有多个seller_id/seller_email），
     * 4、验证app_id是否为该商户本身。上述1、2、3、4有任何一个验证不通过，则表明本次通知是异常通知，务必忽略。
     * 在上述验证通过后商户必须根据支付宝不同类型的业务通知，正确的进行不同的业务处理，并且过滤重复的通知结果数据。
     * 在支付宝的业务通知中，只有交易通知状态为TRADE_SUCCESS或TRADE_FINISHED时，支付宝才会认定为买家付款成功。
     * </pre>
     * 支付宝支付成功结果回调接口
     * 通知触发条件:交易支付成功;未付款交易超时关闭，或支付完成后全额退款
     * 状态TRADE_SUCCESS的通知触发条件是商户签约的产品支持退款功能的前提下，买家付款成功；
     * 交易状态TRADE_FINISHED的通知触发条件是商户签约的产品不支持退款功能的前提下，买家付款成功；或者，商户签约的产品支持退款功能的前提下，交易已经成功并且已经超过可退款期限。
     * 同一条异步通知服务器异步通知参数notify_id是不变的。
     */
    @RequestMapping(value = "/pay/notify", method = {RequestMethod.POST})
    @ResponseBody
    public String getAlipayResult(HttpServletRequest request) throws AlipayApiException {
        // 1-1返回开发者的app_id;支付宝交易号;商户订单号(passback_params中需要解码)-->验签
        // 1-2校验通过后修改该账单在数据库的状态
        // 1-3程序执行完后必须打印输出success
        // 2-1调用同步接口修改账单在支付宝平台的状态

        // 将异步通知中收到的待验证所有参数都存放到map中
        Map<String, String> params = convertRequestParamsToMap(request);
        String paramsJson = JSON.toJSONString(params);
        logger.info("支付宝回调，{}", paramsJson);
        // 自定义:验证报文中的app_id是否在数据库中有记录,获取相关支付宝配置
        String app_id = params.get("app_id");
        if (!StringUtils.isEmpty(app_id)) {
            IsvAlipayInfoDOWithBLOBs isvAlipayInfoDOWithBLOBs = isvAlipayInfoDOMapper.selectByAppIdApplet(app_id);
            if (isvAlipayInfoDOWithBLOBs == null) {
                // 说明app_id不正确,此处直接抛错--支付宝已未收到success会持续25h发送
                throw new AlipayApiException("app_id不一致");
            } else {
                try {
                    // 调用SDK验证签名
                    boolean signVerified = AlipaySignature
                            .rsaCheckV1(params, isvAlipayInfoDOWithBLOBs.getAlipayPublicKey(), AlipayConfig.CHARSET,
                                    AlipayConfig.SIGN_TYPE);
                    if (signVerified) {
                        logger.info("支付宝回调签名认证成功");
                        // 按照支付结果异步通知中的描述，对支付结果中的业务内容进行1\2\3\4二次校验，校验成功后在response中返回success，校验失败返回failure
                        this.check(params);
                        // 另起线程处理业务,业务处理成功-->同步接口修改支付平台账单状态为缴费成功将返回业务状态存入数据库
                        executorService.execute(new Runnable() {
                            @Override
                            public void run() {
                                // 将报文参数封装到AlipayNotifyParamModel对象中
                                AlipayNotifyParamModel param = buildAlipayNotifyParam(params);
                                String trade_status = param.getTradeStatus();
                                String buyerId = param.getBuyerId();
                                String sellerId = param.getSellerId();
                                BigDecimal totalAmount = param.getTotalAmount();
                                Date gmtPayment = param.getGmtPayment();
                                String tradeNo = param.getTradeNo();
                                // 支付成功
                                if (trade_status.equals(AlipayTradeStatusConstant.TRADE_SUCCESS) || trade_status
                                        .equals(AlipayTradeStatusConstant.TRADE_FINISHED)) {
                                    // 处理支付成功逻辑
                                    try {
                                        // 处理业务逻辑。。。先从passbackParams中获取orderNo和isvOrderNo的值:base64解码;参数格式为URL参数（orderNo=&isvOrderNo=）
                                        String passbackParams = params.get("passback_params");
                                        Map<String, String> map = getByBase64(passbackParams);
                                        String isvOrderNo = map.get("isvOrderNo");
                                        // 没有被引用
                                        String orderNo = map.get("orderNo");
                                        // 1、商户需要验证该通知数据中的out_trade_no是否为商户系统中创建的订单号，
                                        AlipayOrderInfoDO alipayOrderInfoDO =
                                                alipayOrderInfoDOMapper.selectByOutTradeNo(isvOrderNo);
                                        if (alipayOrderInfoDO == null) {
                                            logger.error("out_trade_no错误");
                                        } else {
                                            alipayOrderInfoDO.setTradeNo(tradeNo);
                                            alipayOrderInfoDO.setBuyerId(buyerId);
                                            alipayOrderInfoDO.setSellerId(sellerId);
                                            alipayOrderInfoDO.setOrderAmount(totalAmount.doubleValue());
                                            alipayOrderInfoDO.setPayDate(gmtPayment);
                                            alipayOrderInfoDO.setOrderStatus(OrderConstant.ALIPAY_ORDER_PAYED);
                                            int i = alipayOrderInfoDOMapper
                                                    .updateByPrimaryKeySelective(alipayOrderInfoDO);
                                            if (i != 0) {
                                                Integer billId = alipayOrderInfoDO.getBillId();
                                                BillDO billDOExist = billDOMapper.selectByPrimaryKey(billId);
                                                billDOExist.setId(billId);
                                                billDOExist.setOutTradeNo(isvOrderNo);
                                                billDOExist.setPayDate(gmtPayment);
                                                billDOExist.setSerialNum(tradeNo);
                                                billDOExist.setPayType(PayTypeConstant.PAY_BY_ALIPAY);
                                                billDOExist.setBillStatus(BillConstant.BILL_STATUS_PAID);
                                                billDOMapper.updateByPrimaryKeySelective(billDOExist);
                                                // 同步平台的账单状态为缴费成功
                                                Integer isvId = billDOExist.getIsvId();
                                                alipayService.ecoBillModify(isvId, isvOrderNo, tradeNo,
                                                        AlipayTradeStatusConstant.MODIFY_STATUS_PAYED);
                                            }
                                        }
                                    } catch (Exception e) {
                                        logger.error("支付宝回调业务处理报错,params:" + paramsJson, e);
                                    }
                                } else {
                                    logger.error("没有处理支付宝回调业务，支付宝交易状态：{},params:{}", trade_status, paramsJson);
                                }
                            }
                        });
                        // 如果签名验证正确，立即返回success，后续业务另起线程单独处理
                        // 业务处理失败，可查看日志进行补偿，跟支付宝已经没多大关系。
                        return "success";
                    } else {
                        logger.info("支付宝回调签名认证失败，signVerified=false, paramsJson:{}", paramsJson);
                        return "failure";
                    }
                } catch (AlipayApiException e) {
                    logger.error("支付宝回调签名认证失败,paramsJson:{},errorMsg:{}", paramsJson, e.getMessage());
                    return "failure";
                }

            }
        }
        return "failure";
    }

    // 将request中的参数转换成Map
    private static Map<String, String> convertRequestParamsToMap(HttpServletRequest request) {
        Map<String, String> retMap = new HashMap<String, String>();

        Set<Map.Entry<String, String[]>> entrySet = request.getParameterMap().entrySet();

        for (Map.Entry<String, String[]> entry : entrySet) {
            String name = entry.getKey();
            String[] values = entry.getValue();
            int valLen = values.length;

            if (valLen == 1) {
                retMap.put(name, values[0]);
            } else if (valLen > 1) {
                StringBuilder sb = new StringBuilder();
                for (String val : values) {
                    sb.append(",").append(val);
                }
                retMap.put(name, sb.toString().substring(1));
            } else {
                retMap.put(name, "");
            }
        }

        return retMap;
    }

    private AlipayNotifyParamModel buildAlipayNotifyParam(Map<String, String> params) {
        String json = JSON.toJSONString(params);
        return JSON.parseObject(json, AlipayNotifyParamModel.class);
    }


    /**
     * 1、商户需要验证该通知数据中的out_trade_no是否为商户系统中创建的订单号，
     * 2、判断total_amount是否确实为该订单的实际金额（即商户订单创建时的金额），
     * 3、校验通知中的seller_id（或者seller_email)是否为out_trade_no这笔单据的对应的操作方（有的时候，一个商户可能有多个seller_id/seller_email），
     * 4、验证app_id是否为该商户本身。上述1、2、3、4有任何一个验证不通过，则表明本次通知是异常通知，务必忽略。
     * 在上述验证通过后商户必须根据支付宝不同类型的业务通知，正确的进行不同的业务处理，并且过滤重复的通知结果数据。
     * 在支付宝的业务通知中，只有交易通知状态为TRADE_SUCCESS或TRADE_FINISHED时，支付宝才会认定为买家付款成功。
     *
     * @param params 通知报文
     * @throws AlipayApiException 支付异常
     */
    private void check(Map<String, String> params) throws AlipayApiException {
        // 使用passback_params解码后获得的outTradeNo
        //        String outTradeNo = params.get("out_trade_no");
        String passbackParams = params.get("passback_params");
        Map<String, String> map = this.getByBase64(passbackParams);
        String isvOrderNo = map.get("isvOrderNo");
        // 1、商户需要验证该通知数据中的out_trade_no是否为商户系统中创建的订单号，
        AlipayOrderInfoDO alipayOrderInfoDO = alipayOrderInfoDOMapper.selectByOutTradeNo(isvOrderNo);
        if (alipayOrderInfoDO == null) {
            throw new AlipayApiException("out_trade_no错误");
        }
        // 2、判断total_amount是否确实为该订单的实际金额（即商户订单创建时的金额），
        // TODO CHECK-精度缺失,只校验保留2位小数的值,如需精度再调整
        long total_amount = new BigDecimal(params.get("total_amount")).multiply(new BigDecimal(100)).longValue();
        if (total_amount != new BigDecimal(alipayOrderInfoDO.getBillAmount()).multiply(new BigDecimal(100))
                .longValue()) {
            throw new AlipayApiException("total_amount错误");
        }
        // 3、校验通知中的seller_id（或者seller_email)是否为out_trade_no这笔单据的对应的操作方（有的时候，一个商户可能有多个seller_id/seller_email），
        // 第三步可根据实际情况省略
        // 4、验证app_id是否为该商户本身。
        //        if (!params.get("app_id").equals(alipayConfig.getAppid())) {
        //            throw new AlipayApiException("app_id不一致");
        //        }
    }

    // 对passback_params参数解码
    private Map<String, String> getByBase64(String passbackParams) {
        Map<String, String> params = new HashMap<>();
        Base64.Decoder decoder = Base64.getDecoder();
        try {
            byte[] textByte = passbackParams.getBytes("GBK");
            String result = new String(decoder.decode(textByte), "GBK");
            // 字符串分割?
            String[] arrSplit = null;
            if (result == null) {
                return params;
            }
            arrSplit = result.split("[&]");
            for (String strSplit : arrSplit) {
                String[] arrSplitEqual = null;
                arrSplitEqual = strSplit.split("[=]");
                //解析出键值
                if (arrSplitEqual.length > 1) {
                    //正确解析
                    params.put(arrSplitEqual[0], arrSplitEqual[1]);
                } else {
                    if (arrSplitEqual[0] != "") {
                        //只有参数没有值，不加入
                        params.put(arrSplitEqual[0], "");
                    }
                }
            }
            return params;
        } catch (UnsupportedEncodingException e) {
            logger.error("base64解码异常:", e);
        }
        return params;
    }


    // List集合去重
    private List removeDuplicate(List list) {
        List listTemp = new ArrayList();
        for (int i = 0; i < list.size(); i++) {
            if (!listTemp.contains(list.get(i))) {
                listTemp.add(list.get(i));
            }
        }
        return listTemp;
    }

    // 将以逗号分隔的字符串转换成List
    private List<Integer> splitString(String string) {
        // 接收包含账单id的字符串，并将它分割成字符串数组
        String[] idString = string.split(",");
        // 将字符串数组转为List<Integer> 类型
        List<Integer> list = new ArrayList<>();
        for (String str : idString) {
            list.add(new Integer(str));
        }
        return list;
    }

}
