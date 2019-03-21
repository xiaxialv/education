package com.zaozao.controller;

import com.alibaba.fastjson.JSON;
import com.zaozao.Constant.AccountRoleConstant;
import com.zaozao.Constant.DeleteStatusConstant;
import com.zaozao.Constant.SchoolConstant;
import com.zaozao.controller.viewobject.AdminVO;
import com.zaozao.controller.viewobject.SchoolGradeClassVO;
import com.zaozao.controller.viewobject.SchoolVO;
import com.zaozao.controller.viewobject.SubMchIdVO;
import com.zaozao.dao.AdminPermissionDOMapper;
import com.zaozao.dao.BillItemDOMapper;
import com.zaozao.dataobject.AdminPermissionDO;
import com.zaozao.dataobject.BillItemDO;
import com.zaozao.error.BusinessException;
import com.zaozao.error.EmBusinessError;
import com.zaozao.response.CommonReturnType;
import com.zaozao.service.ClassService;
import com.zaozao.service.IsvService;
import com.zaozao.service.SchoolService;
import com.zaozao.service.model.ClassModel;
import com.zaozao.service.model.IsvModel;
import com.zaozao.service.model.SchoolModel;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Sidney 2019-01-10.
 * 所有接口的微信子商户号都需要管理员才能添加
 * 和支付宝Id?做到支付宝支付再看
 */
@Controller("schoolController")
@RequestMapping("/school")
@CrossOrigin(allowCredentials = "true", origins = "*")
public class SchoolController extends BaseController {
    @Autowired
    private SchoolService schoolService;

    @Autowired
    private ClassService classService;

    @Autowired
    private HttpServletRequest httpServletRequest;

    @Autowired
    private AdminPermissionDOMapper adminPermissionDOMapper;

    @Autowired
    private IsvService isvService;

    @Autowired
    private BillItemDOMapper billItemDOMapper;


    @RequestMapping(value = "/class/info", method = {RequestMethod.GET})
    @ResponseBody
    public CommonReturnType getSchoolAndClass() throws Exception {
        // 获取学校list
        List<SchoolModel> schoolModelList = schoolService.listSchool();
        List<SchoolGradeClassVO> schoolGradeClassVOList = this.getSchoolGradeClassVO(schoolModelList);
        return CommonReturnType.create(schoolGradeClassVOList);
    }

    /**
     * @return 获取当前用户可以查看到的所有的学校的信息
     * @throws Exception
     */
    @RequestMapping(value = "/info/dynamic", method = {RequestMethod.GET})
    @ResponseBody
    public CommonReturnType getSchoolGradeClass() throws Exception {
        Boolean isLogin = (Boolean) httpServletRequest.getSession().getAttribute("IS_LOGIN");
        AdminVO loginUser =
                JSON.parseObject((String) httpServletRequest.getSession().getAttribute("LOGIN_USER"), AdminVO.class);
        List<SchoolGradeClassVO> schoolGradeClassVOList;
        List<SchoolModel> schoolModelList;
        //PageHelper.startPage(pageNum, PageSizeConstant.PAGE_SIZE_SCHOOL);
        if (isLogin == null || !isLogin) {
            throw new BusinessException(EmBusinessError.USER_NOT_LOGIN);
        } else if (loginUser.getAccountRole() == AccountRoleConstant.ACCOUNT_ROLE_ISV) {
            // 获取当前用户可以查看到的所有的学校的信息
            List<Integer> schoolIdList = loginUser.getSchoolId();
            if (CollectionUtils.isEmpty(schoolIdList)) {
                throw new BusinessException(EmBusinessError.SCHOOL_INFO_ERROR);
            } else {
                schoolModelList = schoolService.listSchoolByGivenIdList(schoolIdList);
                schoolGradeClassVOList = this.getSchoolGradeClassVO(schoolModelList);
            }
        } else if (loginUser.getAccountRole() == AccountRoleConstant.ACCOUNT_ROLE_SUPER_ADMIN) {
            schoolModelList = schoolService.listSchool();
            schoolGradeClassVOList = this.getSchoolGradeClassVO(schoolModelList);
        } else if (loginUser.getAccountRole() == AccountRoleConstant.ACCOUNT_ROLE_SCHOOL
                || loginUser.getAccountRole() == AccountRoleConstant.ACCOUNT_ROLE_SCHOOL_FINACIAL
                || loginUser.getAccountRole() == AccountRoleConstant.ACCOUNT_ROLE_SCHOOL_DEAN) {
            List<Integer> schoolIdList = loginUser.getSchoolId();
            if (CollectionUtils.isEmpty(schoolIdList)) {
                throw new BusinessException(EmBusinessError.SCHOOL_INFO_ERROR);
            } else {
                Integer schoolId = schoolIdList.get(0);
                SchoolModel schoolModel = schoolService.getSchoolById(schoolId);
                if (schoolModel != null) {
                    schoolModelList = new ArrayList<>();
                    schoolModelList.add(schoolModel);
                    schoolGradeClassVOList = this.getSchoolGradeClassVO(schoolModelList);
                } else {
                    throw new BusinessException(EmBusinessError.SCHOOL_INFO_ERROR);
                }
            }
        } else {
            throw new BusinessException(EmBusinessError.ACCESS_VIOLATION_ERROR);
        }
        return CommonReturnType.create(schoolGradeClassVOList);
    }

