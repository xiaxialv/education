package com.zaozao.service.model;

import org.joda.time.DateTime;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * @author Sidney 2019-01-11.
 */
public class AdminModel {
    private Integer id;
    @NotBlank(message = "账号名不能为空")
    private String accountName;
    @NotNull(message = "账号角色未选择")
    private Byte accountRole;
    @NotBlank(message = "公司名不能为空")
    private String companyName;

    private DateTime registerDate;

    @NotBlank(message = "密码不能为空")
    private String adminEncrptPassword;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public Byte getAccountRole() {
        return accountRole;
    }

    public void setAccountRole(Byte accountRole) {
        this.accountRole = accountRole;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public DateTime getRegisterDate() {
        return registerDate;
    }

    public void setRegisterDate(DateTime registerDate) {
        this.registerDate = registerDate;
    }

    public String getAdminEncrptPassword() {
        return adminEncrptPassword;
    }

    public void setAdminEncrptPassword(String adminEncrptPassword) {
        this.adminEncrptPassword = adminEncrptPassword;
    }

    @Override
    public String toString() {
        return "AdminModel{" + "id=" + id + ", accountName='" + accountName + '\'' + ", accountRole=" + accountRole
                + ", companyName='" + companyName + '\'' + ", registerDate=" + registerDate + ", adminEncrptPassword='"
                + adminEncrptPassword + '\'' + '}';
    }
}
