package com.zaozao.service.impl;

import com.zaozao.dao.AdminPermissionDOMapper;
import com.zaozao.dataobject.AdminPermissionDO;
import com.zaozao.service.AdminPermissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @author Sidney 2019-01-15.
 */
@Service
public class AdminPermissionServiceImpl implements AdminPermissionService {

    @Autowired
    private AdminPermissionDOMapper adminPermissionDOMapper;

    @Override
    @Transactional(propagation = Propagation.SUPPORTS)
    public List<AdminPermissionDO> listByAdminId(Integer adminId) {
        List<AdminPermissionDO> adminPermissionDOList = adminPermissionDOMapper.selectByAdminId(adminId);
        return adminPermissionDOList;
    }

    @Override
    @Transactional(propagation = Propagation.SUPPORTS)
    public List<AdminPermissionDO> listBySchoolId(Integer schoolId) {
        List<AdminPermissionDO> adminPermissionDOList = adminPermissionDOMapper.selectBySchoolId(schoolId);
        return adminPermissionDOList;
    }

    @Override
    @Transactional(rollbackFor = Exception.class,propagation = Propagation.REQUIRED)
    public AdminPermissionDO createPermission(AdminPermissionDO adminPermissionDO) {
        adminPermissionDOMapper.insertSelective(adminPermissionDO);
        return adminPermissionDO;
    }

}
