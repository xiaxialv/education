package com.zaozao.service.impl;

import com.zaozao.dao.AdminInfoDOMapper;
import com.zaozao.dao.AdminPasswordDOMapper;
import com.zaozao.dao.AdminPermissionDOMapper;
import com.zaozao.dataobject.AdminInfoDO;
import com.zaozao.dataobject.AdminPasswordDO;
import com.zaozao.error.BusinessException;
import com.zaozao.error.EmBusinessError;
import com.zaozao.service.AdminService;
import com.zaozao.service.model.AdminModel;
import com.zaozao.validator.ValidationResult;
import com.zaozao.validator.ValidatorImpl;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Sidney 2019-01-11.
 */
@Service
public class AdminServiceImpl implements AdminService {

    @Autowired
    private AdminInfoDOMapper adminInfoDOMapper;

    @Autowired
    private AdminPasswordDOMapper adminPasswordDOMapper;

    @Autowired
    private AdminPermissionDOMapper adminPermissionDOMapper;

    @Autowired
    private ValidatorImpl validator;

    @Override
    @Transactional(rollbackFor = Exception.class,propagation = Propagation.REQUIRED)
    public AdminModel createAdmin(AdminModel adminModel) throws BusinessException {
        ValidationResult result = validator.validate(adminModel);
        if (result.isHasError()) {
            throw new BusinessException(EmBusinessError.PARAM_VALIDATION_ERROR,result.getErrMsg());
        }
        //转化adminModel-->dataobject
        AdminInfoDO adminInfoDO = this.convertAdminDoFromItemModel(adminModel);
        try {
            adminInfoDOMapper.insertSelective(adminInfoDO);
        } catch (DuplicateKeyException ex) {
            throw new BusinessException(EmBusinessError.USER_NAME_EXIST);
        }
        //写入数据库
        adminModel.setId(adminInfoDO.getId());
        AdminPasswordDO adminPasswordDO = this.convertAdminPasswordDoFromItemModel(adminModel);
        adminPasswordDOMapper.insertSelective(adminPasswordDO);
        return adminModel;
    }