    /**
     * Model model,@RequestParam(defaultValue = "1", value = "pageNum") Integer pageNum
     */
    @RequestMapping(value = "/get", method = {RequestMethod.GET})
    @ResponseBody
    public CommonReturnType getSchoolList() throws Exception {
        Boolean isLogin = (Boolean) httpServletRequest.getSession().getAttribute("IS_LOGIN");
        AdminVO loginUser =
                JSON.parseObject((String) httpServletRequest.getSession().getAttribute("LOGIN_USER"), AdminVO.class);
        List<SchoolVO> schoolVOList;
        //PageHelper.startPage(pageNum, PageSizeConstant.PAGE_SIZE_SCHOOL);
        if (isLogin == null || !isLogin) {
            throw new BusinessException(EmBusinessError.USER_NOT_LOGIN);
        } else if (loginUser.getAccountRole() == AccountRoleConstant.ACCOUNT_ROLE_ISV) {
            // 获取当前用户可以查看到的所有的学校的信息
            List<Integer> schoolIdList = loginUser.getSchoolId();
            if (CollectionUtils.isEmpty(schoolIdList)) {
                throw new BusinessException(EmBusinessError.SCHOOL_INFO_ERROR);
            } else {
                List<SchoolModel> schoolModelList = schoolService.listSchoolByGivenIdList(schoolIdList);
                schoolVOList = schoolModelList.stream().map(this::convertVOFromModel).collect(Collectors.toList());
            }
        } else if (loginUser.getAccountRole() == AccountRoleConstant.ACCOUNT_ROLE_SUPER_ADMIN) {
            List<SchoolModel> schoolModelList = schoolService.listSchool();
            schoolVOList = schoolModelList.stream().map(this::convertVOFromModel).collect(Collectors.toList());
        } else if (loginUser.getAccountRole() == AccountRoleConstant.ACCOUNT_ROLE_SCHOOL
                || loginUser.getAccountRole() == AccountRoleConstant.ACCOUNT_ROLE_SCHOOL_FINACIAL
                || loginUser.getAccountRole() == AccountRoleConstant.ACCOUNT_ROLE_SCHOOL_DEAN) {
            List<Integer> schoolIdList = loginUser.getSchoolId();
            if (CollectionUtils.isEmpty(schoolIdList)) {
                throw new BusinessException(EmBusinessError.SCHOOL_INFO_ERROR);
            } else {
                Integer schoolId = schoolIdList.get(0);
                SchoolModel schoolModel = schoolService.getSchoolById(schoolId);
                SchoolVO schoolVO = this.convertVOFromModel(schoolModel);
                return CommonReturnType.create(schoolVO);
            }
        } else {
            throw new BusinessException(EmBusinessError.ACCESS_VIOLATION_ERROR);
        }
        return CommonReturnType.create(schoolVOList);
    }

