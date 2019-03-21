package com.zaozao.service.model;

/**
 * @author Sidney 2019-01-10.
 */
public class ClassModel {
    private Integer id;
    private Integer gradeNum;
    private Integer classNum;
    private Integer schoolId;
    private String headTeacherName;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getGradeNum() {
        return gradeNum;
    }

    public void setGradeNum(Integer gradeNum) {
        this.gradeNum = gradeNum;
    }

    public Integer getClassNum() {
        return classNum;
    }

    public void setClassNum(Integer classNum) {
        this.classNum = classNum;
    }

    public Integer getSchoolId() {
        return schoolId;
    }

    public void setSchoolId(Integer schoolId) {
        this.schoolId = schoolId;
    }

    public String getHeadTeacherName() {
        return headTeacherName;
    }

    public void setHeadTeacherName(String headTeacherName) {
        this.headTeacherName = headTeacherName;
    }

    @Override
    public String toString() {
        return "ClassModel{" + "id=" + id + ", gradeNum=" + gradeNum + ", classNum=" + classNum + ", schoolId="
                + schoolId + ", headTeacherName='" + headTeacherName + '\'' + '}';
    }
}
