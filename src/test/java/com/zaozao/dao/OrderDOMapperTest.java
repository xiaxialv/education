//package com.zaozao.dao;
//
//import com.zaozao.Constant.OrderConstant;
//import com.zaozao.dataobject.WechatOrderInfoDO;
//import org.junit.Test;
//import org.junit.runner.RunWith;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.test.context.junit4.SpringRunner;
//
//import java.util.Date;
//
///**
// * @author Sidney 2019-03-05.
// */
//@RunWith(SpringRunner.class)
//@SpringBootTest
//public class OrderDOMapperTest {
//    @Autowired
//    private OrderDOMapper orderDOMapper;
//
//    @Test
//    public void findOne() {
//        WechatOrderInfoDO orderDO = orderDOMapper.selectByBillValid(1,"1", OrderConstant.WECHAT_ORDER__NEW,OrderConstant.WECHAT_PAY_WAIT);
//        Date createTime = orderDO.getCreateTime();
//        long time = createTime.getTime();
//        time += (115 * 60 * 1000);
//        Date expireTime = new Date(time);
//        orderDO.setTimeExpire(expireTime);
//        orderDOMapper.updateByPrimaryKeySelective(orderDO);
//
////        double minute = ((double)(date1.getTime()-date2.getTime()))/(60*1000);
////        long m = (long) Math.ceil(minute);
////        return m;
//
//    }
//
//}
