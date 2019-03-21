package com.zaozao.dataobject;

public class SchoolDO {
    /**
     *
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column school.id
     *
     * @mbg.generated Thu Jan 10 15:00:21 CST 2019
     */
    private Integer id;

    /**
     *
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column school.school_name
     *
     * @mbg.generated Thu Jan 10 15:00:21 CST 2019
     */
    private String schoolName;

    /**
     *
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column school.isv_id
     *
     * @mbg.generated Thu Jan 10 15:00:21 CST 2019
     */
    private Integer isvId;

    /**
     *
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column school.school_address
     *
     * @mbg.generated Thu Jan 10 15:00:21 CST 2019
     */
    private String schoolAddress;

    /**
     *
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column school.school_contact
     *
     * @mbg.generated Thu Jan 10 15:00:21 CST 2019
     */
    private String schoolContact;

    /**
     *
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column school.school_tel
     *
     * @mbg.generated Thu Jan 10 15:00:21 CST 2019
     */
    private String schoolTel;

    /**
     *
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column school.alipay_account
     *
     * @mbg.generated Thu Jan 10 15:00:21 CST 2019
     */
    private String alipayAccount;


    private String alipaySchoolPid;

    /**
     *
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column school.wechat_account_id
     *
     * @mbg.generated Thu Jan 10 15:00:21 CST 2019
     */
    private Integer schoolSystem;

    private String subMchId;
    private String alipayAppAuthToken;
    private String alipayAppRefreshToken;
    private String alipayAuthAppId;
    private String alipaySchoolNo;
    private Integer schoolType;
    private String provinceCode;
    private String provinceName;
    private String cityCode;
    private String cityName;
    private String districtCode;
    private String districtName;
    private Byte schoolStatus;

