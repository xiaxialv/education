package com.zaozao.controller;

import com.alibaba.fastjson.JSON;
import com.zaozao.Constant.AccountRoleConstant;
import com.zaozao.Constant.BillConstant;
import com.zaozao.Constant.DeleteStatusConstant;
import com.zaozao.controller.viewobject.AdminVO;
import com.zaozao.controller.viewobject.BillUnpaidVO;
import com.zaozao.controller.viewobject.BillVO;
import com.zaozao.dao.*;
import com.zaozao.dataobject.BillDO;
import com.zaozao.dataobject.BillItemDO;
import com.zaozao.error.BusinessException;
import com.zaozao.error.EmBusinessError;
import com.zaozao.response.CommonReturnType;
import com.zaozao.service.*;
import com.zaozao.service.model.BillModel;
import com.zaozao.service.model.ClassModel;
import com.zaozao.service.model.SchoolModel;
import com.zaozao.service.model.StudentModel;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.hssf.usermodel.*;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static com.zaozao.Constant.BillConstant.BILL_SYN_STATUS_UNSYN;

/**
 * @author Sidney 2019-01-10.
 */
@Controller("billController")
@RequestMapping("/bill")
@CrossOrigin(allowCredentials = "true", origins = "*")
public class BillController extends BaseController {
    @Autowired
    private BillService billService;

    @Autowired
    private SchoolService schoolService;

    @Autowired
    private ClassService classService;

    @Autowired
    private StudentService studentService;

    @Autowired
    private HttpServletRequest httpServletRequest;

    @Autowired
    private BillDOMapper billDOMapper;

    @Autowired
    private BillItemDOMapper billItemDOMapper;

    @Autowired
    private StudentDOMapper studentDOMapper;

    @Autowired
    private AlipayOrderInfoDOMapper alipayOrderInfoDOMapper;

    @Autowired
    private IsvAlipayInfoDOMapper isvAlipayInfoDOMapper;

    @Autowired
    private AlipayService alipayService;

    @Autowired
    private BillAlipayService billAlipayService;

    private Logger logger = LoggerFactory.getLogger(getClass());

    @RequestMapping(value = "/unpaid", method = {RequestMethod.POST}, consumes = {CONTENT_TYPE_FORMED})
    @ResponseBody
    public CommonReturnType getUnpaidBill(@RequestParam("schoolId") Integer schoolId,
            @RequestParam("classId") Integer classId, @RequestParam("studentNo") String studentNo)
            throws BusinessException {
        List<BillModel> billModelList = billService
                .listBillBySchoolAndClassAndStudentNo(schoolId, classId, studentNo, BillConstant.BILL_STATUS_UNPAID,
                        DeleteStatusConstant.DELETE_STATUS_STAY);
        if (CollectionUtils.isEmpty(billModelList)) {
            throw new BusinessException(EmBusinessError.STUDENT_NOT_FIND);
        }
        List<BillUnpaidVO> billUnpaidVOList =
                billModelList.stream().map(this::convertUnpaidVOFromModel).collect(Collectors.toList());
        return CommonReturnType.create(billUnpaidVOList);
    }

    @RequestMapping(value = "/create/item", method = {RequestMethod.POST}, consumes = {CONTENT_TYPE_FORMED})
    @ResponseBody
    public CommonReturnType createItem(@RequestParam("itemName") String itemName) throws Exception {
        Boolean isLogin = (Boolean) httpServletRequest.getSession().getAttribute("IS_LOGIN");
        AdminVO loginUser =
                JSON.parseObject((String) httpServletRequest.getSession().getAttribute("LOGIN_USER"), AdminVO.class);
        if (isLogin == null || !isLogin) {
            throw new BusinessException(EmBusinessError.USER_NOT_LOGIN);
        }
        Byte accountRole = loginUser.getAccountRole();
        if (accountRole == AccountRoleConstant.ACCOUNT_ROLE_SCHOOL
                || accountRole == AccountRoleConstant.ACCOUNT_ROLE_SCHOOL_FINACIAL) {
            if (StringUtils.isEmpty(itemName)) {
                throw new BusinessException(EmBusinessError.PARAM_VALIDATION_ERROR);
            }
            List<Integer> schoolIdList = loginUser.getSchoolId();
            if (CollectionUtils.isEmpty(schoolIdList)) {
                throw new BusinessException(EmBusinessError.SCHOOL_INFO_ERROR);
            }
            BillItemDO billItemDO = new BillItemDO();
            billItemDO.setItemName(itemName);
            billItemDO.setSchoolId(schoolIdList.get(0));
            billService.createBillItem(billItemDO);
        } else {
            throw new BusinessException(EmBusinessError.ACCESS_VIOLATION_ERROR);
        }
        return CommonReturnType.create(null);
    }

    /**
     * Model model, @RequestParam(defaultValue = "1", value = "pageNum") Integer pageNum
     */
    @RequestMapping(value = "/list/item", method = {RequestMethod.GET})
    @ResponseBody
    public CommonReturnType listItem() throws BusinessException {
        Boolean isLogin = (Boolean) httpServletRequest.getSession().getAttribute("IS_LOGIN");
        AdminVO loginUser =
                JSON.parseObject((String) httpServletRequest.getSession().getAttribute("LOGIN_USER"), AdminVO.class);

        List<BillItemDO> billItemDOList;
        if (isLogin == null || !isLogin) {
            throw new BusinessException(EmBusinessError.USER_NOT_LOGIN);
        }
        Byte accountRole = loginUser.getAccountRole();
        if (accountRole == AccountRoleConstant.ACCOUNT_ROLE_SCHOOL
                || accountRole == AccountRoleConstant.ACCOUNT_ROLE_SCHOOL_FINACIAL) {
            List<Integer> schoolIdList = loginUser.getSchoolId();
            if (CollectionUtils.isEmpty(schoolIdList)) {
                throw new BusinessException(EmBusinessError.SCHOOL_INFO_ERROR);
            }
            Integer schoolId = schoolIdList.get(0);
            //PageHelper.startPage(pageNum, 10);
            billItemDOList = billService.listBillItemBySchool(schoolId, DeleteStatusConstant.DELETE_STATUS_STAY);
            //PageInfo<BillItemDO> pageInfo = new PageInfo<>(billItemDOList);
            //model.addAttribute("pageInfo", pageInfo);
        } else {
            throw new BusinessException(EmBusinessError.ACCESS_VIOLATION_ERROR);
        }
        return CommonReturnType.create(billItemDOList);
    }