    @RequestMapping(value = "/create", method = {RequestMethod.POST}, consumes = {CONTENT_TYPE_FORMED})
    @ResponseBody
    @Transactional(rollbackFor = Exception.class)
    public CommonReturnType createSchool(@RequestParam("schoolName") String schoolName,
            @RequestParam("schoolAddress") String schoolAddress, @RequestParam("schoolContact") String schoolContact,
            @RequestParam("schoolTel") String schoolTel, @RequestParam("alipayAccount") String alipayAccount,
            @RequestParam("schoolSystem") Integer schoolSystem,@RequestParam("schoolType") Integer schoolType, @RequestParam("provinceCodeName") String provinceCodeName,
            @RequestParam("cityCodeName") String cityCodeName,@RequestParam("districtCodeName") String districtCodeName) throws Exception {
        Boolean isLogin = (Boolean) httpServletRequest.getSession().getAttribute("IS_LOGIN");
        AdminVO loginUser =
                JSON.parseObject((String) httpServletRequest.getSession().getAttribute("LOGIN_USER"), AdminVO.class);
        if (isLogin == null || !isLogin) {
            throw new BusinessException(EmBusinessError.USER_NOT_LOGIN);
        } else if (AccountRoleConstant.ACCOUNT_ROLE_ISV == loginUser.getAccountRole()) {
            System.out.println("isv记录新增学校,当前登录用户,loginUser="+loginUser);
            Integer isvId = loginUser.getIsvId();
            if (isvId == null) {
                throw new BusinessException(EmBusinessError.ISV_INFO_ERROR);
            } else {
                SchoolModel schoolModel = new SchoolModel();
                schoolModel.setIsvId(isvId);
                schoolModel.setSchoolName(schoolName);
                schoolModel.setSchoolAddress(schoolAddress);
                schoolModel.setSchoolContact(schoolContact);
                schoolModel.setSchoolTel(schoolTel);
                schoolModel.setAlipayAccount(alipayAccount);
                schoolModel.setSchoolSystem(schoolSystem);
                schoolModel.setSchoolType(schoolType);
                List<String> province = this.splitCodeName(provinceCodeName);
                schoolModel.setProvinceCode(province.get(0));
                schoolModel.setProvinceName(province.get(1));
                List<String> city = this.splitCodeName(cityCodeName);
                schoolModel.setCityCode(city.get(0));
                schoolModel.setCityName(city.get(1));
                List<String> district = this.splitCodeName(districtCodeName);
                schoolModel.setDistrictCode(district.get(0));
                schoolModel.setDistrictName(district.get(1));
                schoolModel.setSchoolStatus(SchoolConstant.SCHOOL_SEND_UNSEND);
                SchoolModel schoolModelReturn = schoolService.createSchool(schoolModel);
                SchoolVO schoolVO = this.convertVOFromModel(schoolModelReturn);
                System.out.println("model->VO,schoolId="+schoolVO.getId());
                // 将isv服务商账号id与新建学校记录id关联permission
                System.out.println("当前登录,loginUser="+loginUser.toString());
                AdminPermissionDO adminPermissionDO = new AdminPermissionDO();
                adminPermissionDO.setAdminId(loginUser.getId());
                adminPermissionDO.setSchoolId(schoolVO.getId());
                adminPermissionDOMapper.insertSelective(adminPermissionDO);
                // 将新增的schoolId加入session中原有的schoolIdList中去并覆盖session
                List<Integer> schoolIdList = loginUser.getSchoolId();
                if (CollectionUtils.isEmpty(schoolIdList)) {
                    schoolIdList = new ArrayList<>();
                    schoolIdList.add(schoolVO.getId());
                    loginUser.setSchoolId(schoolIdList);
                    System.out.println("当前首次添加学校,loginUser="+loginUser.toString());
                    this.httpServletRequest.getSession().setAttribute("LOGIN_USER", JSON.toJSONString(loginUser));
                    return CommonReturnType.create("首次新增学校记录成功");
                } else {
                    schoolIdList.add(schoolVO.getId());
                    loginUser.setSchoolId(schoolIdList);
                    System.out.println("当前非首次添加学校,loginUser="+loginUser.toString());
                    this.httpServletRequest.getSession().setAttribute("LOGIN_USER", JSON.toJSONString(loginUser));
                    return CommonReturnType.create("新增学校记录成功");
                }
            }
        } else {
            throw new BusinessException(EmBusinessError.ACCESS_VIOLATION_ERROR);
        }
    }

