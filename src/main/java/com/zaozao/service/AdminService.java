package com.zaozao.service;

import com.zaozao.dataobject.AdminInfoDO;
import com.zaozao.error.BusinessException;
import com.zaozao.service.model.AdminModel;

import java.util.List;

/**
 * @author Sidney 2019-01-11.
 */
public interface AdminService {
    // 添加管理员用户
    AdminModel createAdmin(AdminModel adminModel) throws BusinessException;

    // 删除管理员用户
    void deleteAdminById(Integer adminId);
    // 删除管理员用户List
    void deleteByAdminIdList(List<Integer> adminId);

    // 管理员用户列表浏览
    List<AdminModel> listAdmin();

    // 管理员用户修改
    void updateAdmin(AdminModel adminModel) throws BusinessException;

    // 管理员用户登录校验
    AdminModel validateLogin(String accountName,String adminEncrptPassword) throws BusinessException;

    // 根据id查询管理员用户
    AdminInfoDO getAdmin(Integer adminId);

    // 查询指定角色指定id范围的管理员
    AdminModel searchByRoleAndIdList(Byte accountRole,List<Integer> adminIdList);

    // 查询指定角色范围指定id范围的管理员
    List<AdminModel> searchByRoleListAndIdList(List<Byte> accountRoleList,List<Integer> adminIdList);

}