    @Override
    @Transactional(rollbackFor = Exception.class,propagation = Propagation.REQUIRED)
    public void deleteAdminById(Integer adminId) {
        adminInfoDOMapper.deleteByPrimaryKey(adminId);
        adminPasswordDOMapper.deleteByAdminId(adminId);
        // 删除permission表中该adminId关联关系记录
        adminPermissionDOMapper.deleteByAdminId(adminId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class,propagation = Propagation.REQUIRED)
    public void deleteByAdminIdList(List<Integer> adminId) {
        for (Integer id : adminId) {
            this.deleteAdminById(id);
        }
    }

    @Override
    @Transactional(propagation = Propagation.SUPPORTS)
    public List<AdminModel> listAdmin() {
        List<AdminInfoDO> adminInfoDOList = adminInfoDOMapper.listAdminInfoDO();
        if (CollectionUtils.isEmpty(adminInfoDOList)) {
            return null;
        }
//        System.out.println(adminInfoDOList);
        //控制台输出Page{count=true, pageNum=1, pageSize=10, startRow=0, endRow=10, total=8, pages=1, reasonable=true, pageSizeZero=false}
        //将list中的每一个AdminInfoDO都转化为Model最后返回一个list
        List<AdminModel> adminModelList = adminInfoDOList.stream().map(adminInfoDO -> {
            AdminPasswordDO adminPasswordDO = adminPasswordDOMapper.selectByAdminId(adminInfoDO.getId());
            AdminModel adminModel = this.convertModelFromDataObject(adminInfoDO,adminPasswordDO);
            return adminModel;
        }).collect(Collectors.toList());
        return adminModelList;
    }

    @Override
    @Transactional(rollbackFor = Exception.class,propagation = Propagation.REQUIRED)
    public void updateAdmin(AdminModel adminModel) throws BusinessException {
        AdminInfoDO adminInfoDO = this.convertAdminDoFromItemModel(adminModel);
        try {
            adminInfoDOMapper.updateByPrimaryKeySelective(adminInfoDO);
        } catch (DuplicateKeyException ex) {
            throw new BusinessException(EmBusinessError.USER_NAME_EXIST);
        }
        //写入数据库
        adminModel.setId(adminInfoDO.getId());
        AdminPasswordDO adminPasswordDO = this.convertAdminPasswordDoFromItemModel(adminModel);
        adminPasswordDOMapper.updateByAdminIdSelective(adminPasswordDO);
    }

    @Override
    @Transactional(propagation = Propagation.SUPPORTS)
    public AdminModel validateLogin(String accountName, String adminEncrptPassword) throws BusinessException {
        //通过用户的账号名获得用户信息
        AdminInfoDO adminInfoDO = adminInfoDOMapper.selectByAccountName(accountName);
        if (adminInfoDO == null) {
            throw new BusinessException(EmBusinessError.USER_LOGIN_FAIL);
        }
        AdminPasswordDO adminPasswordDO = adminPasswordDOMapper.selectByAdminId(adminInfoDO.getId());
        AdminModel adminModel = convertModelFromDataObject(adminInfoDO, adminPasswordDO);
        //比对用户信息内加密的密码是否和传输进来的密码匹配
        if (!StringUtils.equals(adminEncrptPassword, adminModel.getAdminEncrptPassword())) {
            throw new BusinessException(EmBusinessError.USER_LOGIN_FAIL);
        }
        return adminModel;
    }

    @Override
    @Transactional(propagation = Propagation.SUPPORTS)
    public AdminInfoDO getAdmin(Integer adminId) {
        return adminInfoDOMapper.selectByPrimaryKey(adminId);
    }

    @Override
    @Transactional(propagation = Propagation.SUPPORTS)
    public AdminModel searchByRoleAndIdList(Byte accountRole, List<Integer> adminIdList) {
        AdminInfoDO adminInfoDO = adminInfoDOMapper.selectByAccountRoleAndId(accountRole, adminIdList);
        AdminPasswordDO adminPasswordDO = adminPasswordDOMapper.selectByAdminId(adminInfoDO.getId());
        return this.convertModelFromDataObject(adminInfoDO, adminPasswordDO);
    }

    @Override
    @Transactional(propagation = Propagation.SUPPORTS)
    public List<AdminModel> searchByRoleListAndIdList(List<Byte> accountRoleList, List<Integer> adminIdList) {
        List<AdminInfoDO> adminInfoDOList = adminInfoDOMapper.selectByAccountRoleListAndIdList(accountRoleList, adminIdList);
        return adminInfoDOList.stream().map(adminInfoDO -> {
            AdminPasswordDO adminPasswordDO = adminPasswordDOMapper.selectByAdminId(adminInfoDO.getId());
            return this.convertModelFromDataObject(adminInfoDO, adminPasswordDO);
        }).collect(Collectors.toList());
    }


    private AdminInfoDO convertAdminDoFromItemModel(AdminModel adminModel) {
        if (adminModel == null) {
            return null;
        }
        AdminInfoDO adminInfoDO = new AdminInfoDO();
        BeanUtils.copyProperties(adminModel,adminInfoDO);

        DateTime registerDate = adminModel.getRegisterDate();
        if (registerDate != null) {
            Date date = registerDate.toDate();
            adminInfoDO.setRegisterDate(date);
        }
        return adminInfoDO;
    }

    private AdminPasswordDO convertAdminPasswordDoFromItemModel(AdminModel adminModel) {
        if (adminModel == null) {
            return null;
        }
        AdminPasswordDO adminPasswordDO = new AdminPasswordDO();
        adminPasswordDO.setAdminEncrptPassword(adminModel.getAdminEncrptPassword());
        adminPasswordDO.setAdminId(adminModel.getId());
        return adminPasswordDO;
    }
    private AdminModel convertModelFromDataObject(AdminInfoDO adminInfoDO,AdminPasswordDO adminPasswordDO) {
        AdminModel adminModel = new AdminModel();
        BeanUtils.copyProperties(adminInfoDO,adminModel);
        adminModel.setRegisterDate(new DateTime(adminInfoDO.getRegisterDate()));
        adminModel.setAdminEncrptPassword(adminPasswordDO.getAdminEncrptPassword());
        return adminModel;
    }
}