    /**
     * isv为该学校申请好作为子商户后添加子商户号
     * 支付宝相关token参数由学校扫码授权后返回到回调接口,收到后再做记录;学校在支付宝中唯一编码由isv发送学校信息后获得再存入数据库
     *
     * @param schoolId 学校id
     * @param subMchId 学校子商户号
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/add/mch", method = {RequestMethod.POST}, consumes = {CONTENT_TYPE_FORMED})
    @ResponseBody
    @Transactional(rollbackFor = Exception.class)
    public CommonReturnType addSubMchId(@RequestParam("id") Integer schoolId, @RequestParam("subMchId") String subMchId)
            throws Exception {
        Boolean isLogin = (Boolean) httpServletRequest.getSession().getAttribute("IS_LOGIN");
        AdminVO loginUser =
                JSON.parseObject((String) httpServletRequest.getSession().getAttribute("LOGIN_USER"), AdminVO.class);
        if (isLogin == null || !isLogin) {
            throw new BusinessException(EmBusinessError.USER_NOT_LOGIN);
        } else if (loginUser.getAccountRole() != AccountRoleConstant.ACCOUNT_ROLE_ISV) {
            throw new BusinessException(EmBusinessError.ACCESS_VIOLATION_ERROR);
        } else {
            List<Integer> schoolIdList = loginUser.getSchoolId();
            if (CollectionUtils.isEmpty(schoolIdList)) {
                throw new BusinessException(EmBusinessError.SCHOOL_INFO_ERROR);
            } else if (!schoolIdList.contains(schoolId)) {
                throw new BusinessException(EmBusinessError.ACCESS_VIOLATION_ERROR);
            } else {
                // 为该学校添加微信配置
                SchoolModel schoolModel = new SchoolModel();
                schoolModel.setId(schoolId);
                schoolModel.setSubMchId(subMchId);
                schoolService.updateSchool(schoolModel);
            }
        }
        return CommonReturnType.create(null);
    }

    /**
     * 查看子商户号
     * @param schoolId 学校id
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/search/mch", method = {RequestMethod.POST}, consumes = {CONTENT_TYPE_FORMED})
    @ResponseBody
    @Transactional(rollbackFor = Exception.class)
    public CommonReturnType searchSubMchId(@RequestParam("id") Integer schoolId) throws Exception {
        Boolean isLogin = (Boolean) httpServletRequest.getSession().getAttribute("IS_LOGIN");
        AdminVO loginUser =
                JSON.parseObject((String) httpServletRequest.getSession().getAttribute("LOGIN_USER"), AdminVO.class);
        SubMchIdVO subMchIdVO = new SubMchIdVO();
        if (isLogin == null || !isLogin) {
            throw new BusinessException(EmBusinessError.USER_NOT_LOGIN);
        } else if (loginUser.getAccountRole() != AccountRoleConstant.ACCOUNT_ROLE_SUPER_ADMIN) {
            SchoolModel schoolById = schoolService.getSchoolById(schoolId);
            String subMchId = schoolById.getSubMchId();
            if (subMchId != null) {
                subMchIdVO.setSubMchId(subMchId);
            }
        } else if (loginUser.getAccountRole() == AccountRoleConstant.ACCOUNT_ROLE_ISV) {
            List<Integer> schoolIdList = loginUser.getSchoolId();
            if (CollectionUtils.isEmpty(schoolIdList)) {
                throw new BusinessException(EmBusinessError.SCHOOL_INFO_ERROR);
            } else if (!schoolIdList.contains(schoolId)) {
                throw new BusinessException(EmBusinessError.ACCESS_VIOLATION_ERROR);
            } else {
                SchoolModel schoolById = schoolService.getSchoolById(schoolId);
                String subMchId = schoolById.getSubMchId();
                if (subMchId != null) {
                    subMchIdVO.setSubMchId(subMchId);
                }
            }
        } else if (loginUser.getAccountRole() == AccountRoleConstant.ACCOUNT_ROLE_SCHOOL
                || loginUser.getAccountRole() == AccountRoleConstant.ACCOUNT_ROLE_SCHOOL_FINACIAL
                || loginUser.getAccountRole() == AccountRoleConstant.ACCOUNT_ROLE_SCHOOL_DEAN) {
            List<Integer> schoolIdList = loginUser.getSchoolId();
            if (CollectionUtils.isEmpty(schoolIdList)) {
                throw new BusinessException(EmBusinessError.SCHOOL_INFO_ERROR);
            } else if (!schoolIdList.contains(schoolId)) {
                throw new BusinessException(EmBusinessError.SCHOOL_INFO_ERROR);
            } else {
                SchoolModel schoolById = schoolService.getSchoolById(schoolId);
                String subMchId = schoolById.getSubMchId();
                if (subMchId != null) {
                    subMchIdVO.setSubMchId(subMchId);
                }
            }
        } else {
            throw new BusinessException(EmBusinessError.ACCESS_VIOLATION_ERROR);
        }
        return CommonReturnType.create(subMchIdVO);
    }


    @RequestMapping(value = "/update", method = {RequestMethod.POST}, consumes = {CONTENT_TYPE_FORMED})
    @ResponseBody
    public CommonReturnType updateSchool(@RequestParam("id") Integer id, @RequestParam("schoolName") String schoolName,
            @RequestParam("schoolAddress") String schoolAddress, @RequestParam("schoolContact") String schoolContact,
            @RequestParam("schoolTel") String schoolTel, @RequestParam("alipayAccount") String alipayAccount,
            @RequestParam("schoolSystem") Integer schoolSystem,@RequestParam("schoolType") Integer schoolType,
            @RequestParam("provinceCodeName") String provinceCodeName,@RequestParam("cityCodeName") String cityCodeName,
            @RequestParam("districtCodeName") String districtCodeName) throws Exception {
        Boolean isLogin = (Boolean) httpServletRequest.getSession().getAttribute("IS_LOGIN");
        AdminVO loginUser =
                JSON.parseObject((String) httpServletRequest.getSession().getAttribute("LOGIN_USER"), AdminVO.class);
        if (isLogin == null || !isLogin) {
            throw new BusinessException(EmBusinessError.USER_NOT_LOGIN);
        } else if (loginUser.getAccountRole() != AccountRoleConstant.ACCOUNT_ROLE_SCHOOL
                || loginUser.getAccountRole() != AccountRoleConstant.ACCOUNT_ROLE_SCHOOL_DEAN) {
            throw new BusinessException(EmBusinessError.ACCESS_VIOLATION_ERROR);
        } else {
            List<Integer> schoolIdList = loginUser.getSchoolId();
            if (CollectionUtils.isEmpty(schoolIdList)) {
                throw new BusinessException(EmBusinessError.SCHOOL_INFO_ERROR);
            } else if (!schoolIdList.contains(id)) {
                throw new BusinessException(EmBusinessError.ACCESS_VIOLATION_ERROR);
            } else {
                SchoolModel schoolModel = new SchoolModel();
                schoolModel.setId(id);
                schoolModel.setSchoolName(schoolName);
                schoolModel.setSchoolAddress(schoolAddress);
                schoolModel.setSchoolContact(schoolContact);
                schoolModel.setSchoolTel(schoolTel);
                schoolModel.setAlipayAccount(alipayAccount);
                schoolModel.setSchoolSystem(schoolSystem);
                schoolModel.setSchoolType(schoolType);
                List<String> province = this.splitCodeName(provinceCodeName);
                schoolModel.setProvinceCode(province.get(0));
                schoolModel.setProvinceName(province.get(1));
                List<String> city = this.splitCodeName(cityCodeName);
                schoolModel.setCityCode(city.get(0));
                schoolModel.setCityName(city.get(1));
                List<String> district = this.splitCodeName(districtCodeName);
                schoolModel.setDistrictCode(district.get(0));
                schoolModel.setDistrictName(district.get(1));
                schoolService.updateSchool(schoolModel);
            }
        }
        return CommonReturnType.create(null);
    }

    /**
     * 删除学校记录,只有isv类型账号有权删除其下属school
     * 及该学校下属所有管理员,及对应的班级.学生.账单,账单类型记录
     * 硬删除:说明该学校退出系统使用
     * 暂时close,上线开放
     * @param id 学校编号
     */
    @RequestMapping(value = "/delete", method = {RequestMethod.POST}, consumes = {CONTENT_TYPE_FORMED})
    @ResponseBody
    public CommonReturnType deleteSchool(@RequestParam("id") Integer id) throws Exception {
        Boolean isLogin = (Boolean) httpServletRequest.getSession().getAttribute("IS_LOGIN");
        AdminVO loginUser =
                JSON.parseObject((String) httpServletRequest.getSession().getAttribute("LOGIN_USER"), AdminVO.class);
        if (isLogin == null || !isLogin) {
            throw new BusinessException(EmBusinessError.USER_NOT_LOGIN);
        } else if (loginUser.getAccountRole() != AccountRoleConstant.ACCOUNT_ROLE_ISV) {
            throw new BusinessException(EmBusinessError.ACCESS_VIOLATION_ERROR);
        } else {
            List<Integer> schoolIdList = loginUser.getSchoolId();
            if (CollectionUtils.isEmpty(schoolIdList)) {
                throw new BusinessException(EmBusinessError.SCHOOL_INFO_ERROR);
            } else if (!schoolIdList.contains(id)) {
                throw new BusinessException(EmBusinessError.ACCESS_VIOLATION_ERROR);
            } else {
                schoolService.deleteSchool(id);
            }
        }
        return CommonReturnType.create(null);
    }