    @RequestMapping(value = "/delete/item", method = {RequestMethod.POST}, consumes = {CONTENT_TYPE_FORMED})
    @ResponseBody
    public CommonReturnType createItem(@RequestParam("id") Integer id, @RequestParam("schoolId") Integer schoolId)
            throws Exception {
        Boolean isLogin = (Boolean) httpServletRequest.getSession().getAttribute("IS_LOGIN");
        AdminVO loginUser =
                JSON.parseObject((String) httpServletRequest.getSession().getAttribute("LOGIN_USER"), AdminVO.class);
        if (isLogin == null || !isLogin) {
            throw new BusinessException(EmBusinessError.USER_NOT_LOGIN);
        }
        Byte accountRole = loginUser.getAccountRole();
        if (accountRole == AccountRoleConstant.ACCOUNT_ROLE_SCHOOL
                || accountRole == AccountRoleConstant.ACCOUNT_ROLE_SCHOOL_FINACIAL) {
            List<Integer> schoolIdList = loginUser.getSchoolId();
            if (CollectionUtils.isEmpty(schoolIdList)) {
                throw new BusinessException(EmBusinessError.SCHOOL_INFO_ERROR);
            } else if (!schoolIdList.contains(schoolId)) {
                throw new BusinessException(EmBusinessError.ACCESS_VIOLATION_ERROR);
            } else {
                billService.softDeleteBillItem(id);
            }
        } else {
            throw new BusinessException(EmBusinessError.ACCESS_VIOLATION_ERROR);
        }
        return CommonReturnType.create(null);
    }

    /**
     * Model model,@RequestParam(defaultValue = "1", value = "pageNum") Integer pageNum
     */
    @RequestMapping(value = "/list", method = {RequestMethod.GET})
    @ResponseBody
    public CommonReturnType getBillList() throws Exception {
        Boolean isLogin = (Boolean) httpServletRequest.getSession().getAttribute("IS_LOGIN");
        AdminVO loginUser =
                JSON.parseObject((String) httpServletRequest.getSession().getAttribute("LOGIN_USER"), AdminVO.class);
        List<BillVO> billVOList;
        //PageHelper.startPage(pageNum, PageSizeConstant.PAGE_SIZE_BILL);
        if (isLogin == null || !isLogin) {
            throw new BusinessException(EmBusinessError.USER_NOT_LOGIN);
        }
        Byte accountRole = loginUser.getAccountRole();
        if (accountRole == AccountRoleConstant.ACCOUNT_ROLE_SUPER_ADMIN) {
            List<BillModel> billModelList = billService.listBill(DeleteStatusConstant.DELETE_STATUS_STAY);
            billVOList = billModelList.stream().map(this::convertVOFromModel).collect(Collectors.toList());
        } else if (accountRole == AccountRoleConstant.ACCOUNT_ROLE_SCHOOL
                || accountRole == AccountRoleConstant.ACCOUNT_ROLE_SCHOOL_FINACIAL
                || accountRole == AccountRoleConstant.ACCOUNT_ROLE_SCHOOL_DEAN) {
            List<Integer> schoolIdList = loginUser.getSchoolId();
            if (CollectionUtils.isEmpty(schoolIdList)) {
                throw new BusinessException(EmBusinessError.SCHOOL_INFO_ERROR);
            }
            Integer schoolId = schoolIdList.get(0);
            List<BillModel> billModelList =
                    billService.listBillBySchoolId(schoolId, DeleteStatusConstant.DELETE_STATUS_STAY);
            billVOList = billModelList.stream().map(this::convertVOFromModel).collect(Collectors.toList());
        } else if (accountRole == AccountRoleConstant.ACCOUNT_ROLE_ISV) {
            List<Integer> schoolIdList = loginUser.getSchoolId();
            if (CollectionUtils.isEmpty(schoolIdList)) {
                throw new BusinessException(EmBusinessError.SCHOOL_INFO_ERROR);
            }
            List<BillModel> billModelList =
                    billService.listBillBySchoolIdList(schoolIdList, DeleteStatusConstant.DELETE_STATUS_STAY);
            billVOList = billModelList.stream().map(this::convertVOFromModel).collect(Collectors.toList());
        } else {
            throw new BusinessException(EmBusinessError.ACCESS_VIOLATION_ERROR);
        }
        //PageInfo<BillVO> pageInfo = new PageInfo<>(billVOList);
        //model.addAttribute("pageInfo", pageInfo);
        return CommonReturnType.create(billVOList);
    }

