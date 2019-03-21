package com.zaozao.controller;

import com.alibaba.fastjson.JSON;
import com.zaozao.Constant.AccountRoleConstant;
import com.zaozao.controller.viewobject.AdminVO;
import com.zaozao.dao.AdminPermissionDOMapper;
import com.zaozao.dataobject.AdminInfoDO;
import com.zaozao.dataobject.AdminPermissionDO;
import com.zaozao.error.BusinessException;
import com.zaozao.error.EmBusinessError;
import com.zaozao.response.CommonReturnType;
import com.zaozao.service.AdminPermissionService;
import com.zaozao.service.AdminService;
import com.zaozao.service.IsvService;
import com.zaozao.service.SchoolService;
import com.zaozao.service.model.AdminModel;
import com.zaozao.service.model.IsvModel;
import com.zaozao.service.model.SchoolModel;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;
import sun.misc.BASE64Encoder;

import javax.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Sidney 2018-12-23.
 * CrossOrigin,跨域请求处理
 */
@Controller("adminController")
@RequestMapping("/admin")
@CrossOrigin(allowCredentials = "true", allowedHeaders = "*")
public class AdminController extends BaseController {

    @Autowired
    private AdminService adminService;

    @Autowired
    private AdminPermissionService adminPermissionService;

    @Autowired
    private IsvService isvService;

    @Autowired
    private SchoolService schoolService;

    // httpServletRequest以bean的方式注入,说明这是一个单例模式
    // -->如何支持一个request支持多个用户的并发访问,此处包装后的本质是形成一个proxy,
    // 内部拥有ThreadLocal方式的map,让用户在每个线程中处理自己对应的request,并有threadLocal的清除方式

    @Autowired
    private HttpServletRequest httpServletRequest;

    @Autowired
    private AdminPermissionDOMapper adminPermissionDOMapper;

