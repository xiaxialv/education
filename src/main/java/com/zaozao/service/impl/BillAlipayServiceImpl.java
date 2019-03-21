package com.zaozao.service.impl;

import com.zaozao.Constant.AlipayTradeStatusConstant;
import com.zaozao.Constant.BillConstant;
import com.zaozao.Constant.OrderConstant;
import com.zaozao.dao.AlipayOrderInfoDOMapper;
import com.zaozao.dao.BillDOMapper;
import com.zaozao.dao.IsvAlipayInfoDOMapper;
import com.zaozao.dataobject.AlipayOrderInfoDO;
import com.zaozao.dataobject.BillDO;
import com.zaozao.dataobject.IsvAlipayInfoDOWithBLOBs;
import com.zaozao.service.AlipayService;
import com.zaozao.service.BillAlipayService;
import com.zaozao.service.BillService;
import com.zaozao.service.SchoolService;
import com.zaozao.service.model.BillModel;
import com.zaozao.service.model.SchoolModel;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static com.zaozao.Constant.BillConstant.*;

/**
 * @author Sidney 2019-03-20.
 */
@Service
public class BillAlipayServiceImpl implements BillAlipayService {

    @Autowired
    private BillService billService;

    @Autowired
    private AlipayOrderInfoDOMapper alipayOrderInfoDOMapper;

    @Autowired
    private IsvAlipayInfoDOMapper isvAlipayInfoDOMapper;

    @Autowired
    private AlipayService alipayService;

    @Autowired
    private SchoolService schoolService;

    @Autowired
    private BillDOMapper billDOMapper;

