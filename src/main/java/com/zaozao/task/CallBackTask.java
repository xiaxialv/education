package com.zaozao.task;

import com.zaozao.Constant.AlipayTradeStatusConstant;
import com.zaozao.Constant.OrderConstant;
import com.zaozao.dao.AlipayOrderInfoDOMapper;
import com.zaozao.dao.BillDOMapper;
import com.zaozao.dataobject.AlipayOrderInfoDO;
import com.zaozao.dataobject.BillDO;
import com.zaozao.service.AlipayService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Sidney 2019-03-15.
 */
@EnableScheduling
@Component
public class CallBackTask {

    @Autowired
    private AlipayOrderInfoDOMapper alipayOrderInfoDOMapper;

    @Autowired
    private AlipayService alipayService;

    @Autowired
    private BillDOMapper billDOMapper;

    private Logger logger = LoggerFactory.getLogger(getClass());

    //在每天6点到6:59期间的每5分钟触发
    @Scheduled(cron = "0 0/5 6 * * ?")
    public void sendPaid(){
        //定时任务业务,对支付宝订单表中order_status为已支付已关闭;modify_status为失败的记录执行定时任务
        //1.order_status为已支付-同步失败
        logger.info("===========开始同步(已支付)========");
        List<AlipayOrderInfoDO> orderPaidWait = alipayOrderInfoDOMapper
                .selectByOrderStatusAndModifyStatus(OrderConstant.ALIPAY_ORDER_PAYED, OrderConstant.ALIPAY_MODIFY_FAIL);
        if (orderPaidWait == null) {
            orderPaidWait = new ArrayList<>();
        }
        for (AlipayOrderInfoDO alipayOrder : orderPaidWait) {
            String outTradeNo = alipayOrder.getOutTradeNo();
            String tradeNo = alipayOrder.getTradeNo();
            Integer billId = alipayOrder.getBillId();
            BillDO billDO = billDOMapper.selectByPrimaryKey(billId);
            Integer isvId = billDO.getIsvId();
            alipayService
                    .ecoBillModify(isvId, outTradeNo, tradeNo,AlipayTradeStatusConstant.MODIFY_STATUS_PAYED);
            logger.info("当前同步订单,alipayOrder={}",alipayOrder);
        }
        logger.info("===========结束同步(已支付) 本次执行了" + orderPaidWait.size() + "条记录========");
    }

    //在每天7点到7:59期间的每5分钟触发
    @Scheduled(cron = "0 0/5 7 * * ?")
    public void sendClose(){
        //2.order_status为已关闭-同步失败
        logger.info("===========开始同步(已关闭)========");
        List<AlipayOrderInfoDO> orderCloseWait = alipayOrderInfoDOMapper
                .selectByOrderStatusAndModifyStatus(OrderConstant.ALIPAY_ORDER_CLOSED, OrderConstant.ALIPAY_MODIFY_FAIL);
        for (AlipayOrderInfoDO alipayOrder : orderCloseWait) {
            String outTradeNo = alipayOrder.getOutTradeNo();
            Integer billId = alipayOrder.getBillId();
            BillDO billDO = billDOMapper.selectByPrimaryKey(billId);
            Integer isvId = billDO.getIsvId();
            alipayService
                    .ecoBillModify(isvId, outTradeNo, AlipayTradeStatusConstant.MODIFY_STATUS_CLOSE);
            logger.info("当前同步订单,alipayOrder={}",alipayOrder);
        }
        logger.info("===========结束同步(已关闭)========");
    }
}