    @RequestMapping(value = "/query/list", method = {RequestMethod.POST}, consumes = {CONTENT_TYPE_FORMED})
    @ResponseBody
    public CommonReturnType queryBillList(@RequestParam("schoolId") Integer schoolId,
            @RequestParam("gradeNum") Integer gradeNum, @RequestParam("classNum") Integer classNum,
            @RequestParam("studentNo") String studentNo, @RequestParam("studentName") String studentName,
            @RequestParam("createDate") String createDate, @RequestParam("payDate") String payDate,
            @RequestParam("billNum") String billNum, @RequestParam("payType") Byte payType,
            @RequestParam("billStatus") Byte billStatus, @RequestParam("synStatus") Byte synStatus,
            @RequestParam("billName") String billName, @RequestParam("billItemId") Integer billItemId)
            throws Exception {
        Boolean isLogin = (Boolean) httpServletRequest.getSession().getAttribute("IS_LOGIN");
        AdminVO loginUser =
                JSON.parseObject((String) httpServletRequest.getSession().getAttribute("LOGIN_USER"), AdminVO.class);
        List<BillVO> billVOList;
        //PageHelper.startPage(pageNum, PageSizeConstant.PAGE_SIZE_BILL);
        if (isLogin == null || !isLogin) {
            throw new BusinessException(EmBusinessError.USER_NOT_LOGIN);
        }
        Byte accountRole = loginUser.getAccountRole();
        boolean emptyParam = schoolId == null && gradeNum == null && classNum == null && "".equals(studentNo) && ""
                .equals(studentName) && "".equals(createDate) && "".equals(payDate) && "".equals(billNum)
                && payType == null && billStatus == null && synStatus == null && "".equals(billName)
                && billItemId == null;
        if (accountRole == AccountRoleConstant.ACCOUNT_ROLE_SUPER_ADMIN) {
            if (emptyParam) {
                List<BillModel> billModelList = billService.listBill(DeleteStatusConstant.DELETE_STATUS_STAY);
                billVOList = billModelList.stream().map(this::convertVOFromModel).collect(Collectors.toList());
            } else {
                List<BillModel> billModelList = billService
                        .listBillByDynamicQuery(schoolId, gradeNum, classNum, studentNo, studentName, createDate,
                                payDate, billNum, payType, billStatus, synStatus, billName, billItemId,
                                DeleteStatusConstant.DELETE_STATUS_STAY);
                if (billModelList == null) {
                    return CommonReturnType.create(null);
                } else {
                    billVOList = billModelList.stream().map(this::convertVOFromModel).collect(Collectors.toList());
                }
            }
        } else if (accountRole == AccountRoleConstant.ACCOUNT_ROLE_SCHOOL
                || accountRole == AccountRoleConstant.ACCOUNT_ROLE_SCHOOL_FINACIAL
                || accountRole == AccountRoleConstant.ACCOUNT_ROLE_SCHOOL_DEAN) {
            List<Integer> schoolIdList = loginUser.getSchoolId();
            if (CollectionUtils.isEmpty(schoolIdList)) {
                throw new BusinessException(EmBusinessError.SCHOOL_INFO_ERROR);
            }
            if (emptyParam) {
                Integer schoolIdAuthorized = schoolIdList.get(0);
                List<BillModel> billModelList =
                        billService.listBillBySchoolId(schoolIdAuthorized, DeleteStatusConstant.DELETE_STATUS_STAY);
                if (billModelList == null) {
                    return CommonReturnType.create(null);
                } else {
                    billVOList = billModelList.stream().map(this::convertVOFromModel).collect(Collectors.toList());
                }
            } else {
                List<BillModel> billModelList = billService
                        .listBillByDynamicQuery(schoolId, gradeNum, classNum, studentNo, studentName, createDate,
                                payDate, billNum, payType, billStatus, synStatus, billName, billItemId,
                                DeleteStatusConstant.DELETE_STATUS_STAY);
                if (billModelList == null) {
                    return CommonReturnType.create(null);
                } else {
                    billVOList = billModelList.stream().map(this::convertVOFromModel).collect(Collectors.toList());
                }
            }
        } else if (accountRole == AccountRoleConstant.ACCOUNT_ROLE_ISV) {
            List<Integer> schoolIdList = loginUser.getSchoolId();
            if (CollectionUtils.isEmpty(schoolIdList)) {
                throw new BusinessException(EmBusinessError.SCHOOL_INFO_ERROR);
            }
            if (emptyParam) {
                List<BillModel> billModelList =
                        billService.listBillBySchoolIdList(schoolIdList, DeleteStatusConstant.DELETE_STATUS_STAY);
                billVOList = billModelList.stream().map(this::convertVOFromModel).collect(Collectors.toList());
            } else {
                List<BillModel> billModelList = billService
                        .listBillByDynamicQuery(schoolId, gradeNum, classNum, studentNo, studentName, createDate,
                                payDate, billNum, payType, billStatus, synStatus, billName, billItemId,
                                DeleteStatusConstant.DELETE_STATUS_STAY);
                billVOList = billModelList.stream().map(this::convertVOFromModel).collect(Collectors.toList());
            }
        } else {
            throw new BusinessException(EmBusinessError.ACCESS_VIOLATION_ERROR);
        }
        //PageInfo<BillVO> pageInfo = new PageInfo<>(billVOList);
        //model.addAttribute("pageInfo", pageInfo);
        return CommonReturnType.create(billVOList);
    }