    private Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
    public void updateBill(Integer id, String studentNo, String billName, Integer billItemId, BigDecimal billAmount,
            String comment, String endDate) {
        BillModel billModel = new BillModel();
        billModel.setId(id);
        billModel.setStudentNo(studentNo);
        billModel.setBillName(billName);
        billModel.setBillItemId(billItemId);
        billModel.setBillAmount(billAmount);
        DateTime now = new DateTime();
        billModel.setUpdateDate(now);
        DateTime end = new DateTime(endDate);
        //        DateTime end = DateTime.parse(endDate);
        billModel.setEndDate(end);
        billModel.setComment(comment);
        billService.updateBill(billModel);
        // TODO CHECK-后台账单修改后,判断账单状态,
        // 如果是未同步则不需要操作,
        // 如果是同步失败,需修改除了outTradeNo之外的参数-->这一步在发送账单接口时已经加了判断保留商户订单号其他均从数据库中读取
        // 如果是已同步状态需要修改订单表中的order_status为关闭,调用同步接口通知支付宝平台将该账单关闭;bill表的同步状态修改为未同步-->需要用户手动发送账单至支付宝平台
        BillDO billDO = billDOMapper.selectByPrimaryKey(id);
        Byte synStatus = billDO.getSynStatus();
        if (synStatus == BILL_SYN_STATUS_SYN) {
            // 在订单表查询是否该笔订单存在同步成功的记录,不存在说明账单表和订单表不同步,后端获取日志处理
            AlipayOrderInfoDO alipayOrderInfoDOExist =
                    alipayOrderInfoDOMapper.selectByBillIdAndOrderStatus(id, OrderConstant.ALIPAY_ORDER_SYN);
            if (alipayOrderInfoDOExist != null) {
                Integer isvId = billDO.getIsvId();
                // 调用查询接口确认要被删除的账单处于NOT_PAY 待缴费才能通过其他途径缴费,该订单方可被关闭,否则抛账单状态异常
                IsvAlipayInfoDOWithBLOBs isvAlipayInfoDOWithBLOBs = isvAlipayInfoDOMapper.selectByIsvId(isvId);
                String isvPid = isvAlipayInfoDOWithBLOBs.getIsvPid();
                SchoolModel schoolById = schoolService.getSchoolById(billDO.getSchoolId());
                String query = alipayService.ecoBillQuery(isvId, isvPid, schoolById.getAlipaySchoolPid(),
                        alipayOrderInfoDOExist.getOutTradeNo());
                if (AlipayTradeStatusConstant.QUERY_NOT_PAY.equals(query)) {
                    // 修改订单表
                    alipayOrderInfoDOExist.setOrderStatus(OrderConstant.ALIPAY_ORDER_CLOSED);
                    alipayOrderInfoDOMapper.updateByPrimaryKeySelective(alipayOrderInfoDOExist);
                    // 修改账单表
                    billDO.setSynStatus(BILL_SYN_STATUS_UNSYN);
                    billDOMapper.updateByPrimaryKeySelective(billDO);
                    // 同步支付宝平台
                    // 支付宝平台缴费业务流程:1.后台发送账单到平台;2.平台展示,用户支付;3.支付成功异步通知后台
                    String outTradeNo = alipayOrderInfoDOExist.getOutTradeNo();
                    alipayService.ecoBillModify(isvId, outTradeNo, AlipayTradeStatusConstant.MODIFY_STATUS_CLOSE);
                }
            } else {
                logger.info("修改账单同步时异常:订单表中无此账单已同步的记录,账单记录为billDO={}", billDO);
            }
        }

    }

    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
    public void delete(Integer id) {
        //TODO CHECK-删除-判断账单状态,支付状态是否需要判断?未完;一键升级的历史账单如何处理(区分是否同步到支付宝且为未支付,将其关闭?)
        //如果是未同步,则修改bill表的同步状态修改为无须同步,软删除
        //如果是同步失败(必然在支付宝平台未支付,可能被其他渠道支付),修改订单表中的order_status为关闭,bill表的同步状态修改为无须支付,软删除
        //如果是已同步状态,调用查询接口确认要被删除的账单处于NOT_PAY,修改订单表中的order_status为关闭;调用同步接口通知支付宝平台将该账单关闭,bill表的同步状态修改为无须同步
        //如果是已支付状态,支付宝平台无须操作,软删除
        BillDO billDO = billDOMapper.selectByPrimaryKey(id);
        Byte billStatus = billDO.getBillStatus();
        Byte synStatus = billDO.getSynStatus();
        if (synStatus == BILL_SYN_STATUS_UNSYN) {
            // 修改账单表
            billDO.setSynStatus(BILL_SYN_STATUS_SYN_NEEDLESS);
            billDOMapper.updateByPrimaryKeySelective(billDO);
            billService.softDeleteBill(id);
        } else if (synStatus == BILL_SYN_STATUS_SYN_FAIL) {
            // 在订单表查询是否该笔订单存在同步失败的记录,不存在说明账单表和订单表不同步,后端获取日志处理
            AlipayOrderInfoDO alipayOrderInfoDOExist =
                    alipayOrderInfoDOMapper.selectByBillIdAndOrderStatus(id, OrderConstant.ALIPAY_ORDER_SYN_FAIL);
            if (alipayOrderInfoDOExist != null) {
                // 修改订单表
                alipayOrderInfoDOExist.setOrderStatus(OrderConstant.ALIPAY_ORDER_CLOSED);
                alipayOrderInfoDOMapper.updateByPrimaryKeySelective(alipayOrderInfoDOExist);
                // 修改账单表
                billDO.setSynStatus(BILL_SYN_STATUS_SYN_NEEDLESS);
                billDOMapper.updateByPrimaryKeySelective(billDO);
                // 软删除
                billService.softDeleteBill(id);
            } else {
                logger.info("修改账单同步时异常:订单表中无此账单已同步的记录,账单记录为billDO={}", billDO);
            }
        } else if (synStatus == BILL_SYN_STATUS_SYN) {
            // 在订单表查询是否该笔订单存在同步成功的记录,不存在说明账单表和订单表不同步,后端获取日志处理
            AlipayOrderInfoDO alipayOrderInfoDOExist =
                    alipayOrderInfoDOMapper.selectByBillIdAndOrderStatus(id, OrderConstant.ALIPAY_ORDER_SYN);
            if (alipayOrderInfoDOExist != null) {
                Integer isvId = billDO.getIsvId();
                // 调用查询接口确认要被删除的账单处于NOT_PAY 待缴费才能被关闭,否则抛账单状态异常
                IsvAlipayInfoDOWithBLOBs isvAlipayInfoDOWithBLOBs = isvAlipayInfoDOMapper.selectByIsvId(isvId);
                String isvPid = isvAlipayInfoDOWithBLOBs.getIsvPid();
                SchoolModel schoolById = schoolService.getSchoolById(billDO.getSchoolId());
                String query = alipayService.ecoBillQuery(isvId, isvPid, schoolById.getAlipaySchoolPid(),
                        alipayOrderInfoDOExist.getOutTradeNo());
                if (AlipayTradeStatusConstant.QUERY_NOT_PAY.equals(query)) {
                    // 修改订单表
                    alipayOrderInfoDOExist.setOrderStatus(OrderConstant.ALIPAY_ORDER_CLOSED);
                    alipayOrderInfoDOMapper.updateByPrimaryKeySelective(alipayOrderInfoDOExist);
                    // 修改账单表
                    billDO.setSynStatus(BILL_SYN_STATUS_SYN_NEEDLESS);
                    billDOMapper.updateByPrimaryKeySelective(billDO);
                    // 同步支付宝平台
                    String outTradeNo = alipayOrderInfoDOExist.getOutTradeNo();
                    alipayService.ecoBillModify(isvId, outTradeNo, AlipayTradeStatusConstant.MODIFY_STATUS_CLOSE);
                    // 软删除
                    billService.softDeleteBill(id);
                }
            } else {
                logger.info("修改账单同步时异常:订单表中无此账单已同步的记录,账单记录为billDO={}", billDO);
            }
        } else if (billStatus == BILL_STATUS_PAID) {
            billService.softDeleteBill(id);
        }

    }

    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
    public void clickCollection(String ids, Byte payType, String comment, String billNum) {
        BillModel billModel = new BillModel();
        List<Integer> listId = this.splitString(ids);
        for (Integer id : listId) {
            // 添加校验,已支付的账单是不允许再一键缴费的
            // 使用for循环对一批账单进行线下缴费处理,遇到已缴费的账单直接continue出去,
            BillDO billDOBefore = billDOMapper.selectByPrimaryKey(id);
            Byte billStatus = billDOBefore.getBillStatus();
            if (BillConstant.BILL_STATUS_PAID == billStatus) {
                // TODO 对于批量操作中某一条记录存在异常该如何返回前端提示?
                continue;
            } else {
                billModel.setId(id);
                billModel.setBillStatus(BillConstant.BILL_STATUS_PAID);
                billModel.setPayType(payType);
                DateTime now = new DateTime();
                billModel.setPayDate(now);
                billModel.setComment(comment);
                billModel.setBillNum(billNum);
                billService.updateBill(billModel);
                // 确保修改操作成功
                // TODO CHECK-一键缴费,判断账单状态,
                // 如果是未同步则,修改bill表的同步状态修改为无须同步:删除
                // 如果是同步失败,需修改修改订单表中的order_status为关闭,bill表的同步状态修改为无须支付:删除;无须同步支付宝
                // 如果是已同步状态需要修改订单表中的order_status为关闭,调用查询接口确认要被删除的账单处于NOT_PAY 待缴费才能通过其他途径缴费,该订单方可被关闭调用同步接口通知支付宝平台将该账单关闭(必须是待缴费状态的);bill表的同步状态修改为无须同步:删除
                Byte synStatus = billDOBefore.getSynStatus();
                if (synStatus == BILL_SYN_STATUS_UNSYN) {
                    // 修改账单表
                    BillDO billDO1 = new BillDO();
                    billDO1.setId(id);
                    billDO1.setSynStatus(BILL_SYN_STATUS_SYN_NEEDLESS);
                    billDOMapper.updateByPrimaryKeySelective(billDO1);
                } else if (synStatus == BILL_SYN_STATUS_SYN_FAIL) {
                    // 在订单表查询是否该笔订单存在同步成功的记录,不存在说明账单表和订单表不同步,后端获取日志处理
                    AlipayOrderInfoDO alipayOrderInfoDOExist = alipayOrderInfoDOMapper
                            .selectByBillIdAndOrderStatus(id, OrderConstant.ALIPAY_ORDER_SYN_FAIL);
                    if (alipayOrderInfoDOExist != null) {
                        // 修改订单表
                        alipayOrderInfoDOExist.setOrderStatus(OrderConstant.ALIPAY_ORDER_CLOSED);
                        alipayOrderInfoDOMapper.updateByPrimaryKeySelective(alipayOrderInfoDOExist);
                        // 修改账单表
                        BillDO billDO1 = new BillDO();
                        billDO1.setId(id);
                        billDO1.setSynStatus(BILL_SYN_STATUS_SYN_NEEDLESS);
                        billDOMapper.updateByPrimaryKeySelective(billDO1);
                    } else {
                        logger.info("修改账单同步时异常:订单表中无此账单已同步的记录,账单记录为billDO={}", billDOBefore);
                    }
                } else if (synStatus == BILL_SYN_STATUS_SYN) {
                    // 在订单表查询是否该笔订单存在同步成功的记录,不存在说明账单表和订单表不同步,后端获取日志处理
                    AlipayOrderInfoDO alipayOrderInfoDOExist =
                            alipayOrderInfoDOMapper.selectByBillIdAndOrderStatus(id, OrderConstant.ALIPAY_ORDER_SYN);
                    if (alipayOrderInfoDOExist != null) {
                        Integer isvId = billDOBefore.getIsvId();
                        // 调用查询接口确认要被删除的账单处于NOT_PAY 待缴费才能通过其他途径缴费,该订单方可被关闭,否则抛账单状态异常
                        IsvAlipayInfoDOWithBLOBs isvAlipayInfoDOWithBLOBs = isvAlipayInfoDOMapper.selectByIsvId(isvId);
                        String isvPid = isvAlipayInfoDOWithBLOBs.getIsvPid();
                        SchoolModel schoolById = schoolService.getSchoolById(billDOBefore.getSchoolId());
                        String query = alipayService.ecoBillQuery(isvId, isvPid, schoolById.getAlipaySchoolPid(),
                                alipayOrderInfoDOExist.getOutTradeNo());
                        if (AlipayTradeStatusConstant.QUERY_NOT_PAY.equals(query)) {
                            // 修改订单表
                            alipayOrderInfoDOExist.setOrderStatus(OrderConstant.ALIPAY_ORDER_CLOSED);
                            alipayOrderInfoDOMapper.updateByPrimaryKeySelective(alipayOrderInfoDOExist);
                            // 修改账单表
                            BillDO billDO1 = new BillDO();
                            billDO1.setId(id);
                            billDO1.setSynStatus(BILL_SYN_STATUS_SYN_NEEDLESS);
                            billDOMapper.updateByPrimaryKeySelective(billDO1);
                            // 同步支付宝平台
                            String outTradeNo = alipayOrderInfoDOExist.getOutTradeNo();
                            alipayService
                                    .ecoBillModify(isvId, outTradeNo, AlipayTradeStatusConstant.MODIFY_STATUS_CLOSE);
                        }
                        //                            else {
                        //                                throw new BusinessException(EmBusinessError.BILL_STATUS_ERROR);
                        //                            }
                    } else {
                        logger.info("修改账单同步时异常:订单表中无此账单已同步的记录,账单记录为billDO={}", billDOBefore);
                    }
                }

            }

        }
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