    // admin用户登录接口
    @RequestMapping(value = "/login", method = {RequestMethod.POST}, consumes = {CONTENT_TYPE_FORMED})
    @ResponseBody
    public CommonReturnType login(@RequestParam(name = "accountName") String accountName,
            @RequestParam(name = "password") String password) throws BusinessException, NoSuchAlgorithmException {
        //入参校验
        System.out.println("1入参校验开始"+System.currentTimeMillis());
        if (StringUtils.isEmpty(accountName) || StringUtils.isEmpty(password)) {
            throw new BusinessException(EmBusinessError.PARAM_VALIDATION_ERROR);
        }
        System.out.println("1入参校验结束"+System.currentTimeMillis());
        //用户登录服务,用来校验用户登录是否合法
        System.out.println("2用户名密码校验开始"+System.currentTimeMillis());
        AdminModel adminModel = adminService.validateLogin(accountName, this.encodingByMD5(password));
        System.out.println("2用户名密码校验结束"+System.currentTimeMillis());
        //将登陆凭证加入到用户登录成功的session内
        System.out.println("3登陆凭证加入到用户登录成功的session开始"+System.currentTimeMillis());
        AdminVO adminVO = this.convertVOFromModel(adminModel);
        this.httpServletRequest.getSession().setAttribute("IS_LOGIN", true);
        this.httpServletRequest.getSession().setAttribute("LOGIN_USER", JSON.toJSONString(adminVO));
        System.out.println("3登陆凭证加入到用户登录成功的session结束"+System.currentTimeMillis());
        System.out.println("4获取session中adminVO开始"+System.currentTimeMillis());
        AdminVO loginUser =
                JSON.parseObject((String) httpServletRequest.getSession().getAttribute("LOGIN_USER"), AdminVO.class);
        System.out.println("4获取session中adminVO结束"+System.currentTimeMillis());
        // Integer isvId = loginUser.getIsvId();
        //        List<Integer> schoolIdList = loginUser.getSchoolId();
        // 服务商登录后校验是否已添加isv记录,学校管理员及学校记录,如没有则在前端提示并转向指定页面
        //         && isvId != null
        System.out.println("5判断当前登录角色开始"+System.currentTimeMillis());
        if (AccountRoleConstant.ACCOUNT_ROLE_ISV == adminModel.getAccountRole()) {
            // isv登录后-->isv表查询是否有记录
            System.out.println("5-1当前登录角色-isv"+System.currentTimeMillis());
            int result = isvService.countIsvByAdminId(loginUser.getId());
            System.out.println("5-2当前登录角色-isv是否有isv记录"+System.currentTimeMillis());
            if (result == 0) {
                // 没有记录前端获取错误码进行跳转
                // TODO 登录跳转
                System.out.println("5-3当前登录角色-isv无isv记录"+System.currentTimeMillis());
                return CommonReturnType.create(adminVO,"60003");
//                throw new BusinessException(EmBusinessError.ISV_INFO_ERROR);
            } else {
                // 有记录将值存入
                System.out.println("5-4当前登录角色-isv有isv记录"+System.currentTimeMillis());
                System.out.println("5-5当前登录角色-isv将isv记录查出来"+System.currentTimeMillis());
                IsvModel isvByAdminId = isvService.getIsvByAdminId(loginUser.getId());
                System.out.println("5-6当前登录角色-isv将isvId存入session开始"+System.currentTimeMillis());
                // 前端拿到的登录对象是isvId和schoolId均为空
                assert adminVO != null;
                adminVO.setIsvId(isvByAdminId.getId());
                loginUser.setIsvId(isvByAdminId.getId());
                this.httpServletRequest.getSession().setAttribute("LOGIN_USER", JSON.toJSONString(loginUser));
                System.out.println("5-7当前登录角色-isv将isvId存入session结束"+System.currentTimeMillis());
                // 根据当前登录isv账号id查询其下属学校idList
                System.out.println("5-8当前登录角色-isv查询其下属学校idList开始"+System.currentTimeMillis());
                List<AdminPermissionDO> adminPermissionDOList = adminPermissionService.listByAdminId(adminModel.getId());
                System.out.println("5-9当前登录角色-isv查询其下属学校idList结束"+System.currentTimeMillis());
                // mark完善isv信息后前端立即跳转添加学校记录,如客户端导致跳转失败提示没有学校记录前端继续跳转/school/create
                if (CollectionUtils.isEmpty(adminPermissionDOList)) {
                    // TODO 登录跳转
                    System.out.println("5-10当前登录角色-isv无下属学校idList"+System.currentTimeMillis());
                    return CommonReturnType.create(adminVO,"60002");
//                    throw new BusinessException(EmBusinessError.SCHOOL_INFO_ERROR);
                }
                System.out.println("5-11当前登录角色-isv有下属学校idList,开始获取"+System.currentTimeMillis());
                List<Integer> schoolIdList =
                        adminPermissionDOList.stream().map(AdminPermissionDO::getSchoolId).collect(Collectors.toList());
                adminVO.setSchoolId(schoolIdList);
                loginUser.setSchoolId(schoolIdList);
                this.httpServletRequest.getSession().setAttribute("LOGIN_USER", JSON.toJSONString(loginUser));
                System.out.println("5-12当前登录角色-isv将其下属学校idList存入session"+System.currentTimeMillis());
            }
        } else if (AccountRoleConstant.ACCOUNT_ROLE_SCHOOL == adminModel.getAccountRole()
                || AccountRoleConstant.ACCOUNT_ROLE_SCHOOL_DEAN == adminModel.getAccountRole()
                || AccountRoleConstant.ACCOUNT_ROLE_SCHOOL_FINACIAL == adminModel.getAccountRole()) {
            // 根据当前登录学校账号id查询其关联的schoolId上属isv
            System.out.println("6-1当前登录角色-school"+System.currentTimeMillis());
            List<AdminPermissionDO> adminPermissionDOList = adminPermissionService.listByAdminId(adminModel.getId());
            System.out.println("6-2当前登录角色-school查询其schoolId"+System.currentTimeMillis());
            //            if (CollectionUtils.isEmpty(adminPermissionDOList)) {
            ////                throw new BusinessException(EmBusinessError.SCHOOL_INFO_ERROR.setErrMsg("请联系您的isv服务商添加关于您学校的记录"));
            //            }
            Integer schoolId = adminPermissionDOList.get(0).getSchoolId();
            System.out.println("6-3当前登录角色-school查询其school记录"+System.currentTimeMillis());
            SchoolModel schoolById = schoolService.getSchoolById(schoolId);
            System.out.println("6-4当前登录角色-session存入其isvId"+System.currentTimeMillis());
            loginUser.setIsvId(schoolById.getIsvId());
            System.out.println("6-4当前登录角色-session存入其schoolIdList"+System.currentTimeMillis());
            List<Integer> schoolIdList = new ArrayList<>();
            schoolIdList.add(schoolId);
            loginUser.setSchoolId(schoolIdList);
            this.httpServletRequest.getSession().setAttribute("LOGIN_USER", JSON.toJSONString(loginUser));
            System.out.println("6-4当前登录角色-session存入其schoolIdList结束"+System.currentTimeMillis());
        }
        return CommonReturnType.create(adminVO);
    }

