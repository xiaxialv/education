package com.zaozao.dao;

import com.zaozao.dataobject.StudentDO;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public interface StudentDOMapper {
    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table student
     *
     * @mbg.generated Thu Jan 10 15:00:21 CST 2019
     */
    int deleteByPrimaryKey(Integer id);
    int deleteByStudentNo(String studentNo);
    int deleteStudentBySchoolId(Integer schoolId);
    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table student
     *
     * @mbg.generated Thu Jan 10 15:00:21 CST 2019
     */
    int insert(StudentDO record);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table student
     *
     * @mbg.generated Thu Jan 10 15:00:21 CST 2019
     */
    int insertSelective(StudentDO record);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table student
     *
     * @mbg.generated Thu Jan 10 15:00:21 CST 2019
     */
    StudentDO selectByPrimaryKey(Integer id);
    StudentDO selectByNameAndPhone(@Param("name") String name,@Param("parentPhone") String parentPhone,@Param("deleteStatus") String deleteStatus);
    List<StudentDO> selectByPrimaryKeyList(@Param("idList")List<Integer> idList);
    List<StudentDO> selectByWechatOpenId(@Param("openId")String openId,@Param("deleteStatus") String deleteStatus);
    List<StudentDO> listStudentBySchoolAndClass(@Param("schoolId") Integer schoolId,@Param("classId")Integer classId,@Param("deleteStatus") String deleteStatus);
    List<StudentDO> listStudent(String deleteStatus);
    List<StudentDO> listStudentBySchool(@Param("schoolId")Integer schoolId,@Param("deleteStatus") String deleteStatus);
    List<StudentDO> listStudentBySchoolIdList(@Param("schoolIdList")List<Integer> schoolIdList,@Param("deleteStatus") String deleteStatus);
    List<StudentDO> listStudentByClassIdList(@Param("classIdList")List<Integer> classIdList,
            @Param("deleteStatus") String deleteStatus);

    StudentDO getStudentBySchoolAndClassAndStudentNo(@Param("schoolId")Integer schoolId,
            @Param("classId")Integer classId,@Param("studentNo")String studentNo,
            @Param("deleteStatus") String deleteStatus);
    StudentDO getStudentByStudentNo(@Param("schoolId")Integer schoolId,@Param("studentNo")String studentNo,@Param(
            "deleteStatus") String deleteStatus);
    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table student
     *
     * @mbg.generated Thu Jan 10 15:00:21 CST 2019
     */
    int updateByPrimaryKeySelective(StudentDO record);
    int updateByStudentNoSelective(StudentDO studentDO);
    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table student
     *
     * @mbg.generated Thu Jan 10 15:00:21 CST 2019
     */
    int updateByPrimaryKey(StudentDO record);
    int selectAmountByStudentNo(@Param("schoolId")Integer schoolId,
            @Param("classId")Integer classId,@Param("studentNo")String studentNo,@Param("deleteStatus") String deleteStatus);
    int selectAmountByClass(@Param("classId")Integer classId,@Param("deleteStatus") String deleteStatus);

    List<StudentDO> listStudentByDynamicQuery(Map<String, Object> map);

    int countAmountByDynamic(Map<String, Object> map);

    StudentDO selectByNameAndOpenId(@Param("name") String name,@Param("openId") String openId,@Param("deleteStatus") String deleteStatus);
}
