package com.zaozao.service;

import com.zaozao.dataobject.AdminPermissionDO;

import java.util.List;

/**
 * @author Sidney 2019-01-15.
 */
public interface AdminPermissionService {
    // 根据管理员id获得可操作学校的id
    List<AdminPermissionDO> listByAdminId(Integer adminId);
    // 根据学校的id获得可操作管理员id
    List<AdminPermissionDO> listBySchoolId(Integer schoolId);
    // 新增AdminPermission
    AdminPermissionDO createPermission(AdminPermissionDO adminPermissionDO);
    //删除
}
