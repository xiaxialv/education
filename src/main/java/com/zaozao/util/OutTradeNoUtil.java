package com.zaozao.util;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author Sidney 2019-03-13.
 */
public class OutTradeNoUtil {
    public static String getOutTradeNo(Integer billId) {
        String today = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        String code = PayUtil.createCode(10);
        return today+code+billId.toString();
    }
}
