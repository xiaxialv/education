package com.zaozao.service.impl;

import com.zaozao.dao.AdminPermissionDOMapper;
import com.zaozao.dao.SchoolDOMapper;
import com.zaozao.dataobject.AdminPermissionDO;
import com.zaozao.dataobject.SchoolDO;
import com.zaozao.service.*;
import com.zaozao.service.model.SchoolModel;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Sidney 2019-01-10.
 */
@Service
public class SchoolServiceImpl implements SchoolService {

    @Autowired
    private SchoolDOMapper schoolDOMapper;

    @Autowired
    private AdminPermissionDOMapper adminPermissionDOMapper;

    @Autowired
    private AdminService adminService;

    @Autowired
    private ClassService classService;

    @Autowired
    private StudentService studentService;

    @Autowired
    private BillService billService;

    @Override
    @Transactional(rollbackFor = Exception.class,propagation = Propagation.REQUIRED)
    public SchoolModel createSchool(SchoolModel schoolModel){
        SchoolDO schoolDO = this.convertDoFromModel(schoolModel);
        schoolDOMapper.insertSelective(schoolDO);
        schoolModel.setId(schoolDO.getId());
        return this.getSchoolById(schoolModel.getId());
    }

    @Override
    @Transactional(propagation = Propagation.SUPPORTS)
    public List<SchoolModel> listSchool() {
        List<SchoolDO> schoolDOList = schoolDOMapper.listSchool();
        List<SchoolModel> schoolModelList = schoolDOList.stream().map(schoolDO -> {
            SchoolModel schoolModel = this.convertModelFromDataObject(schoolDO);
            return schoolModel;
        }).collect(Collectors.toList());
        return schoolModelList;
    }

    @Override
    @Transactional(propagation = Propagation.SUPPORTS)
    public List<SchoolModel> listSchoolByIsvId(Integer isvId) {
        List<SchoolDO> schoolDOList = schoolDOMapper.listSchoolByIsvId(isvId);
        List<SchoolModel> schoolModelList = schoolDOList.stream().map(schoolDO -> {
            SchoolModel schoolModel = this.convertModelFromDataObject(schoolDO);
            return schoolModel;
        }).collect(Collectors.toList());
        return schoolModelList;
    }

    @Override
    @Transactional(propagation = Propagation.SUPPORTS)
    public SchoolModel getSchoolById(Integer id) {
        SchoolDO schoolDO = schoolDOMapper.selectByPrimaryKey(id);
        SchoolModel schoolModel = this.convertModelFromDataObject(schoolDO);
        return schoolModel;
    }

    @Override
    @Transactional(rollbackFor = Exception.class,propagation = Propagation.REQUIRED)
    public void updateSchool(SchoolModel schoolModel) {
        SchoolDO schoolDO = this.convertDoFromModel(schoolModel);
        schoolDOMapper.updateByPrimaryKeySelective(schoolDO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class,propagation = Propagation.REQUIRED)
    public void deleteSchool(Integer id) {
        // 删除学校记录
        schoolDOMapper.deleteByPrimaryKey(id);
        // 从permission表中获取该学校Id-->对应的所有管理员Id
        List<AdminPermissionDO> adminPermissionDOList = adminPermissionDOMapper.selectBySchoolId(id);
        List<Integer> adminIdList = adminPermissionDOList.stream().map(AdminPermissionDO::getAdminId).collect(Collectors.toList());
        // 删除admin表中的所有该学校的管理人员账户(含permission表中该schoolId对应的所有adminId关联关系)
        adminService.deleteByAdminIdList(adminIdList);
        // 删除class表中该学校所有班级
        classService.deleteClassBySchoolId(id);
        // 删除student表中该学校所有学生
        studentService.deleteStudentBySchoolId(id);
        // 删除bill表中该学校所有账单
        billService.deleteBillBySchoolId(id);
        // 删除billItem表中该学校所有缴费类型
        billService.deleteBillItemBySchoolId(id);
    }

    @Override
    @Transactional(propagation = Propagation.SUPPORTS)
    public List<SchoolModel> listSchoolByGivenIdList(List<Integer> schoolIdList) {
        List<SchoolDO> schoolDOList = schoolDOMapper.listSchoolByGivenIdList(schoolIdList);
        List<SchoolModel> schoolModelList = schoolDOList.stream().map(schoolDO -> {
            SchoolModel schoolModel = this.convertModelFromDataObject(schoolDO);
            return schoolModel;
        }).collect(Collectors.toList());
        return schoolModelList;
    }

    private SchoolModel convertModelFromDataObject(SchoolDO schoolDO) {
        SchoolModel schoolModel = new SchoolModel();
        BeanUtils.copyProperties(schoolDO, schoolModel);
        return schoolModel;
    }

    private SchoolDO convertDoFromModel(SchoolModel schoolModel) {
        if (schoolModel == null) {
            return null;
        }
        SchoolDO schoolDO = new SchoolDO();
        BeanUtils.copyProperties(schoolModel,schoolDO);
        return schoolDO;
    }

}


