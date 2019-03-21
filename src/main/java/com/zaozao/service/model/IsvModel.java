package com.zaozao.service.model;

import javax.validation.constraints.NotBlank;

/**
 * @author Sidney 2019-01-14.
 */
public class IsvModel {
    private Integer id;

    @NotBlank(message = "公司名称不能为空")
    private String companyName;
    @NotBlank(message = "公司地址不能为空")
    private String companyAddress;
    @NotBlank(message = "公司联络人不能为空")
    private String companyContact;
    @NotBlank(message = "公司联络电话不能为空")
    private String companyTel;

    private Integer adminId;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getCompanyAddress() {
        return companyAddress;
    }

    public void setCompanyAddress(String companyAddress) {
        this.companyAddress = companyAddress;
    }

    public String getCompanyContact() {
        return companyContact;
    }

    public void setCompanyContact(String companyContact) {
        this.companyContact = companyContact;
    }

    public String getCompanyTel() {
        return companyTel;
    }

    public void setCompanyTel(String companyTel) {
        this.companyTel = companyTel;
    }

    public Integer getAdminId() {
        return adminId;
    }
    public void setAdminId(Integer adminId) {
        this.adminId = adminId;
    }
}