    // admin用户登出接口
    @RequestMapping(value = "/logout", method = {RequestMethod.GET})
    @ResponseBody
    public CommonReturnType loginOut() {
        // 将登陆凭证session中清除
        this.httpServletRequest.getSession().setAttribute("IS_LOGIN", false);
        this.httpServletRequest.getSession().setAttribute("LOGIN_USER", null);
        return CommonReturnType.create(null);
    }

    // 新增管理员用户
    @RequestMapping(value = "/create", method = {RequestMethod.POST}, consumes = {CONTENT_TYPE_FORMED})
    @ResponseBody
    public CommonReturnType create(@RequestParam(name = "accountName") String accountName,
            @RequestParam(name = "accountRole") Integer accountRole,
            @RequestParam(name = "companyName") String companyName,
            @RequestParam(name = "adminEncrptPassword1") String adminEncrptPassword1,
            @RequestParam(name = "adminEncrptPassword2") String adminEncrptPassword2,
            @RequestParam(name = "schoolId", required = false) Integer schoolId)
            throws BusinessException, NoSuchAlgorithmException {
        Boolean isLogin = (Boolean) httpServletRequest.getSession().getAttribute("IS_LOGIN");
        AdminVO loginUser =
                JSON.parseObject((String) httpServletRequest.getSession().getAttribute("LOGIN_USER"), AdminVO.class);
        if (isLogin == null || !isLogin) {
            throw new BusinessException(EmBusinessError.USER_NOT_LOGIN);
        } else if (!StringUtils.equals(adminEncrptPassword1, adminEncrptPassword2)) {
            throw new BusinessException(EmBusinessError.USER_PASSWORD_DIFFERENT);
        }
        // 用户的创建流程
        AdminModel adminModel = new AdminModel();
        adminModel.setAccountName(accountName);
        adminModel.setAccountRole(new Byte(String.valueOf(accountRole)));
        adminModel.setCompanyName(companyName);
        adminModel.setAdminEncrptPassword(this.encodingByMD5(adminEncrptPassword1));
        adminModel.setRegisterDate(new DateTime());
        AdminVO adminVO;
        if (accountRole <= loginUser.getAccountRole()) {
            throw new BusinessException(EmBusinessError.ACCESS_VIOLATION_CLASS_ERROR);
        } else if (AccountRoleConstant.ACCOUNT_ROLE_SUPER_ADMIN == loginUser.getAccountRole()) {
            // 超级管理员创建其下属isv服务商
            AdminModel adminModelReturn = adminService.createAdmin(adminModel);
            adminVO = this.convertVOFromModel(adminModelReturn);
        } else if (AccountRoleConstant.ACCOUNT_ROLE_ISV == loginUser.getAccountRole()) {
            // isv服务商登录创建其下属学校的管理员,并将学校管理员id与该学校id相关联permission
            // 保证学校记录已经被创建,如果未创建此时新建学校管理员报错
            if (schoolId == null) {
                throw new BusinessException(EmBusinessError.SCHOOL_INFO_ERROR);
            }
            AdminModel adminModelReturn = adminService.createAdmin(adminModel);
            adminVO = this.convertVOFromModel(adminModelReturn);
            AdminPermissionDO adminPermissionDO = new AdminPermissionDO();
            adminPermissionDO.setAdminId(adminVO.getId());
            adminPermissionDO.setSchoolId(schoolId);
            adminPermissionDOMapper.insertSelective(adminPermissionDO);
        } else if (AccountRoleConstant.ACCOUNT_ROLE_SCHOOL == loginUser.getAccountRole()) {
            AdminModel adminModelReturn = adminService.createAdmin(adminModel);
            // isv学校管理员登录创建其下属学校教务/财务管理员
            adminVO = this.convertVOFromModel(adminModelReturn);
            // 获取学校下属管理员id与该学校id相关联permission
            List<AdminPermissionDO> adminPermissionDOList = adminPermissionDOMapper.selectByAdminId(loginUser.getId());
            AdminPermissionDO adminPermissionDO = new AdminPermissionDO();
            adminPermissionDO.setAdminId(adminVO.getId());
            adminPermissionDO.setSchoolId(adminPermissionDOList.get(0).getSchoolId());
            adminPermissionDOMapper.insertSelective(adminPermissionDO);
        } else {
            throw new BusinessException(EmBusinessError.ACCESS_VIOLATION_ERROR);
        }
        return CommonReturnType.create(adminVO);
    }