    /**
     * This method was generated by MyBatis Generator.
     * This method returns the value of the database column school.id
     *
     * @return the value of school.id
     *
     * @mbg.generated Thu Jan 10 15:00:21 CST 2019
     */
    public Integer getId() {
        return id;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method sets the value of the database column school.id
     *
     * @param id the value for school.id
     *
     * @mbg.generated Thu Jan 10 15:00:21 CST 2019
     */
    public void setId(Integer id) {
        this.id = id;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method returns the value of the database column school.school_name
     *
     * @return the value of school.school_name
     *
     * @mbg.generated Thu Jan 10 15:00:21 CST 2019
     */
    public String getSchoolName() {
        return schoolName;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method sets the value of the database column school.school_name
     *
     * @param schoolName the value for school.school_name
     *
     * @mbg.generated Thu Jan 10 15:00:21 CST 2019
     */
    public void setSchoolName(String schoolName) {
        this.schoolName = schoolName == null ? null : schoolName.trim();
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method returns the value of the database column school.isv_id
     *
     * @return the value of school.isv_id
     *
     * @mbg.generated Thu Jan 10 15:00:21 CST 2019
     */
    public Integer getIsvId() {
        return isvId;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method sets the value of the database column school.isv_id
     *
     * @param isvId the value for school.isv_id
     *
     * @mbg.generated Thu Jan 10 15:00:21 CST 2019
     */
    public void setIsvId(Integer isvId) {
        this.isvId = isvId;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method returns the value of the database column school.school_address
     *
     * @return the value of school.school_address
     *
     * @mbg.generated Thu Jan 10 15:00:21 CST 2019
     */
    public String getSchoolAddress() {
        return schoolAddress;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method sets the value of the database column school.school_address
     *
     * @param schoolAddress the value for school.school_address
     *
     * @mbg.generated Thu Jan 10 15:00:21 CST 2019
     */
    public void setSchoolAddress(String schoolAddress) {
        this.schoolAddress = schoolAddress == null ? null : schoolAddress.trim();
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method returns the value of the database column school.school_contact
     *
     * @return the value of school.school_contact
     *
     * @mbg.generated Thu Jan 10 15:00:21 CST 2019
     */
    public String getSchoolContact() {
        return schoolContact;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method sets the value of the database column school.school_contact
     *
     * @param schoolContact the value for school.school_contact
     *
     * @mbg.generated Thu Jan 10 15:00:21 CST 2019
     */
    public void setSchoolContact(String schoolContact) {
        this.schoolContact = schoolContact == null ? null : schoolContact.trim();
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method returns the value of the database column school.school_tel
     *
     * @return the value of school.school_tel
     *
     * @mbg.generated Thu Jan 10 15:00:21 CST 2019
     */
    public String getSchoolTel() {
        return schoolTel;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method sets the value of the database column school.school_tel
     *
     * @param schoolTel the value for school.school_tel
     *
     * @mbg.generated Thu Jan 10 15:00:21 CST 2019
     */
    public void setSchoolTel(String schoolTel) {
        this.schoolTel = schoolTel == null ? null : schoolTel.trim();
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method returns the value of the database column school.alipay_account
     *
     * @return the value of school.alipay_account
     *
     * @mbg.generated Thu Jan 10 15:00:21 CST 2019
     */
    public String getAlipayAccount() {
        return alipayAccount;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method sets the value of the database column school.alipay_account
     *
     * @param alipayAccount the value for school.alipay_account
     *
     * @mbg.generated Thu Jan 10 15:00:21 CST 2019
     */
    public void setAlipayAccount(String alipayAccount) {
        this.alipayAccount = alipayAccount == null ? null : alipayAccount.trim();
    }

    public String getAlipaySchoolPid() {
        return alipaySchoolPid;
    }

    public void setAlipaySchoolPid(String alipaySchoolPid) {
        this.alipaySchoolPid = alipaySchoolPid;
    }

    public Integer getSchoolSystem() {
        return schoolSystem;
    }

    public void setSchoolSystem(Integer schoolSystem) {
        this.schoolSystem = schoolSystem;
    }

    public String getSubMchId() {
        return subMchId;
    }

    public void setSubMchId(String subMchId) {
        this.subMchId = subMchId;
    }

    public String getAlipayAppAuthToken() {
        return alipayAppAuthToken;
    }

    public void setAlipayAppAuthToken(String alipayAppAuthToken) {
        this.alipayAppAuthToken = alipayAppAuthToken;
    }

    public String getAlipayAppRefreshToken() {
        return alipayAppRefreshToken;
    }

    public void setAlipayAppRefreshToken(String alipayAppRefreshToken) {
        this.alipayAppRefreshToken = alipayAppRefreshToken;
    }

    public String getAlipayAuthAppId() {
        return alipayAuthAppId;
    }

    public void setAlipayAuthAppId(String alipayAuthAppId) {
        this.alipayAuthAppId = alipayAuthAppId;
    }

    public String getAlipaySchoolNo() {
        return alipaySchoolNo;
    }

    public void setAlipaySchoolNo(String alipaySchoolNo) {
        this.alipaySchoolNo = alipaySchoolNo;
    }

    public Integer getSchoolType() {
        return schoolType;
    }

    public void setSchoolType(Integer schoolType) {
        this.schoolType = schoolType;
    }

    public String getProvinceCode() {
        return provinceCode;
    }

    public void setProvinceCode(String provinceCode) {
        this.provinceCode = provinceCode;
    }

    public String getProvinceName() {
        return provinceName;
    }

    public void setProvinceName(String provinceName) {
        this.provinceName = provinceName;
    }

    public String getCityCode() {
        return cityCode;
    }

    public void setCityCode(String cityCode) {
        this.cityCode = cityCode;
    }

    public String getCityName() {
        return cityName;
    }

    public void setCityName(String cityName) {
        this.cityName = cityName;
    }

    public String getDistrictCode() {
        return districtCode;
    }

    public void setDistrictCode(String districtCode) {
        this.districtCode = districtCode;
    }

    public String getDistrictName() {
        return districtName;
    }

    public void setDistrictName(String districtName) {
        this.districtName = districtName;
    }

    public Byte getSchoolStatus() {
        return schoolStatus;
    }

    public void setSchoolStatus(Byte schoolStatus) {
        this.schoolStatus = schoolStatus;
    }
}