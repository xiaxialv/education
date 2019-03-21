package com.zaozao.service.model;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * @author Sidney 2019-01-10.
 */
public class StudentModel {
    private Integer id;
    @NotBlank(message = "学生姓名不能为空")
    private String name;
    @NotNull(message = "性别不能不填写")
    private Byte gender;
    private String studentIdentity;
    private String studentNo;
    @NotNull(message = "班级不能不填写")
    private Integer classId;
    private Integer schoolId;
    @NotBlank(message = "家长手机号不能为空")
    private String parentPhoneNum;
    @NotBlank(message = "家长姓名不能为空")
    private String parentName;
    @NotBlank(message = "监护人不能为空")
    private String guardian;
    @NotNull(message = "是否住校不能不填写")
    private Byte residence;
    @NotNull(message = "是否需要校车接送不能不填写")
    private Byte schoolShuttle;
    private String openId;


    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Byte getGender() {
        return gender;
    }

    public void setGender(Byte gender) {
        this.gender = gender;
    }

    public String getStudentNo() {
        return studentNo;
    }

    public void setStudentNo(String studentNo) {
        this.studentNo = studentNo;
    }

    public Integer getClassId() {
        return classId;
    }

    public void setClassId(Integer classId) {
        this.classId = classId;
    }

    public Integer getSchoolId() {
        return schoolId;
    }

    public void setSchoolId(Integer schoolId) {
        this.schoolId = schoolId;
    }

    public String getParentPhoneNum() {
        return parentPhoneNum;
    }

    public void setParentPhoneNum(String parentPhoneNum) {
        this.parentPhoneNum = parentPhoneNum;
    }

    public String getParentName() {
        return parentName;
    }

    public void setParentName(String parentName) {
        this.parentName = parentName;
    }

    public String getGuardian() {
        return guardian;
    }

    public void setGuardian(String guardian) {
        this.guardian = guardian;
    }

    public Byte getResidence() {
        return residence;
    }

    public void setResidence(Byte residence) {
        this.residence = residence;
    }

    public Byte getSchoolShuttle() {
        return schoolShuttle;
    }

    public void setSchoolShuttle(Byte schoolShuttle) {
        this.schoolShuttle = schoolShuttle;
    }

    public String getOpenId() {
        return openId;
    }

    public void setOpenId(String openId) {
        this.openId = openId;
    }

    public String getStudentIdentity() {
        return studentIdentity;
    }

    public void setStudentIdentity(String studentIdentity) {
        this.studentIdentity = studentIdentity;
    }
}