    /**
     * Model model,@RequestParam(defaultValue = "1", value = "pageNum") Integer pageNum
     */
    @RequestMapping(value = "/list/history", method = {RequestMethod.GET})
    @ResponseBody
    public CommonReturnType getBillHistoryList() throws Exception {
        Boolean isLogin = (Boolean) httpServletRequest.getSession().getAttribute("IS_LOGIN");
        AdminVO loginUser =
                JSON.parseObject((String) httpServletRequest.getSession().getAttribute("LOGIN_USER"), AdminVO.class);
        List<BillVO> billVOList;
        //PageHelper.startPage(pageNum, PageSizeConstant.PAGE_SIZE_BILL);
        if (isLogin == null || !isLogin) {
            throw new BusinessException(EmBusinessError.USER_NOT_LOGIN);
        }
        Byte accountRole = loginUser.getAccountRole();
        if (accountRole == AccountRoleConstant.ACCOUNT_ROLE_SUPER_ADMIN) {
            List<BillModel> billModelList = billService.listBillLike(DeleteStatusConstant.DELETE_STATUS_HISTORY);
            billVOList = billModelList.stream().map(this::convertVOFromModel).collect(Collectors.toList());
        } else if (accountRole == AccountRoleConstant.ACCOUNT_ROLE_SCHOOL
                || accountRole == AccountRoleConstant.ACCOUNT_ROLE_SCHOOL_FINACIAL
                || accountRole == AccountRoleConstant.ACCOUNT_ROLE_SCHOOL_DEAN) {
            List<Integer> schoolIdList = loginUser.getSchoolId();
            if (CollectionUtils.isEmpty(schoolIdList)) {
                throw new BusinessException(EmBusinessError.SCHOOL_INFO_ERROR);
            }
            Integer schoolId = schoolIdList.get(0);
            List<BillModel> billModelList =
                    billService.listBillBySchoolIdLike(schoolId, DeleteStatusConstant.DELETE_STATUS_HISTORY);
            billVOList = billModelList.stream().map(this::convertVOFromModel).collect(Collectors.toList());
        } else if (accountRole == AccountRoleConstant.ACCOUNT_ROLE_ISV) {
            List<Integer> schoolIdList = loginUser.getSchoolId();
            if (CollectionUtils.isEmpty(schoolIdList)) {
                throw new BusinessException(EmBusinessError.SCHOOL_INFO_ERROR);
            }
            List<BillModel> billModelList =
                    billService.listBillBySchoolIdListLike(schoolIdList, DeleteStatusConstant.DELETE_STATUS_HISTORY);
            billVOList = billModelList.stream().map(this::convertVOFromModel).collect(Collectors.toList());
        } else {
            throw new BusinessException(EmBusinessError.ACCESS_VIOLATION_ERROR);
        }
        //PageInfo<BillVO> pageInfo = new PageInfo<>(billVOList);
        //model.addAttribute("pageInfo", pageInfo);
        return CommonReturnType.create(billVOList);
    }

    /**
     * 基于学号的添加,学校信息根据登录账号获得,班级信息根据学号从班级表中查询出来,学生信息根据学生表查询
     *
     * @param studentNo  学号
     * @param billName   账单名称
     * @param billItemId 缴费类型编号
     * @param billAmount 缴费金额
     * @param comment    备注
     * @return billVO
     */
    @RequestMapping(value = "/create/bill", method = {RequestMethod.POST}, consumes = {CONTENT_TYPE_FORMED})
    @ResponseBody
    public CommonReturnType createBill(@RequestParam("studentNo") String studentNo,
            @RequestParam("billName") String billName, @RequestParam("billItemId") Integer billItemId,
            @RequestParam("billAmount") BigDecimal billAmount, @RequestParam("comment") String comment,
            @RequestParam("endDate") String endDate) throws Exception {
        Boolean isLogin = (Boolean) httpServletRequest.getSession().getAttribute("IS_LOGIN");
        AdminVO loginUser =
                JSON.parseObject((String) httpServletRequest.getSession().getAttribute("LOGIN_USER"), AdminVO.class);

        //        BillVO billVO;
        if (isLogin == null || !isLogin) {
            throw new BusinessException(EmBusinessError.USER_NOT_LOGIN);
        }
        Byte accountRole = loginUser.getAccountRole();
        if (AccountRoleConstant.ACCOUNT_ROLE_SCHOOL == accountRole
                || AccountRoleConstant.ACCOUNT_ROLE_SCHOOL_FINACIAL == accountRole) {
            List<Integer> schoolIdList = loginUser.getSchoolId();
            if (CollectionUtils.isEmpty(schoolIdList)) {
                throw new BusinessException(EmBusinessError.SCHOOL_INFO_ERROR);
            }
            Integer schoolId = schoolIdList.get(0);
            // 判断学生是否存在
            SchoolModel schoolById = schoolService.getSchoolById(schoolId);
            StudentModel studentByStudentNo =
                    studentService.getStudentByStudentNo(schoolId, studentNo, DeleteStatusConstant.DELETE_STATUS_STAY);
            if (studentByStudentNo == null) {
                throw new BusinessException(EmBusinessError.STUDENT_NOT_FIND);
            }
            // 判断缴费类型是否存在
            List<BillItemDO> billItemDOList =
                    billItemDOMapper.listItemBySchool(schoolId, DeleteStatusConstant.DELETE_STATUS_STAY);
            List<Integer> billItemIdList = billItemDOList.stream().map(BillItemDO::getId).collect(Collectors.toList());
            if (CollectionUtils.isEmpty(billItemIdList) || !billItemIdList.contains(billItemId)) {
                throw new BusinessException(EmBusinessError.BILL_ITEM_NULL);
            }
            Integer classId = studentByStudentNo.getClassId();
            ClassModel classById = classService.getClassById(classId);
            BillModel billModel = new BillModel();
            billModel.setStudentIdentity(studentByStudentNo.getStudentIdentity());
            billModel.setStudentId(studentByStudentNo.getId());
            billModel.setStudentName(studentByStudentNo.getName());
            billModel.setSchoolId(schoolId);
            billModel.setSchoolName(schoolById.getSchoolName());
            billModel.setIsvId(schoolById.getIsvId());
            billModel.setClassId(classId);
            billModel.setGradeNum(classById.getGradeNum());
            billModel.setClassNum(classById.getClassNum());
            billModel.setStudentNo(studentNo);
            billModel.setBillName(billName);
            billModel.setBillItemId(billItemId);
            billModel.setBillAmount(billAmount);
            billModel.setBillStatus(BillConstant.BILL_STATUS_UNPAID);
            billModel.setSynStatus(BILL_SYN_STATUS_UNSYN);
            DateTime now = new DateTime();
            billModel.setCreateDate(now);
            DateTime end = new DateTime(endDate);
            billModel.setEndDate(end);
            billModel.setComment(comment);
            billService.createBill(billModel);
        } else {
            throw new BusinessException(EmBusinessError.ACCESS_VIOLATION_ERROR);
        }
        return CommonReturnType.create(null);
    }

