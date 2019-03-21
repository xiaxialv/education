package com.zaozao.service.impl;

import com.zaozao.dao.AdminPermissionDOMapper;
import com.zaozao.dao.IsvDOMapper;
import com.zaozao.dataobject.AdminPermissionDO;
import com.zaozao.dataobject.IsvDO;
import com.zaozao.error.BusinessException;
import com.zaozao.error.EmBusinessError;
import com.zaozao.service.AdminService;
import com.zaozao.service.IsvService;
import com.zaozao.service.SchoolService;
import com.zaozao.service.model.IsvModel;
import com.zaozao.validator.ValidationResult;
import com.zaozao.validator.ValidatorImpl;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Sidney 2019-01-14.
 */
@Service
public class IsvServiceImpl implements IsvService {

    @Autowired
    private IsvDOMapper isvDOMapper;

    @Autowired
    private ValidatorImpl validator;

    @Autowired
    private AdminPermissionDOMapper adminPermissionDOMapper;

    @Autowired
    private SchoolService schoolService;

    @Autowired
    private AdminService adminService;

    @Override
    @Transactional(rollbackFor = Exception.class,propagation = Propagation.REQUIRED)
    public IsvModel createIsv(IsvModel isvModel) throws BusinessException {
        //校验入参
        ValidationResult result = validator.validate(isvModel);
        if (result.isHasError()) {
            throw new BusinessException(EmBusinessError.PARAM_VALIDATION_ERROR,result.getErrMsg());
        }
        //转化Model-->dataobject
        IsvDO isvDO = this.convertDoFromModel(isvModel);
        //写入数据库
        isvDOMapper.insertSelective(isvDO);
        isvModel.setId(isvDO.getId());
        //返回创建完成的对象
        return this.getIsvById(isvModel.getId());
    }

    @Override
    @Transactional(propagation = Propagation.SUPPORTS)
    public List<IsvModel> listIsv() {
        List<IsvDO> isvDOList = isvDOMapper.listIsv();
        List<IsvModel> isvModelList = isvDOList.stream().map(isvDO -> {
            IsvModel isvModel = this.convertModelFromDataObject(isvDO);
            return isvModel;
        }).collect(Collectors.toList());
        return isvModelList;
    }

    @Override
    @Transactional(propagation = Propagation.SUPPORTS)
    public IsvModel getIsvById(Integer id) {
        IsvDO isvDO = isvDOMapper.selectByPrimaryKey(id);
        return this.convertModelFromDataObject(isvDO);
    }

    @Override
    @Transactional(propagation = Propagation.SUPPORTS)
    public IsvModel getIsvByAdminId(Integer adminId) {
        IsvDO isvDO = isvDOMapper.selectByAdminId(adminId);
        IsvModel isvModel = this.convertModelFromDataObject(isvDO);
        return isvModel;
    }

    @Override
    @Transactional(rollbackFor = Exception.class,propagation = Propagation.REQUIRED)
    public void deleteIsvById(Integer id) {
        // 删除isv记录,isv表中获得adminId,将其从admin表中删除isv管理员,删除permission中isv的记录
        IsvDO isvDO = isvDOMapper.selectByPrimaryKey(id);
        isvDOMapper.deleteByPrimaryKey(id);
        Integer adminId = isvDO.getAdminId();
        adminService.deleteAdminById(adminId);
        // 从permission表中根据adminId获得下属所有学校id
        List<AdminPermissionDO> adminPermissionDOList = adminPermissionDOMapper.selectByAdminId(adminId);
        List<Integer> schoolIdList =
                adminPermissionDOList.stream().map(AdminPermissionDO::getSchoolId).collect(Collectors.toList());
        // 删除学校记录,调用schoolService删除所有管理员.学校.班级.学生.账单
        for (Integer schoolId : schoolIdList) {
            schoolService.deleteSchool(schoolId);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class,propagation = Propagation.REQUIRED)
    public void updateIsv(IsvModel isvModel) {
        IsvDO isvDO = this.convertDoFromModel(isvModel);
        isvDOMapper.updateByPrimaryKeySelective(isvDO);
    }

    @Override
    @Transactional(propagation = Propagation.SUPPORTS)
    public int countIsvByAdminId(Integer adminId) {
        return isvDOMapper.countIsvByAdminId(adminId);
    }

    private IsvModel convertModelFromDataObject(IsvDO isvDO) {
        IsvModel isvModel = new IsvModel();
        BeanUtils.copyProperties(isvDO, isvModel);
        return isvModel;
    }
    private IsvDO convertDoFromModel(IsvModel isvModel) {
        if (isvModel == null) {
            return null;
        }
        IsvDO isvDO = new IsvDO();
        BeanUtils.copyProperties(isvModel,isvDO);
        return isvDO;
    }
}