    private SchoolVO convertVOFromModel(SchoolModel schoolModel) {
        if (schoolModel == null) {
            return null;
        }
        SchoolVO schoolVO = new SchoolVO();
        BeanUtils.copyProperties(schoolModel, schoolVO);
        IsvModel isvById = isvService.getIsvById(schoolModel.getIsvId());
        schoolVO.setIsvName(isvById.getCompanyName());
        return schoolVO;
    }

    private List<SchoolGradeClassVO> getSchoolGradeClassVO(List<SchoolModel> schoolModelList) {
        List<SchoolGradeClassVO> schoolGradeClassVOList = new ArrayList<>();
        for (SchoolModel schoolModel : schoolModelList) {
            if (schoolModel == null) {
                return null;
            }
            SchoolGradeClassVO schoolGradeClassVO = new SchoolGradeClassVO();
            schoolGradeClassVO.setSchoolId(schoolModel.getId());
            schoolGradeClassVO.setSchoolName(schoolModel.getSchoolName());
            // 根据学校id获取班级list
            List<ClassModel> classModelList =
                    classService.listClassBySchoolId(schoolModel.getId(), DeleteStatusConstant.DELETE_STATUS_STAY);
            if (classModelList != null) {
                List<SchoolGradeClassVO.ClassInfo> classInfoList = classModelList.stream().map(classModel -> {
                    SchoolGradeClassVO.ClassInfo classInfo = new SchoolGradeClassVO.ClassInfo();
                    classInfo.setClassId(classModel.getId());
                    classInfo.setGradeNum(classModel.getGradeNum());
                    classInfo.setClassNum(classModel.getClassNum());
                    return classInfo;
                }).collect(Collectors.toList());
                schoolGradeClassVO.setClassInfoList(classInfoList);
            }
            // 根据学校id获取缴费类型list
            List<BillItemDO> billItemDOList =
                    billItemDOMapper.listItemBySchool(schoolModel.getId(), DeleteStatusConstant.DELETE_STATUS_STAY);
            if (billItemDOList != null) {
                List<SchoolGradeClassVO.ItemInfo> itemInfoList = billItemDOList.stream().map(billItemDO -> {
                    SchoolGradeClassVO.ItemInfo itemInfo = new SchoolGradeClassVO.ItemInfo();
                    itemInfo.setItemId(billItemDO.getId());
                    itemInfo.setItemName(billItemDO.getItemName());
                    return itemInfo;
                }).collect(Collectors.toList());
                schoolGradeClassVO.setItemInfoList(itemInfoList);
            }
            schoolGradeClassVOList.add(schoolGradeClassVO);
        }
        return schoolGradeClassVOList;
    }

    private List<String> splitCodeName(String string) {
        // 接收包含账单id的字符串，并将它分割成字符串数组
        String[] idString = string.split(",");
        // 将字符串数组转为List<Integer> 类型
        return new ArrayList<>(Arrays.asList(idString));
    }

}
