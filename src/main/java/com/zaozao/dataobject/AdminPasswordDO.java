package com.zaozao.dataobject;

public class AdminPasswordDO {
    /**
     *
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column admin_password.id
     *
     * @mbg.generated Fri Jan 11 15:46:27 CST 2019
     */
    private Integer id;

    /**
     *
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column admin_password.adminId
     *
     * @mbg.generated Fri Jan 11 15:46:27 CST 2019
     */
    private Integer adminId;

    /**
     *
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column admin_password.admin_encrpt_password
     *
     * @mbg.generated Fri Jan 11 15:46:27 CST 2019
     */
    private String adminEncrptPassword;

    /**
     * This method was generated by MyBatis Generator.
     * This method returns the value of the database column admin_password.id
     *
     * @return the value of admin_password.id
     *
     * @mbg.generated Fri Jan 11 15:46:27 CST 2019
     */
    public Integer getId() {
        return id;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method sets the value of the database column admin_password.id
     *
     * @param id the value for admin_password.id
     *
     * @mbg.generated Fri Jan 11 15:46:27 CST 2019
     */
    public void setId(Integer id) {
        this.id = id;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method returns the value of the database column admin_password.adminId
     *
     * @return the value of admin_password.adminId
     *
     * @mbg.generated Fri Jan 11 15:46:27 CST 2019
     */
    public Integer getAdminId() {
        return adminId;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method sets the value of the database column admin_password.adminId
     *
     * @param adminId the value for admin_password.admin_id
     *
     * @mbg.generated Fri Jan 11 15:46:27 CST 2019
     */
    public void setAdminId(Integer adminId) {
        this.adminId = adminId;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method returns the value of the database column admin_password.admin_encrpt_password
     *
     * @return the value of admin_password.admin_encrpt_password
     *
     * @mbg.generated Fri Jan 11 15:46:27 CST 2019
     */
    public String getAdminEncrptPassword() {
        return adminEncrptPassword;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method sets the value of the database column admin_password.admin_encrpt_password
     *
     * @param adminEncrptPassword the value for admin_password.admin_encrpt_password
     *
     * @mbg.generated Fri Jan 11 15:46:27 CST 2019
     */
    public void setAdminEncrptPassword(String adminEncrptPassword) {
        this.adminEncrptPassword = adminEncrptPassword == null ? null : adminEncrptPassword.trim();
    }
}
