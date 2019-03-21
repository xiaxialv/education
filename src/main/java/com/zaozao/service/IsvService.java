package com.zaozao.service;

import com.zaozao.error.BusinessException;
import com.zaozao.service.model.IsvModel;

import java.util.List;

/**
 * @author Sidney 2019-01-14.
 */
public interface IsvService {
    // 创建isv服务商
    IsvModel createIsv(IsvModel isvModel) throws BusinessException;

    // isv服务商列表
    List<IsvModel> listIsv();

    // 根据isv服务商id详情
    IsvModel getIsvById(Integer id);

    // 根据isv管理员id查询
    IsvModel getIsvByAdminId(Integer adminId);

    // 删除is服务商
    void deleteIsvById(Integer id);

    // 修改isv服务商详情
    void updateIsv(IsvModel isvModel);

    // 根据isv管理员id查询是否存在记录
    int countIsvByAdminId(Integer adminId);

}
