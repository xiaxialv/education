package com.zaozao.service;

import java.math.BigDecimal;

/**
 * @author Sidney 2019-03-20.
 */
public interface BillAlipayService {
    // 更新操作
    void updateBill(Integer id,String studentNo, String billName, Integer billItemId,
            BigDecimal billAmount, String comment, String endDate);

    // 删除操作
    void delete(Integer id);

    // 一键缴费操作
    void clickCollection(String ids, Byte payType, String comment, String billNum);
}
