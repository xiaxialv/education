package com.zaozao.controller.viewobject;

/**
 * @author Sidney 2019-01-10.
 */
public class StudentVO {
    private Integer id;
    private String name;
    private Byte gender;
    private String studentIdentity;
    private String studentNo;
    private Integer classId;
    private String className;
    private Integer schoolId;
    private String schoolName;
    private String parentPhoneNum;
    private String parentName;
    private String guardian;
    private Byte residence;
    private Byte schoolShuttle;

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

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getSchoolName() {
        return schoolName;
    }

    public void setSchoolName(String schoolName) {
        this.schoolName = schoolName;
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

    public String getStudentIdentity() {
        return studentIdentity;
    }

    public void setStudentIdentity(String studentIdentity) {
        this.studentIdentity = studentIdentity;
    }
}
