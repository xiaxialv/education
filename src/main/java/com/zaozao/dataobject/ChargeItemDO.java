package com.zaozao.dataobject;

import java.math.BigDecimal;

/**
 * @author Sidney 2019-03-13.
 */
public class ChargeItemDO {
    private String itemName;
    private BigDecimal itemPrice;

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public BigDecimal getItemPrice() {
        return itemPrice;
    }

    public void setItemPrice(BigDecimal itemPrice) {
        this.itemPrice = itemPrice;
    }
}
