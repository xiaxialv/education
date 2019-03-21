package com.zaozao.controller.viewobject;

import java.util.List;

/**
 * @author Sidney 2019-01-11.
 */
public class SchoolGradeClassVO {
    private Integer schoolId;
    private String schoolName;

    private List<ClassInfo> classInfoList;

    public static class ClassInfo{
        private Integer classId;
        private Integer gradeNum;
        private Integer classNum;
//        private String className;

        public Integer getClassId() {
            return classId;
        }

        public void setClassId(Integer classId) {
            this.classId = classId;
        }

//        public String getClassName() {
//            return className;
//        }

//        public void setClassName(String className) {
//            this.className = className;
//        }

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
    }

    private List<ItemInfo> itemInfoList;
    public static class ItemInfo {
        private Integer itemId;
        private String itemName;

        public Integer getItemId() {
            return itemId;
        }

        public void setItemId(Integer itemId) {
            this.itemId = itemId;
        }

        public String getItemName() {
            return itemName;
        }

        public void setItemName(String itemName) {
            this.itemName = itemName;
        }
    }


    public Integer getSchoolId() {
        return schoolId;
    }

    public void setSchoolId(Integer schoolId) {
        this.schoolId = schoolId;
    }

    public String getSchoolName() {
        return schoolName;
    }

    public void setSchoolName(String schoolName) {
        this.schoolName = schoolName;
    }

    public List<ClassInfo> getClassInfoList() {
        return classInfoList;
    }

    public void setClassInfoList(List<ClassInfo> classInfoList) {
        this.classInfoList = classInfoList;
    }

    public List<ItemInfo> getItemInfoList() {
        return itemInfoList;
    }

    public void setItemInfoList(List<ItemInfo> itemInfoList) {
        this.itemInfoList = itemInfoList;
    }
}
