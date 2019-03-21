package com.zaozao.Constant;

/**
 * @author Sidney 2019-03-11.
 */
public class SchoolConstant {
    // 托幼小初高。1、托, 2、幼,  3、小,  4、初,  5、高。
    public static final int SCHOOL_TYPE_NURSERY = 1;
    public static final int SCHOOL_TYPE_KINDERGARTEN = 2;
    public static final int SCHOOL_TYPE_PRIMARY = 3;
    public static final int SCHOOL_TYPE_MIDDLE = 4;
    public static final int SCHOOL_TYPE_HIGH = 5;

    // 学校在支付宝的状态:是否已同步
    public static final byte SCHOOL_SEND_UNSEND = 0;
    public static final byte SCHOOL_SEND_SUCCESS = 1;
    public static final byte SCHOOL_SEND_FAIL = 2;
}
