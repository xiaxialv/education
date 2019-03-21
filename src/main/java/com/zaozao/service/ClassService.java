package com.zaozao.service;

import com.zaozao.error.BusinessException;
import com.zaozao.service.model.ClassModel;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * @author Sidney 2019-01-10.
 */
public interface ClassService {
    // 创建班级
    void createClass(ClassModel classModel) throws BusinessException;
    // 根据学校编号查询班级列表
    List<ClassModel> listClassBySchoolId(Integer schoolId,String deleteStatus);
    // 根据学校编号集合查询班级列表
    List<ClassModel> listClassBySchoolIds(List<Integer> schoolIds,String deleteStatus);
    // 查询所有班级列表
    List<ClassModel> listClass(String deleteStatus);
    // 班级详情浏览
    ClassModel getClassById(Integer id);
    // 修改班级详情
    void updateClass(ClassModel classModel);
    // 升级班级(最高年级毕业,其他年级升一级,毕业班级关联学生/账单软删除)
    void upgradeClass(Integer schoolId,Integer schoolSystem,String deleteStatus);

    // 软删除班级
    void softDeleteClass(Integer id);

    // 硬删除班级
    void deleteClass(Integer id);

    // 根据学校id删除班级(硬删除)
    void deleteClassBySchoolId(Integer schoolId);

    // 班级导入Excel
    boolean batchImport(String fileName, MultipartFile file,Integer schoolId) throws Exception;
}
