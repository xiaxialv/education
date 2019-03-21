package com.zaozao.controller.viewobject;

import java.util.List;

/**
 * @author Sidney 2019-01-11.
 */
public class AdminVO {
    private Integer id;
    private String accountName;
    private Byte accountRole;
    private String companyName;
    private String registerDate;
//    private Integer adminRoleId;


    private Integer isvId;
    private List<Integer> schoolId;

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

    public String getRegisterDate() {
        return registerDate;
    }

    public void setRegisterDate(String registerDate) {
        this.registerDate = registerDate;
    }

//    public Integer getAdminRoleId() {
//        return adminRoleId;
//    }
//
//    public void setAdminRoleId(Integer adminRoleId) {
//        this.adminRoleId = adminRoleId;
//    }
//

    public Integer getIsvId() {
        return isvId;
    }

    public void setIsvId(Integer isvId) {
        this.isvId = isvId;
    }

    public List<Integer> getSchoolId() {
        return schoolId;
    }

    public void setSchoolId(List<Integer> schoolId) {
        this.schoolId = schoolId;
    }

    @Override
    public String toString() {
        return "AdminVO{" + "id=" + id + ", accountName='" + accountName + '\'' + ", accountRole=" + accountRole
                + ", companyName='" + companyName + '\'' + ", registerDate='" + registerDate + '\'' + ", isvId=" + isvId
                + ", schoolId=" + schoolId + '}';
    }
}