    /**
     * 班级信息根据学号从学生表中获取(删除)
     *
     * @param billStatus 已支付的账单不允许修改
     */
    @RequestMapping(value = "/update/bill", method = {RequestMethod.POST}, consumes = {CONTENT_TYPE_FORMED})
    @ResponseBody
    public CommonReturnType updateBill(@RequestParam("id") Integer id, @RequestParam("schoolId") Integer schoolId,
            @RequestParam("studentNo") String studentNo, @RequestParam("billName") String billName,
            @RequestParam("billItemId") Integer billItemId, @RequestParam("billAmount") BigDecimal billAmount,
            @RequestParam("comment") String comment, @RequestParam("billStatus") Byte billStatus,
            @RequestParam("endDate") String endDate) throws Exception {
        Boolean isLogin = (Boolean) httpServletRequest.getSession().getAttribute("IS_LOGIN");
        AdminVO loginUser =
                JSON.parseObject((String) httpServletRequest.getSession().getAttribute("LOGIN_USER"), AdminVO.class);
        if (isLogin == null || !isLogin) {
            throw new BusinessException(EmBusinessError.USER_NOT_LOGIN);
        }
        Byte accountRole = loginUser.getAccountRole();
        if (AccountRoleConstant.ACCOUNT_ROLE_SCHOOL == accountRole
                || AccountRoleConstant.ACCOUNT_ROLE_SCHOOL_FINACIAL == accountRole) {
            List<Integer> schoolIdList = loginUser.getSchoolId();
            if (CollectionUtils.isEmpty(schoolIdList)) {
                throw new BusinessException(EmBusinessError.SCHOOL_INFO_ERROR);
            } else if (!schoolIdList.contains(schoolId)) {
                throw new BusinessException(EmBusinessError.ACCESS_VIOLATION_ERROR);
                // TODO 已支付的账单不允许修改;其他地方或也需加此校验
            } else if (billStatus.equals(BillConstant.BILL_STATUS_PAID)) {
                throw new BusinessException(EmBusinessError.BILL_UNPAID_ERROR);
            }
            // 判断缴费类型是否存在
            List<BillItemDO> billItemDOList =
                    billItemDOMapper.listItemBySchool(schoolId, DeleteStatusConstant.DELETE_STATUS_STAY);
            List<Integer> billItemIdList = billItemDOList.stream().map(BillItemDO::getId).collect(Collectors.toList());
            if (CollectionUtils.isEmpty(billItemIdList) || !billItemIdList.contains(billItemId)) {
                throw new BusinessException(EmBusinessError.BILL_ITEM_NULL);
            }
            // 学生是否存在
            StudentModel studentByStudentNo =
                    studentService.getStudentByStudentNo(schoolId, studentNo, DeleteStatusConstant.DELETE_STATUS_STAY);
            if (studentByStudentNo == null) {
                throw new BusinessException(EmBusinessError.STUDENT_NOT_FIND);
            }
            billAlipayService.updateBill(id, studentNo, billName, billItemId, billAmount, comment, endDate);
        } else {
            throw new BusinessException(EmBusinessError.ACCESS_VIOLATION_ERROR);
        }
        return CommonReturnType.create(null);
    }

    /**
     * 一键缴费,可多选
     *
     * @param ids       账单ids
     * @param payType   支付方式
     * @param schoolIds 学校ids
     * @param comment   备注
     * @param billNum   票据号
     * @return 通用类
     * @throws Exception Business异常
     *                   TODO 修改账单后如何保证后续的修改--同步到数据库的代码都执行了?是否需要在controller添加事务?
     */
    @RequestMapping(value = "/click/collection", method = {RequestMethod.POST}, consumes = {CONTENT_TYPE_FORMED})
    @ResponseBody
    public CommonReturnType updateBill(@RequestParam("ids") String ids, @RequestParam("payType") Byte payType,
            @RequestParam("schoolIds") String schoolIds, @RequestParam("comment") String comment,
            @RequestParam("billNum") String billNum) throws Exception {
        Boolean isLogin = (Boolean) httpServletRequest.getSession().getAttribute("IS_LOGIN");
        AdminVO loginUser =
                JSON.parseObject((String) httpServletRequest.getSession().getAttribute("LOGIN_USER"), AdminVO.class);
        if (isLogin == null || !isLogin) {
            throw new BusinessException(EmBusinessError.USER_NOT_LOGIN);
        }
        Byte accountRole = loginUser.getAccountRole();
        if (AccountRoleConstant.ACCOUNT_ROLE_SCHOOL == accountRole
                || AccountRoleConstant.ACCOUNT_ROLE_SCHOOL_FINACIAL == accountRole) {
            List<Integer> schoolIdList = loginUser.getSchoolId();
            // 将账单对应的学校ids进行转化并去重
            List<Integer> listSchool = this.splitString(schoolIds);
            List list = this.removeDuplicate(listSchool);
            if (CollectionUtils.isEmpty(schoolIdList)) {
                throw new BusinessException(EmBusinessError.SCHOOL_INFO_ERROR);
            } else if (list.size() > 1) {
                throw new BusinessException(EmBusinessError.ACCESS_VIOLATION_ERROR);
            } else {
                billAlipayService.clickCollection(ids,payType,comment,billNum);
            }
        } else {
            throw new BusinessException(EmBusinessError.ACCESS_VIOLATION_ERROR);
        }
        return CommonReturnType.create(null);
    }

