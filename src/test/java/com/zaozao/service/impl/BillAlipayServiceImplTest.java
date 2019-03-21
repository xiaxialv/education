package com.zaozao.service.impl;

import com.zaozao.App;
import com.zaozao.service.BillAlipayService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.math.BigDecimal;

/**
 * @author Sidney 2019-03-20.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = App.class)
public class BillAlipayServiceImplTest {

    @Autowired
    BillAlipayService billAlipayService;

    @Test
    public void updateBill() {
        Integer id=1;
        String studentNo="1001";
        String billName="2019年下学期";
        Integer billItemId=1;
        BigDecimal billAmount=new BigDecimal(5);
        String comment="111";
        String endDate="2020-01-02";
        billAlipayService.updateBill(id,studentNo, billName, billItemId, billAmount, comment,endDate);
    }

    @Test
    public void delete() {
        billAlipayService.delete(1);
    }

    @Test
    public void clickCollection() {
        billAlipayService.clickCollection("1", (byte) 2,"test","20190320");
    }
}
