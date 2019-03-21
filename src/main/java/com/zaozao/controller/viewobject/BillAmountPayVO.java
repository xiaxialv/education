package com.zaozao.controller.viewobject;

import java.math.BigDecimal;

public class BillAmountPayVO {
    private BigDecimal value;
    private String name;

    public BigDecimal getValue() {
        return value;
    }

    public void setValue(BigDecimal value) {
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
