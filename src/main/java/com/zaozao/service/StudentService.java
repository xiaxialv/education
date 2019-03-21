package com.zaozao.service;

import com.zaozao.error.BusinessException;
import com.zaozao.service.model.StudentModel;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * @author Sidney 2019-01-10.
 */
public interface StudentService {
    // 学生新增
    StudentModel createStudent(StudentModel studentModel) throws BusinessException;

    // 获取同校同班学生列表
    List<StudentModel> listStudentBySchoolAndClass(Integer schoolId,Integer classId,String deleteStatus);

    // 获取系统所有学生列表
    List<StudentModel> listStudent(String deleteStatus);

    // 获取某一学校全校学生列表
    List<StudentModel> listStudentBySchool(Integer schoolId,String deleteStatus);

    // 获取某几个学校学生列表
    List<StudentModel> listStudentBySchoolIdList(List<Integer> schoolIdList,String deleteStatus);

    // 获取指定学校班级学号的学生详情
    StudentModel getStudentBySchoolAndClassAndStudentNo(Integer schoolId,Integer classId,String studentNo,
            String deleteStatus);

    // 根据学号获取学生
    StudentModel getStudentByStudentNo(Integer schoolId,String studentNo,String deleteStatus) throws BusinessException;

    // 根据主键id获取学生
    StudentModel getStudentByPrimaryKey(Integer id);

    // 根据学号更新学生详情,返回学生对象根据状态查询
    StudentModel updateStudentInfo(StudentModel studentModel,String deleteStatus);

    // 学生软删除
    void softDeleteStudent(Integer id);

    // 学生批量软删除
    void softDeleteStudentBatch(List<Integer> idList);

    // 根据班级学生软删除
    void softDeleteStudentByClassIdList(List<Integer> classIdList);

    // 根据学校硬删除学生
    void deleteStudentBySchoolId(Integer schoolId);

    // 学生导入Excel
    boolean batchImport(String fileName, MultipartFile file,Integer schoolId,Integer classId) throws Exception;

    // 同校同班学生人数
    Integer studentAmountByClass(Integer classId,String deleteStatus);

    // 动态查询学生记录-超管
    List<StudentModel> listStudentByDynamicQuery(Map<String, Object> requestMap);

}
