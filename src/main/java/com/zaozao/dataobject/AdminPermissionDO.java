package com.zaozao.dataobject;

public class AdminPermissionDO {
    /**
     *
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column admin_permission.id
     *
     * @mbg.generated Tue Jan 15 14:12:34 CST 2019
     */
    private Integer id;

    /**
     *
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column admin_permission.admin_id
     *
     * @mbg.generated Tue Jan 15 14:12:34 CST 2019
     */
    private Integer adminId;

    /**
     *
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column admin_permission.school_id
     *
     * @mbg.generated Tue Jan 15 14:12:34 CST 2019
     */
    private Integer schoolId;

    /**
     * This method was generated by MyBatis Generator.
     * This method returns the value of the database column admin_permission.id
     *
     * @return the value of admin_permission.id
     *
     * @mbg.generated Tue Jan 15 14:12:34 CST 2019
     */
    public Integer getId() {
        return id;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method sets the value of the database column admin_permission.id
     *
     * @param id the value for admin_permission.id
     *
     * @mbg.generated Tue Jan 15 14:12:34 CST 2019
     */
    public void setId(Integer id) {
        this.id = id;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method returns the value of the database column admin_permission.admin_id
     *
     * @return the value of admin_permission.admin_id
     *
     * @mbg.generated Tue Jan 15 14:12:34 CST 2019
     */
    public Integer getAdminId() {
        return adminId;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method sets the value of the database column admin_permission.admin_id
     *
     * @param adminId the value for admin_permission.admin_id
     *
     * @mbg.generated Tue Jan 15 14:12:34 CST 2019
     */
    public void setAdminId(Integer adminId) {
        this.adminId = adminId;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method returns the value of the database column admin_permission.school_id
     *
     * @return the value of admin_permission.school_id
     *
     * @mbg.generated Tue Jan 15 14:12:34 CST 2019
     */
    public Integer getSchoolId() {
        return schoolId;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method sets the value of the database column admin_permission.school_id
     *
     * @param schoolId the value for admin_permission.school_id
     *
     * @mbg.generated Tue Jan 15 14:12:34 CST 2019
     */
    public void setSchoolId(Integer schoolId) {
        this.schoolId = schoolId;
    }
}