    /**
     * 获取所有admin用户接口
     * Model model, @RequestParam(defaultValue = "1", value = "pageNum") Integer pageNum
     */
    @RequestMapping(value = "/list", method = {RequestMethod.GET})
    @ResponseBody
    public CommonReturnType listAdmin() throws BusinessException {
        Boolean isLogin = (Boolean) httpServletRequest.getSession().getAttribute("IS_LOGIN");
        AdminVO loginUser =
                JSON.parseObject((String) httpServletRequest.getSession().getAttribute("LOGIN_USER"), AdminVO.class);
        List<Byte> roleList = new ArrayList<>();
        List<AdminModel> adminModelList;
        //PageHelper.startPage(pageNum, PageSizeConstant.PAGE_SIZE_ADMIN);
        if (isLogin == null || !isLogin) {
            throw new BusinessException(EmBusinessError.USER_NOT_LOGIN);
        } else if (AccountRoleConstant.ACCOUNT_ROLE_SUPER_ADMIN == loginUser.getAccountRole()) {
            // 超级管理员查看所有管理员
            adminModelList = adminService.listAdmin();
        } else if (AccountRoleConstant.ACCOUNT_ROLE_ISV == loginUser.getAccountRole()) {
            // isv服务商只显示自己以及下属学校的总管理员
            // VO获得自己的下属学校idList,permission中查出adminIdList—admin表中role=1and2 adminId in adminIdList
            List<Integer> schoolIdList = loginUser.getSchoolId();
            List<AdminPermissionDO> adminPermissionDOList =
                    adminPermissionDOMapper.listPermissionByGivenSchoolIdList(schoolIdList);
            List<Integer> adminIdList =
                    adminPermissionDOList.stream().map(AdminPermissionDO::getAdminId).collect(Collectors.toList());
            roleList.add(AccountRoleConstant.ACCOUNT_ROLE_ISV);
            roleList.add(AccountRoleConstant.ACCOUNT_ROLE_SCHOOL);
            adminModelList = adminService.searchByRoleListAndIdList(roleList, adminIdList);
        } else if (AccountRoleConstant.ACCOUNT_ROLE_SCHOOL == loginUser.getAccountRole()
                || loginUser.getAccountRole() == AccountRoleConstant.ACCOUNT_ROLE_SCHOOL_FINACIAL
                || loginUser.getAccountRole() == AccountRoleConstant.ACCOUNT_ROLE_SCHOOL_DEAN) {
            List<Integer> schoolIdList = loginUser.getSchoolId();
            Integer schoolId = schoolIdList.get(0);
            List<AdminPermissionDO> adminPermissionDOList = adminPermissionDOMapper.selectBySchoolId(schoolId);
            List<Integer> adminIdList =
                    adminPermissionDOList.stream().map(AdminPermissionDO::getAdminId).collect(Collectors.toList());
            roleList.add(AccountRoleConstant.ACCOUNT_ROLE_SCHOOL);
            roleList.add(AccountRoleConstant.ACCOUNT_ROLE_SCHOOL_FINACIAL);
            roleList.add(AccountRoleConstant.ACCOUNT_ROLE_SCHOOL_DEAN);
            adminModelList = adminService.searchByRoleListAndIdList(roleList, adminIdList);
        } else {
            throw new BusinessException(EmBusinessError.ACCESS_VIOLATION_ERROR);
        }
        List<AdminVO> adminVOList = adminModelList.stream().map(this::convertVOFromModel).collect(Collectors.toList());
        //PageInfo<AdminVO> pageInfo = new PageInfo<>(adminVOList);
        //model.addAttribute("pageInfo", pageInfo);
        return CommonReturnType.create(adminVOList);
    }

