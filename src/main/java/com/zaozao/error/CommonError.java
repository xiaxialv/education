package com.zaozao.error;

/**
 * @author Sidney 2018-12-23.
 */
public interface CommonError {
    int getErrCode();
    String getErrMsg();
    CommonError setErrMsg(String errMsg);
}