    /**
     * 软删除
     */
    @RequestMapping(value = "/delete/bill", method = {RequestMethod.POST}, consumes = {CONTENT_TYPE_FORMED})
    @ResponseBody
    public CommonReturnType deleteBill(@RequestParam("id") Integer id, @RequestParam("schoolId") Integer schoolId)
            throws Exception {
        Boolean isLogin = (Boolean) httpServletRequest.getSession().getAttribute("IS_LOGIN");
        AdminVO loginUser =
                JSON.parseObject((String) httpServletRequest.getSession().getAttribute("LOGIN_USER"), AdminVO.class);
        if (isLogin == null || !isLogin) {
            throw new BusinessException(EmBusinessError.USER_NOT_LOGIN);
        }
        Byte accountRole = loginUser.getAccountRole();
        if (AccountRoleConstant.ACCOUNT_ROLE_SCHOOL == accountRole
                || AccountRoleConstant.ACCOUNT_ROLE_SCHOOL_FINACIAL == accountRole) {
            List<Integer> schoolIdList = loginUser.getSchoolId();
            if (CollectionUtils.isEmpty(schoolIdList)) {
                throw new BusinessException(EmBusinessError.SCHOOL_INFO_ERROR);
            } else if (!schoolIdList.contains(schoolId)) {
                throw new BusinessException(EmBusinessError.ACCESS_VIOLATION_ERROR);
            } else {
                billAlipayService.delete(id);
            }

        } else {
            throw new BusinessException(EmBusinessError.ACCESS_VIOLATION_ERROR);
        }
        return CommonReturnType.create(null);
    }

    /**
     * 批量删除账单
     */
    @RequestMapping(value = "/delete/batch", method = {RequestMethod.POST}, consumes = {CONTENT_TYPE_FORMED})
    @ResponseBody
    public CommonReturnType deleteBatchBill(@RequestParam("ids") String ids, @RequestParam("schoolId") Integer schoolId)
            throws Exception {
        Boolean isLogin = (Boolean) httpServletRequest.getSession().getAttribute("IS_LOGIN");
        AdminVO loginUser =
                JSON.parseObject((String) httpServletRequest.getSession().getAttribute("LOGIN_USER"), AdminVO.class);
        if (isLogin == null || !isLogin) {
            throw new BusinessException(EmBusinessError.USER_NOT_LOGIN);
        }
        Byte accountRole = loginUser.getAccountRole();
        if (AccountRoleConstant.ACCOUNT_ROLE_SCHOOL == accountRole
                || AccountRoleConstant.ACCOUNT_ROLE_SCHOOL_FINACIAL == accountRole) {
            List<Integer> schoolIdList = loginUser.getSchoolId();
            if (CollectionUtils.isEmpty(schoolIdList)) {
                throw new BusinessException(EmBusinessError.SCHOOL_INFO_ERROR);
            } else if (!schoolIdList.contains(schoolId)) {
                throw new BusinessException(EmBusinessError.ACCESS_VIOLATION_ERROR);
            } else {
                List<Integer> idList = this.splitString(ids);
                // 调用service层的批量删除函数
                for (Integer id : idList) {
                    billAlipayService.delete(id);
                }
            }
        } else {
            throw new BusinessException(EmBusinessError.ACCESS_VIOLATION_ERROR);
        }
        return CommonReturnType.create(null);
    }