    /**
     * 删除管理员,只校验上级可以删除下级,并删除permission表中对应关系
     *
     * @param adminId 管理员id
     * @return null
     */
    @RequestMapping(value = "/delete", method = {RequestMethod.POST}, consumes = {CONTENT_TYPE_FORMED})
    @ResponseBody
    public CommonReturnType deleteById(@RequestParam(name = "adminId") Integer adminId) throws BusinessException {
        Boolean isLogin = (Boolean) httpServletRequest.getSession().getAttribute("IS_LOGIN");
        AdminVO loginUser =
                JSON.parseObject((String) httpServletRequest.getSession().getAttribute("LOGIN_USER"), AdminVO.class);
        AdminInfoDO admin = adminService.getAdmin(adminId);
        if (isLogin == null || !isLogin) {
            throw new BusinessException(EmBusinessError.USER_NOT_LOGIN);
        } else if (admin.getAccountRole() <= loginUser.getAccountRole()) {
            throw new BusinessException(EmBusinessError.ACCESS_VIOLATION_CLASS_ERROR);
        }
        adminService.deleteAdminById(adminId);
        return CommonReturnType.create(null);
    }

    @RequestMapping(value = "/update", method = {RequestMethod.POST}, consumes = {CONTENT_TYPE_FORMED})
    @ResponseBody
    public CommonReturnType updateById(@RequestParam(name = "adminId") Integer adminId,
            @RequestParam(name = "accountName") String accountName,
            @RequestParam(name = "accountRole") Integer accountRole,
            @RequestParam(name = "companyName") String companyName,
            @RequestParam(name = "adminEncrptPassword1") String adminEncrptPassword1,
            @RequestParam(name = "adminEncrptPassword2") String adminEncrptPassword2)
            throws BusinessException, NoSuchAlgorithmException {
        Boolean isLogin = (Boolean) httpServletRequest.getSession().getAttribute("IS_LOGIN");
        AdminVO loginUser =
                JSON.parseObject((String) httpServletRequest.getSession().getAttribute("LOGIN_USER"), AdminVO.class);
        // 获取传入admin对象
        AdminInfoDO admin = adminService.getAdmin(adminId);
        AdminModel adminModel = new AdminModel();
        adminModel.setId(adminId);
        adminModel.setAccountName(accountName);
        adminModel.setAccountRole(new Byte(String.valueOf(accountRole)));
        adminModel.setCompanyName(companyName);
        adminModel.setAdminEncrptPassword(this.encodingByMD5(adminEncrptPassword1));
        List<Byte> accountRoleList = new ArrayList<>();
        List<AdminModel> adminModelList;
        if (isLogin == null || !isLogin) {
            throw new BusinessException(EmBusinessError.USER_NOT_LOGIN);
        } else if (!StringUtils.equals(adminEncrptPassword1, adminEncrptPassword2)) {
            throw new BusinessException(EmBusinessError.USER_PASSWORD_DIFFERENT);
        } else if (admin.getAccountRole() <= loginUser.getAccountRole()) {
            throw new BusinessException(EmBusinessError.ACCESS_VIOLATION_CLASS_ERROR);
        } else if (AccountRoleConstant.ACCOUNT_ROLE_SUPER_ADMIN == loginUser.getAccountRole()) {
            adminService.updateAdmin(adminModel);
        } else if (AccountRoleConstant.ACCOUNT_ROLE_ISV == loginUser.getAccountRole()) {
            List<Integer> schoolIdList = loginUser.getSchoolId();
            if (CollectionUtils.isEmpty(schoolIdList)) {
                throw new BusinessException(EmBusinessError.SCHOOL_INFO_ERROR);
            } else {
                // 获取permission中指定schoolId的所有对象并取出adminIdList(含服务商及学校管理员)
                List<AdminPermissionDO> adminPermissionDOList =
                        adminPermissionDOMapper.listPermissionByGivenSchoolIdList(schoolIdList);
                List<Integer> adminIdList =
                        adminPermissionDOList.stream().map(AdminPermissionDO::getAdminId).collect(Collectors.toList());
                accountRoleList.add(AccountRoleConstant.ACCOUNT_ROLE_SCHOOL);
                // 从admin中查询出属于isv的学校管理员对象取得adminIds
                adminModelList = adminService.searchByRoleListAndIdList(accountRoleList, adminIdList);
                List<Integer> adminIds = adminModelList.stream().map(AdminModel::getId).collect(Collectors.toList());
                // 判读传入的adminId是否是自己下属的学校总管理员
                if (CollectionUtils.isEmpty(adminIds)) {
                    throw new BusinessException(EmBusinessError.SCHOOL_ADMIN_ERROR);
                } else if (!adminIds.contains(adminId)) {
                    throw new BusinessException(EmBusinessError.ACCESS_VIOLATION_ERROR);
                } else {
                    adminService.updateAdmin(adminModel);
                }
            }
        } else if (AccountRoleConstant.ACCOUNT_ROLE_SCHOOL == loginUser.getAccountRole()
                || loginUser.getAccountRole() == AccountRoleConstant.ACCOUNT_ROLE_SCHOOL_FINACIAL
                || loginUser.getAccountRole() == AccountRoleConstant.ACCOUNT_ROLE_SCHOOL_DEAN) {
            List<Integer> schoolIdList = loginUser.getSchoolId();
            if (CollectionUtils.isEmpty(schoolIdList)) {
                throw new BusinessException(EmBusinessError.SCHOOL_INFO_ERROR);
            } else {
                // 获取permission中指定schoolId的所有对象并取出adminIdList(含服务商及学校管理员)
                List<AdminPermissionDO> adminPermissionDOList =
                        adminPermissionDOMapper.listPermissionByGivenSchoolIdList(schoolIdList);
                List<Integer> adminIdList =
                        adminPermissionDOList.stream().map(AdminPermissionDO::getAdminId).collect(Collectors.toList());
                accountRoleList.add(AccountRoleConstant.ACCOUNT_ROLE_SCHOOL_FINACIAL);
                accountRoleList.add(AccountRoleConstant.ACCOUNT_ROLE_SCHOOL_DEAN);
                // 从admin中查询出属于学校的下属管理员对象取得adminIds
                adminModelList = adminService.searchByRoleListAndIdList(accountRoleList, adminIdList);
                List<Integer> adminIds = adminModelList.stream().map(AdminModel::getId).collect(Collectors.toList());
                // 判读传入的adminId是否是自己下属的学校管理员
                if (CollectionUtils.isEmpty(adminIds)) {
                    throw new BusinessException(EmBusinessError.SCHOOL_ADMIN_ERROR.setErrMsg("您还没有创建下属学校管理员"));
                } else if (!adminIds.contains(adminId)) {
                    throw new BusinessException(EmBusinessError.ACCESS_VIOLATION_ERROR);
                } else {
                    adminService.updateAdmin(adminModel);
                }
            }
        }
        return CommonReturnType.create(null);
    }

    //密码加密处理
    private String encodingByMD5(String str) throws NoSuchAlgorithmException {
        //确定计算方法
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        BASE64Encoder base64en = new BASE64Encoder();
        //加密字符串
        return base64en.encode(md5.digest(str.getBytes(StandardCharsets.UTF_8)));
    }

    private AdminVO convertVOFromModel(AdminModel adminModel) {
        if (adminModel == null) {
            return null;
        }
        AdminVO adminVO = new AdminVO();
        BeanUtils.copyProperties(adminModel, adminVO);
        adminVO.setRegisterDate(
                adminModel.getRegisterDate().toString(DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss")));
        return adminVO;
    }

    //    private AdminVO convertVOFromDO(AdminInfoDO adminInfoDO) {
    //        if (adminInfoDO == null) {
    //            return null;
    //        }
    //        AdminVO adminVO = new AdminVO();
    //        BeanUtils.copyProperties(adminInfoDO, adminVO);
    //        adminVO.setRegisterDate(
    //                adminInfoDO.getRegisterDate());
    //        return adminVO;
    //    }


}
