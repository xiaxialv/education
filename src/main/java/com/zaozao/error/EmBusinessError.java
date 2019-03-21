package com.zaozao.error;

/**
 * @author Sidney 2018-12-23.
 */
public enum EmBusinessError implements CommonError {
    //通用类型错误10001
    PARAM_VALIDATION_ERROR(10001, "参数异常"),
    UNKNOWN_ERROR(10002, "未知错误"),
    //20000开头为用户相关错误定义
    USER_NAME_EXIST(20001, "账号名已存在"),
    USER_LOGIN_FAIL(20002, "用户账号名或密码不正确"),
    USER_NOT_LOGIN(20003, "您还没有登录,请登录后再进行操作"),
    USER_PASSWORD_DIFFERENT(20003, "您两次输入的密码不同,请重新输入"),
    //30000开头为交易信息相关错误定义
    PAY_FAILED(30001,"缴费失败"),
    //40000开头为导入导出相关错误定义
    FILE_TYPE_ERROR(40001,"上传文件格式不正确"),
    EXCEL_ROW_TYPE_ERROR(40002,"表格内容格式异常"),
    //50000开头为权限相关错误定义
    ACCESS_VIOLATION_ERROR(50001,"您无此权限"),
    ACCESS_VIOLATION_CLASS_ERROR(50002,"您只能操作您下一级的管理员"),
    //60000开头为友情提示
    SCHOOL_ADMIN_ERROR(60001,"您还没有创建学校管理员"),
    SCHOOL_INFO_ERROR(60002,"您没有相关学校记录"),
    ISV_INFO_ERROR(60003,"您没有相关isv记录"),
    ISV_INFO_EXIST(60004,"您已有isv记录不可重复创建"),
//    CLASS_SCHOOL_INFO_ERROR(60005,"您还没有绑定相关学校"),
    //70000开头为学生相关错误提示
    STUDENT_NOT_FIND(70001,"学生不存在,请确认信息无误"),
    //80000开头为账单相关错误提示
    BILL_UNPAID_ERROR(80001,"不允许修改已缴费账单"),
    BILL_UNEXIT_ERROR(80002,"账单不存在"),
    BILL_ITEM_NULL(80002,"您还没有添加相应缴费类型,请先添加缴费类型"),
    //90000开头为班级相关错误提示
    CLASS_NUEXIST_ERROR(90001,"该班级不存在,请确认信息无误"),
    CLASS_EXIST_ERROR(90001,"该班级已存在,请勿重复创建"),
    CLASS_SYSTEM_ERROR(90001,"年级不应超过学校学制"),
    //11000开头为微信小程序相关错误提示
    CODE_NULL_ERROR(11001,"invalid null, code is null."),
    ORDER_PAY_ERROR(11002,"请勿重复支付"),
    ISV_CONFIG_ERROR(11003,"服务商配置错误"),
    //12000开头为支付宝中小学教育缴费相关错误提示
    AUTH_SCHOOL_ERROR(12001,"请使用学校管理员账号操作"),
    SEND_SCHOOL_ERROR(12002,"发送学校信息至支付宝平台失败"),
    SEND_BILL_ERROR(12003,"发送账单至支付宝平台失败"),
    BILL_STATUS_ERROR(12003,"账单状态异常"),
    CLIENT_INIT_ERROR(12004,"初始化数据异常,请联系isv服务商");
    private int errCode;
    private String errMsg;

    EmBusinessError(int errCode, String errMsg) {
        this.errCode = errCode;
        this.errMsg = errMsg;
    }

    @Override
    public int getErrCode() {
        return this.errCode;
    }

    @Override
    public String getErrMsg() {
        return this.errMsg;
    }

    @Override
    public CommonError setErrMsg(String errMsg) {
        this.errMsg = errMsg;
        return this;
    }}