    /**
     * 导入学生账单excel表
     */
    @RequestMapping(value = "/import", method = {RequestMethod.POST}, consumes = {CONTENT_TYPE_FILE})
    @ResponseBody
    public boolean importBill(@RequestParam("file") MultipartFile file, @RequestParam("billName") String billName,
            @RequestParam("endDate") String endDate) throws BusinessException {
        boolean a = false;
        Boolean isLogin = (Boolean) httpServletRequest.getSession().getAttribute("IS_LOGIN");
        AdminVO loginUser =
                JSON.parseObject((String) httpServletRequest.getSession().getAttribute("LOGIN_USER"), AdminVO.class);

        if (isLogin == null || !isLogin) {
            throw new BusinessException(EmBusinessError.USER_NOT_LOGIN);
        }
        Byte accountRole = loginUser.getAccountRole();
        if (AccountRoleConstant.ACCOUNT_ROLE_SCHOOL == accountRole
                || AccountRoleConstant.ACCOUNT_ROLE_SCHOOL_FINACIAL == accountRole
                || AccountRoleConstant.ACCOUNT_ROLE_SCHOOL_DEAN == accountRole) {
            List<Integer> schoolIdList = loginUser.getSchoolId();
            if (CollectionUtils.isEmpty(schoolIdList)) {
                throw new BusinessException(EmBusinessError.SCHOOL_INFO_ERROR);
            }
            Integer schoolId = schoolIdList.get(0);
            String fileName = file.getOriginalFilename();
            try {
                a = billService.batchImport(fileName, file, schoolId, billName, endDate);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return a;
        } else {
            throw new BusinessException(EmBusinessError.ACCESS_VIOLATION_ERROR);
        }

    }

    /**
     * 导出学生账单excel表
     */
    @RequestMapping(value = "/export", method = RequestMethod.GET)
    public void exportBill(HttpServletRequest request, HttpServletResponse response,
            @RequestParam("classId") Integer classId) throws IOException, BusinessException {
        Boolean isLogin = (Boolean) httpServletRequest.getSession().getAttribute("IS_LOGIN");
        AdminVO loginUser =
                JSON.parseObject((String) httpServletRequest.getSession().getAttribute("LOGIN_USER"), AdminVO.class);
        if (isLogin == null || !isLogin) {
            throw new BusinessException(EmBusinessError.USER_NOT_LOGIN);
        }
        Byte accountRole = loginUser.getAccountRole();
        if (AccountRoleConstant.ACCOUNT_ROLE_SCHOOL == accountRole
                || AccountRoleConstant.ACCOUNT_ROLE_SCHOOL_DEAN == accountRole) {
            List<Integer> schoolIdList = loginUser.getSchoolId();
            if (CollectionUtils.isEmpty(schoolIdList)) {
                throw new BusinessException(EmBusinessError.SCHOOL_INFO_ERROR);
            }
            Integer schoolId = schoolIdList.get(0);
            HSSFWorkbook workbook = new HSSFWorkbook();
            HSSFSheet sheet = workbook.createSheet("学生账单表");
            List<BillDO> billDOList =
                    billDOMapper.listBillBySchoolAndClass(schoolId, classId, DeleteStatusConstant.DELETE_STATUS_STAY);
            //设置要导出的文件的名字
            String fileName = "学生账单表";
            //新增数据行，并且设置单元格数据
            int rowNum = 1;
            String[] headers =
                    {"编号", "姓名", "学校编号", "学校名称", "班级编号", "年级号", "班级号", "身份证号", "学号", "账单名称", "缴费类型名称", "缴费金额", "账单状态",
                            "支付类型", "支付流水号", "票据号", "创建时间", "支付时间", "更新时间", "截止时间", "备注"};
            //headers表示excel表中第一行的表头
            HSSFRow row = sheet.createRow(0);
            //在excel表中添加表头
            for (int i = 0; i < headers.length; i++) {
                HSSFCell cell = row.createCell(i);
                HSSFRichTextString text = new HSSFRichTextString(headers[i]);
                cell.setCellValue(text);
            }
            //在表中存放查询到的数据放入对应的列
            for (BillDO billDO : billDOList) {
                HSSFRow row1 = sheet.createRow(rowNum);
                row1.createCell(0).setCellValue(billDO.getId());
                row1.createCell(1).setCellValue(billDO.getStudentName());
                row1.createCell(2).setCellValue(billDO.getSchoolId());
                row1.createCell(3).setCellValue(billDO.getSchoolName());
                row1.createCell(4).setCellValue(billDO.getClassId());
                row1.createCell(5).setCellValue(billDO.getGradeNum());
                row1.createCell(6).setCellValue(billDO.getClassNum());
                row1.createCell(7).setCellValue(billDO.getStudentIdentity());
                row1.createCell(8).setCellValue(billDO.getStudentNo());
                row1.createCell(9).setCellValue(billDO.getBillName());
                Integer billItemId = billDO.getBillItemId();
                BillItemDO billItemDO = billItemDOMapper.selectByPrimaryKey(billItemId);
                row1.createCell(10).setCellValue(billItemDO.getItemName());
                row1.createCell(11).setCellValue(billDO.getBillAmount());
                Byte billStatus = billDO.getBillStatus();
                String statusBy;
                switch (billStatus) {
                    case 0:
                        statusBy = "未支付";
                        break;
                    case 1:
                        statusBy = "已支付";
                        break;
                    default:
                        statusBy = "未知状态";
                        break;
                }
                row1.createCell(12).setCellValue(statusBy);
                // 转型
                Byte payType = billDO.getPayType();
                String payBy;
                switch (payType) {
                    case 0:
                        payBy = "未支付";
                        break;
                    case 1:
                        payBy = "支付宝";
                        break;
                    case 2:
                        payBy = "微信";
                        break;
                    case 3:
                        payBy = "现金";
                        break;
                    case 4:
                        payBy = "POS机";
                        break;
                    case 5:
                        payBy = "其他";
                        break;
                    default:
                        payBy = "未知方式";
                        break;
                }
                String serialNum = billDO.getSerialNum();
                String billNum = billDO.getBillNum();
                row1.createCell(13).setCellValue(payBy);
                row1.createCell(14).setCellValue(serialNum);
                row1.createCell(15).setCellValue(billNum);
                Date createDate = billDO.getCreateDate();
                row1.createCell(16).setCellValue(
                        new DateTime(createDate).toString(DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss")));
                Date payDate = billDO.getPayDate();
                if (payDate == null) {
                    row1.createCell(17).setCellValue("");
                } else {
                    row1.createCell(17).setCellValue(
                            new DateTime(payDate).toString(DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss")));
                }
                Date updateDate = billDO.getUpdateDate();
                if (payDate == null) {
                    row1.createCell(18).setCellValue("");
                } else {
                    row1.createCell(18).setCellValue(
                            new DateTime(updateDate).toString(DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss")));
                }
                Date endDate = billDO.getEndDate();
                if (endDate == null) {
                    row1.createCell(19).setCellValue("");
                } else {
                    row1.createCell(19).setCellValue(
                            new DateTime(endDate).toString(DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss")));
                }
                String comment = billDO.getComment();
                if (comment == null) {
                    row1.createCell(20).setCellValue("");
                } else {
                    row1.createCell(20).setCellValue(comment);
                }
                rowNum++;
            }
            // 解决导出Excel时文件名乱码
            String agent = request.getHeader("USER-AGENT").toLowerCase();
            response.setContentType("application/vnd.ms-excel");
            String codedFileName = java.net.URLEncoder.encode(fileName, "UTF-8");
            if (agent.contains("firefox")) {
                response.setCharacterEncoding("utf-8");
                response.setHeader("content-disposition",
                        "attachment;filename=" + new String(fileName.getBytes(), "ISO8859-1") + ".xls");
            } else {
                response.setHeader("content-disposition", "attachment;filename=" + codedFileName + ".xls");
            }
            //            response.setContentType("application/octet-stream");
            //            response.setHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode(fileName, "utf-8"));
            response.flushBuffer();
            workbook.write(response.getOutputStream());
        } else {
            throw new BusinessException(EmBusinessError.ACCESS_VIOLATION_ERROR);
        }
    }

    /**
     * 导出学生账单excel表模板
     */
    @RequestMapping(value = "/export/template", method = RequestMethod.GET)
    public void exportBillTemplate(HttpServletRequest request, HttpServletResponse response)
            throws IOException, BusinessException {
        Boolean isLogin = (Boolean) httpServletRequest.getSession().getAttribute("IS_LOGIN");
        AdminVO loginUser =
                JSON.parseObject((String) httpServletRequest.getSession().getAttribute("LOGIN_USER"), AdminVO.class);
        if (isLogin == null || !isLogin) {
            throw new BusinessException(EmBusinessError.USER_NOT_LOGIN);
        }
        Byte accountRole = loginUser.getAccountRole();
        if (AccountRoleConstant.ACCOUNT_ROLE_SCHOOL == accountRole
                || AccountRoleConstant.ACCOUNT_ROLE_SCHOOL_DEAN == accountRole) {
            List<Integer> schoolIdList = loginUser.getSchoolId();
            if (CollectionUtils.isEmpty(schoolIdList)) {
                throw new BusinessException(EmBusinessError.SCHOOL_INFO_ERROR);
            }
            HSSFWorkbook workbook = new HSSFWorkbook();
            HSSFSheet sheet = workbook.createSheet("学生账单表");
            //设置要导出的文件的名字
            String fileName = "学生账单表(模板)" + ".xls";
            //新增数据行，并且设置单元格数据
            int rowNum = 1;
            String[] headers = {"学号", "缴费类型编号", "缴费金额", "备注"};
            //headers表示excel表中第一行的表头
            HSSFRow row = sheet.createRow(0);
            //在excel表中添加表头
            for (int i = 0; i < headers.length; i++) {
                HSSFCell cell = row.createCell(i);
                HSSFRichTextString text = new HSSFRichTextString(headers[i]);
                cell.setCellValue(text);
            }
            // 解决导出Excel时文件名乱码
            String agent = request.getHeader("USER-AGENT").toLowerCase();
            response.setContentType("application/vnd.ms-excel");
            String codedFileName = java.net.URLEncoder.encode(fileName, "UTF-8");
            if (agent.contains("firefox")) {
                response.setCharacterEncoding("utf-8");
                response.setHeader("content-disposition",
                        "attachment;filename=" + new String(fileName.getBytes(), "ISO8859-1") + ".xls");
            } else {
                response.setHeader("content-disposition", "attachment;filename=" + codedFileName + ".xls");
            }
            //            response.setContentType("application/octet-stream");
            //            response.setHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode(fileName, "utf-8"));
            response.flushBuffer();
            workbook.write(response.getOutputStream());
        } else {
            throw new BusinessException(EmBusinessError.ACCESS_VIOLATION_ERROR);
        }
    }



    private BillVO convertVOFromModel(BillModel billModel) {
        if (billModel == null) {
            return null;
        }
        BillVO billVO = new BillVO();
        BeanUtils.copyProperties(billModel, billVO);
        Integer schoolId = billModel.getSchoolId();
        Integer classId = billModel.getClassId();
        SchoolModel schoolModel = schoolService.getSchoolById(schoolId);
        billVO.setSchoolName(schoolModel.getSchoolName());
        ClassModel classModel = classService.getClassById(classId);
        billVO.setClassName(classModel.getGradeNum().toString() + "(" + classModel.getClassNum().toString() + ")班");
        BillItemDO billItemDO = billService.getBillItem(billModel.getBillItemId());
        billVO.setBillItemName(billItemDO.getItemName());
        billVO.setCreateDate(billModel.getCreateDate().toString(DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss")));
        DateTime payDate = billModel.getPayDate();
        if (payDate != null) {
            billVO.setPayDate(billModel.getPayDate().toString(DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss")));
        }
        DateTime updateDate = billModel.getUpdateDate();
        if (updateDate != null) {
            billVO.setUpdateDate(billModel.getUpdateDate().toString(DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss")));
        }
        DateTime endDate = billModel.getEndDate();
        if (endDate != null) {
            billVO.setEndDate(billModel.getEndDate().toString(DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss")));
        }
        return billVO;
    }

    private BillUnpaidVO convertUnpaidVOFromModel(BillModel billModel) {
        if (billModel == null) {
            return null;
        }
        BillUnpaidVO billUnpaidVO = new BillUnpaidVO();
        BeanUtils.copyProperties(billModel, billUnpaidVO);
        Integer schoolId = billModel.getSchoolId();
        Integer classId = billModel.getClassId();
        SchoolModel schoolModel = schoolService.getSchoolById(schoolId);
        billUnpaidVO.setSchoolName(schoolModel.getSchoolName());
        ClassModel classModel = classService.getClassById(classId);
        billUnpaidVO
                .setClassName(classModel.getGradeNum().toString() + "(" + classModel.getClassNum().toString() + ")班");
        BillItemDO billItemDO = billService.getBillItem(billModel.getBillItemId());
        billUnpaidVO.setBillItemName(billItemDO.getItemName());
        return billUnpaidVO;
    }

    // List集合去重
    private List removeDuplicate(List list) {
        List listTemp = new ArrayList();
        for (int i = 0; i < list.size(); i++) {
            if (!listTemp.contains(list.get(i))) {
                listTemp.add(list.get(i));
            }
        }
        return listTemp;
    }

    // 将以逗号分隔的字符串转换成List
    private List<Integer> splitString(String string) {
        // 接收包含账单id的字符串，并将它分割成字符串数组
        String[] idString = string.split(",");
        // 将字符串数组转为List<Integer> 类型
        List<Integer> list = new ArrayList<>();
        for (String str : idString) {
            list.add(new Integer(str));
        }
        return list;
    }
}
