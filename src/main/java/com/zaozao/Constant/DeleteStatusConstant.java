package com.zaozao.Constant;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;

/**
 * @author Sidney 2019-01-25.
 */
public class DeleteStatusConstant {
    // 删除状态0未删除
    public static final String DELETE_STATUS_STAY = "0";

    // 删除状态1-已删除
    public static final String DELETE_STATUS_HISTORY = "1-%";

    // 动态生成非0删除状态码
    public static String createDeleteStatus() {
        DateTime now = new DateTime();
        return "1-"+now.toString(DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss"));
    }

}
