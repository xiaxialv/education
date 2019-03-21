package com.zaozao.controller;

import com.alibaba.fastjson.JSON;
import com.zaozao.Constant.AccountRoleConstant;
import com.zaozao.Constant.BillConstant;
import com.zaozao.Constant.DeleteStatusConstant;
import com.zaozao.controller.viewobject.AdminVO;
import com.zaozao.controller.viewobject.BillAmountPayVO;
import com.zaozao.controller.viewobject.HomeDataVO;
import com.zaozao.dao.BillDOMapper;
import com.zaozao.dao.BillItemDOMapper;
import com.zaozao.dao.SchoolDOMapper;
import com.zaozao.dao.StudentDOMapper;
import com.zaozao.dataobject.BillAmountPayDO;
import com.zaozao.dataobject.BillItemDO;
import com.zaozao.dataobject.SchoolDO;
import com.zaozao.error.BusinessException;
import com.zaozao.error.EmBusinessError;
import com.zaozao.response.CommonReturnType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 首页数据展示
 * allowedHeaders = "*",X
 * @author chenxiaotian
 */
@Controller("indexDataController")
@RequestMapping("/home")
@CrossOrigin(allowCredentials = "true",origins = "*")
public class IndexDataController extends BaseController {

    @Autowired
    private HttpServletRequest httpServletRequest;

    @Autowired
    private BillItemDOMapper billItemDOMapper;

    @Autowired
    private BillDOMapper billDOMapper;

    @Autowired
    private StudentDOMapper studentDOMapper;

    @Autowired
    private SchoolDOMapper schoolDOMapper;

    @RequestMapping(value = "/data", method = {RequestMethod.GET})
    @ResponseBody
    public CommonReturnType searchData() throws BusinessException {
        Boolean isLogin = (Boolean) httpServletRequest.getSession().getAttribute("IS_LOGIN");
        AdminVO loginUser =
                JSON.parseObject((String) httpServletRequest.getSession().getAttribute("LOGIN_USER"), AdminVO.class);
        if (isLogin == null || !isLogin) {
            throw new BusinessException(EmBusinessError.USER_NOT_LOGIN);
        }
        Byte accountRole = loginUser.getAccountRole();
        // VO
        HomeDataVO homeDataVO = new HomeDataVO();
        List<HomeDataVO.BillDetail> billUnpaidDetailList;
        Map<String, List<HomeDataVO.BillDetail>> unpaidBillDetailBySchool = new HashMap<>();

        List<HomeDataVO.BillDetail> billPaidDetailList;
        Map<String, List<HomeDataVO.BillDetail>> paidBillDetailBySchool = new HashMap<>();
        // 动态查询的条件集合
        Map<String, Object> map = new HashMap<>();
        map.put("deleteStatus", DeleteStatusConstant.DELETE_STATUS_STAY);
        if (accountRole == AccountRoleConstant.ACCOUNT_ROLE_SUPER_ADMIN) {
            // 查询应缴账单
            Double totalAmount = billDOMapper.countBillAmountDynamic(map);
            // 查询已缴
            map.put("billStatus", BillConstant.BILL_STATUS_PAID);
            Double receivedAmount = billDOMapper.countBillAmountDynamic(map);
            // 查询未缴
            map.put("billStatus", BillConstant.BILL_STATUS_UNPAID);
            Double unreceivedAmount = billDOMapper.countBillAmountDynamic(map);
            // 查询学生人数
            Integer studentAmount = studentDOMapper.countAmountByDynamic(map);
            homeDataVO.setTotalAmount(new BigDecimal(totalAmount));
            homeDataVO.setReceivedAmount(new BigDecimal(receivedAmount));
            homeDataVO.setUnreceivedAmount(new BigDecimal(unreceivedAmount));
            homeDataVO.setStudentAmount(studentAmount);
            // 获取可以查看所有schoolIdList
            List<SchoolDO> schoolDOList = schoolDOMapper.listSchool();
            List<Integer> schoolIdList = schoolDOList.stream().map(SchoolDO::getId).collect(Collectors.toList());
            // 获得未缴费账单明细
            for (Integer sId : schoolIdList) {
                // 获取某学校下的所有billItem对象
                map.put("schoolId", sId);
                // 获取该学校名称
                SchoolDO schoolDO = schoolDOMapper.selectByPrimaryKey(sId);
                List<BillItemDO> billItemDOList = billItemDOMapper.listItemBySchoolDynamic(map);
                if (billItemDOList != null) {
                    billUnpaidDetailList = billItemDOList.stream().map(billItemDO -> {
                        HomeDataVO.BillDetail billDetail = new HomeDataVO.BillDetail();
                        billDetail.setBillItemName(billItemDO.getItemName());
                        map.put("billItemId", billItemDO.getId());
                        Double detailAmount = billDOMapper.countBillAmountDynamic(map);
                        if (detailAmount != null) {
                            billDetail.setBillAmountByItem(new BigDecimal(detailAmount));
                        } else {
                            billDetail.setBillAmountByItem(null);
                        }
                        return billDetail;
                    }).collect(Collectors.toList());
                    unpaidBillDetailBySchool.put(sId+"-"+schoolDO.getSchoolName(), billUnpaidDetailList);
                }
            }
            homeDataVO.setUnpaidBillDetail(unpaidBillDetailBySchool);
            // 获得已缴费账单明细
            map.put("billStatus", BillConstant.BILL_STATUS_PAID);
            for (Integer sId : schoolIdList) {
                // 获取某学校下的所有billItem对象
                map.put("schoolId", sId);
                // 获取该学校名称
                SchoolDO schoolDO = schoolDOMapper.selectByPrimaryKey(sId);
                List<BillItemDO> billItemDOList = billItemDOMapper.listItemBySchoolDynamic(map);
                if (billItemDOList != null) {
                    billPaidDetailList = billItemDOList.stream().map(billItemDO -> {
                        HomeDataVO.BillDetail billDetail = new HomeDataVO.BillDetail();
                        billDetail.setBillItemName(billItemDO.getItemName());
                        map.put("billItemId", billItemDO.getId());
                        Double detailAmount = billDOMapper.countBillAmountDynamic(map);
                        if (detailAmount != null) {
                            billDetail.setBillAmountByItem(new BigDecimal(detailAmount));
                        } else {
                            billDetail.setBillAmountByItem(null);
                        }
                        return billDetail;
                    }).collect(Collectors.toList());
                    paidBillDetailBySchool.put(sId+"-"+schoolDO.getSchoolName(), billPaidDetailList);
                }
            }
            homeDataVO.setPaidBillDetail(paidBillDetailBySchool);
        } else if (accountRole == AccountRoleConstant.ACCOUNT_ROLE_SCHOOL
                || accountRole == AccountRoleConstant.ACCOUNT_ROLE_SCHOOL_FINACIAL
                || accountRole == AccountRoleConstant.ACCOUNT_ROLE_SCHOOL_DEAN) {
            List<Integer> schoolIdList = loginUser.getSchoolId();
            if (CollectionUtils.isEmpty(schoolIdList)) {
                throw new BusinessException(EmBusinessError.SCHOOL_INFO_ERROR);
            }
            map.put("schoolIdList", schoolIdList);
            // 查询应缴账单
            Double totalAmount = billDOMapper.countBillAmountDynamic(map);
            // 查询已缴
            map.put("billStatus", BillConstant.BILL_STATUS_PAID);
            Double receivedAmount = billDOMapper.countBillAmountDynamic(map);
            // 查询未缴
            map.put("billStatus", BillConstant.BILL_STATUS_UNPAID);
            Double unreceivedAmount = billDOMapper.countBillAmountDynamic(map);
            // 查询学生人数
            Integer studentAmount = studentDOMapper.countAmountByDynamic(map);
            homeDataVO.setTotalAmount(new BigDecimal(totalAmount));
            homeDataVO.setReceivedAmount(new BigDecimal(receivedAmount));
            homeDataVO.setUnreceivedAmount(new BigDecimal(unreceivedAmount));
            homeDataVO.setStudentAmount(studentAmount);
            // 获得未缴费账单明细
            for (Integer sId : schoolIdList) {
                // 获取某学校下的所有billItem对象
                map.put("schoolId", sId);
                // 获取该学校名称
                SchoolDO schoolDO = schoolDOMapper.selectByPrimaryKey(sId);
                List<BillItemDO> billItemDOList = billItemDOMapper.listItemBySchoolDynamic(map);
                if (billItemDOList != null) {
                    billUnpaidDetailList = billItemDOList.stream().map(billItemDO -> {
                        HomeDataVO.BillDetail billDetail = new HomeDataVO.BillDetail();
                        billDetail.setBillItemName(billItemDO.getItemName());
                        map.put("billItemId", billItemDO.getId());
                        Double detailAmount = billDOMapper.countBillAmountDynamic(map);
                        if (detailAmount != null) {
                            billDetail.setBillAmountByItem(new BigDecimal(detailAmount));
                        } else {
                            billDetail.setBillAmountByItem(null);
                        }
                        return billDetail;
                    }).collect(Collectors.toList());
                    unpaidBillDetailBySchool.put(sId+"-"+schoolDO.getSchoolName(), billUnpaidDetailList);
                }
            }
            homeDataVO.setUnpaidBillDetail(unpaidBillDetailBySchool);
            // 获得已缴费账单明细
            map.put("billStatus", BillConstant.BILL_STATUS_PAID);
            for (Integer sId : schoolIdList) {
                // 获取某学校下的所有billItem对象
                map.put("schoolId", sId);
                // 获取该学校名称
                SchoolDO schoolDO = schoolDOMapper.selectByPrimaryKey(sId);
                List<BillItemDO> billItemDOList = billItemDOMapper.listItemBySchoolDynamic(map);
                if (billItemDOList != null) {
                    billPaidDetailList = billItemDOList.stream().map(billItemDO -> {
                        HomeDataVO.BillDetail billDetail = new HomeDataVO.BillDetail();
                        billDetail.setBillItemName(billItemDO.getItemName());
                        map.put("billItemId", billItemDO.getId());
                        Double detailAmount = billDOMapper.countBillAmountDynamic(map);
                        if (detailAmount != null) {
                            billDetail.setBillAmountByItem(new BigDecimal(detailAmount));
                        } else {
                            billDetail.setBillAmountByItem(null);
                        }
                        return billDetail;
                    }).collect(Collectors.toList());
                    paidBillDetailBySchool.put(sId+"-"+schoolDO.getSchoolName(), billPaidDetailList);
                }
            }
            homeDataVO.setPaidBillDetail(paidBillDetailBySchool);
        } else if (accountRole == AccountRoleConstant.ACCOUNT_ROLE_ISV) {
            List<Integer> schoolIdList = loginUser.getSchoolId();
            Integer isvId = loginUser.getIsvId();
            if (isvId==null) {
                throw new BusinessException(EmBusinessError.ISV_INFO_ERROR);
            } else if (CollectionUtils.isEmpty(schoolIdList)) {
                throw new BusinessException(EmBusinessError.SCHOOL_INFO_ERROR);
            }
            map.put("schoolIdList", schoolIdList);
            // 查询应缴账单
            Double totalAmount = billDOMapper.countBillAmountDynamic(map);
            // 查询已缴
            map.put("billStatus", BillConstant.BILL_STATUS_PAID);
            Double receivedAmount = billDOMapper.countBillAmountDynamic(map);
            // 查询未缴
            map.put("billStatus", BillConstant.BILL_STATUS_UNPAID);
            Double unreceivedAmount = billDOMapper.countBillAmountDynamic(map);
            // 查询学生人数
            Integer studentAmount = studentDOMapper.countAmountByDynamic(map);
            homeDataVO.setTotalAmount(new BigDecimal(totalAmount));
            homeDataVO.setReceivedAmount(new BigDecimal(receivedAmount));
            homeDataVO.setUnreceivedAmount(new BigDecimal(unreceivedAmount));
            homeDataVO.setStudentAmount(studentAmount);
            // 获得未缴费账单明细
            for (Integer sId : schoolIdList) {
                // 获取某学校下的所有billItem对象
                map.put("schoolId", sId);
                // 获取该学校名称
                SchoolDO schoolDO = schoolDOMapper.selectByPrimaryKey(sId);
                List<BillItemDO> billItemDOList = billItemDOMapper.listItemBySchoolDynamic(map);
                if (billItemDOList != null) {
                    billUnpaidDetailList = billItemDOList.stream().map(billItemDO -> {
                        HomeDataVO.BillDetail billDetail = new HomeDataVO.BillDetail();
                        billDetail.setBillItemName(billItemDO.getItemName());
                        map.put("billItemId", billItemDO.getId());
                        Double detailAmount = billDOMapper.countBillAmountDynamic(map);
                        if (detailAmount != null) {
                            billDetail.setBillAmountByItem(new BigDecimal(detailAmount));
                        } else {
                            billDetail.setBillAmountByItem(null);
                        }
                        return billDetail;
                    }).collect(Collectors.toList());
                    unpaidBillDetailBySchool.put(sId+"-"+schoolDO.getSchoolName(), billUnpaidDetailList);
                }
            }
            homeDataVO.setUnpaidBillDetail(unpaidBillDetailBySchool);
            // 获得已缴费账单明细
            map.put("billStatus", BillConstant.BILL_STATUS_PAID);
            for (Integer sId : schoolIdList) {
                // 获取某学校下的所有billItem对象
                map.put("schoolId", sId);
                // 获取该学校名称
                SchoolDO schoolDO = schoolDOMapper.selectByPrimaryKey(sId);
                List<BillItemDO> billItemDOList = billItemDOMapper.listItemBySchoolDynamic(map);
                if (billItemDOList != null) {
                    billPaidDetailList = billItemDOList.stream().map(billItemDO -> {
                        HomeDataVO.BillDetail billDetail = new HomeDataVO.BillDetail();
                        billDetail.setBillItemName(billItemDO.getItemName());
                        map.put("billItemId", billItemDO.getId());
                        Double detailAmount = billDOMapper.countBillAmountDynamic(map);
                        if (detailAmount != null) {
                            billDetail.setBillAmountByItem(new BigDecimal(detailAmount));
                        } else {
                            billDetail.setBillAmountByItem(null);
                        }
                        return billDetail;
                    }).collect(Collectors.toList());
                    paidBillDetailBySchool.put(sId+"-"+schoolDO.getSchoolName(), billPaidDetailList);
                }
            }
            homeDataVO.setPaidBillDetail(paidBillDetailBySchool);
        } else {
            throw new BusinessException(EmBusinessError.ACCESS_VIOLATION_ERROR);
        }
        return CommonReturnType.create(homeDataVO);
    }

    @RequestMapping(value = "/pay/data", method = {RequestMethod.GET})
    @ResponseBody
    public CommonReturnType searchPayData() throws BusinessException {
        Boolean isLogin = (Boolean) httpServletRequest.getSession().getAttribute("IS_LOGIN");
        AdminVO loginUser =
                JSON.parseObject((String) httpServletRequest.getSession().getAttribute("LOGIN_USER"), AdminVO.class);
        if (isLogin == null || !isLogin) {
            throw new BusinessException(EmBusinessError.USER_NOT_LOGIN);
        }
        Byte accountRole = loginUser.getAccountRole();
        Map<String, Object> map = new HashMap<>();
        map.put("billStatus", BillConstant.BILL_STATUS_PAID);
        if (accountRole == AccountRoleConstant.ACCOUNT_ROLE_SUPER_ADMIN) {
            // 查询billStatus为1已支付的所有账单并根据payType分组,并计算各组总额
            List<BillAmountPayDO> billAmountPayDOS = billDOMapper.countBillAmountByPayType(map);
            List<BillAmountPayVO> collect =
                    billAmountPayDOS.stream().map(this::convertVOFromDO).collect(Collectors.toList());
            return CommonReturnType.create(collect);
        }
        else if (accountRole == AccountRoleConstant.ACCOUNT_ROLE_ISV) {
            Integer isvId = loginUser.getIsvId();
            List<Integer> schoolIdList = loginUser.getSchoolId();
            if (isvId==null) {
                throw new BusinessException(EmBusinessError.ISV_INFO_ERROR);
            } else if (CollectionUtils.isEmpty(schoolIdList)) {
                throw new BusinessException(EmBusinessError.SCHOOL_INFO_ERROR);
            }
            // 根据isvId-查询billStatus为1已支付的所有账单并根据payType分组,并计算各组总额
            map.put("isvId", isvId);
            List<BillAmountPayDO> billAmountPayDOS = billDOMapper.countBillAmountByPayType(map);
            List<BillAmountPayVO> collect =
                    billAmountPayDOS.stream().map(this::convertVOFromDO).collect(Collectors.toList());
            return CommonReturnType.create(collect);
        } else if (accountRole == AccountRoleConstant.ACCOUNT_ROLE_SCHOOL
                || accountRole == AccountRoleConstant.ACCOUNT_ROLE_SCHOOL_FINACIAL
                || accountRole == AccountRoleConstant.ACCOUNT_ROLE_SCHOOL_DEAN) {
            List<Integer> schoolIdList = loginUser.getSchoolId();
            if (CollectionUtils.isEmpty(schoolIdList)) {
                throw new BusinessException(EmBusinessError.SCHOOL_INFO_ERROR);
            }
            Integer schoolId = schoolIdList.get(0);
            // 根据schoolId-查询billStatus为1已支付的所有账单并根据payType分组,并计算各组总额
            map.put("schoolId", schoolId);
            List<BillAmountPayDO> billAmountPayDOS = billDOMapper.countBillAmountByPayType(map);
            List<BillAmountPayVO> collect =
                    billAmountPayDOS.stream().map(this::convertVOFromDO).collect(Collectors.toList());
            return CommonReturnType.create(collect);
        } else {
            throw new BusinessException(EmBusinessError.ACCESS_VIOLATION_ERROR);
        }
    }

    private BillAmountPayVO convertVOFromDO(BillAmountPayDO billAmountPayDO) {
        if (billAmountPayDO == null) {
            return null;
        }
        BillAmountPayVO billAmountPayVO = new BillAmountPayVO();
        billAmountPayVO.setValue(BigDecimal.valueOf(billAmountPayDO.getBillAmount()));
        Byte payType = billAmountPayDO.getPayType();
        String payTypeName;
        switch (payType) {
            case 0:
                payTypeName = "未支付";
                break;
            case 1:
                payTypeName = "支付宝";
                break;
            case 2:
                payTypeName = "微信";
                break;
            case 3:
                payTypeName = "现金";
                break;
            case 4:
                payTypeName = "POS机";
                break;
            case 5:
                payTypeName = "其他";
                break;
            default:
                payTypeName = "未知方式";
                break;
        }
        billAmountPayVO.setName(payTypeName);
        return billAmountPayVO;
    }

    // 代码复用
    private HomeDataVO homeData(List<Integer> schoolIdList){
        // 动态查询的条件集合
        Map<String, Object> map = new HashMap<>();
        // VO
        HomeDataVO homeDataVO = new HomeDataVO();
        List<HomeDataVO.BillDetail> billUnpaidDetailList;
        Map<String, List<HomeDataVO.BillDetail>> unpaidBillDetailBySchool = new HashMap<>();
        List<HomeDataVO.BillDetail> billPaidDetailList;
        Map<String, List<HomeDataVO.BillDetail>> paidBillDetailBySchool = new HashMap<>();
        map.put("schoolIdList", schoolIdList);
        // 查询应缴账单
        Double totalAmount = billDOMapper.countBillAmountDynamic(map);
        // 查询已缴
        map.put("billStatus", BillConstant.BILL_STATUS_PAID);
        Double receivedAmount = billDOMapper.countBillAmountDynamic(map);
        // 查询未缴
        map.put("billStatus", BillConstant.BILL_STATUS_UNPAID);
        Double unreceivedAmount = billDOMapper.countBillAmountDynamic(map);
        // 查询学生人数
        Integer studentAmount = studentDOMapper.countAmountByDynamic(map);
        homeDataVO.setTotalAmount(new BigDecimal(totalAmount));
        homeDataVO.setReceivedAmount(new BigDecimal(receivedAmount));
        homeDataVO.setUnreceivedAmount(new BigDecimal(unreceivedAmount));
        homeDataVO.setStudentAmount(studentAmount);
        // 获得未缴费账单明细
        for (Integer sId : schoolIdList) {
            // 获取某学校下的所有billItem对象
            map.put("schoolId", sId);
            // 获取该学校名称
            SchoolDO schoolDO = schoolDOMapper.selectByPrimaryKey(sId);
            List<BillItemDO> billItemDOList = billItemDOMapper.listItemBySchoolDynamic(map);
            if (billItemDOList != null) {
                billUnpaidDetailList = billItemDOList.stream().map(billItemDO -> {
                    HomeDataVO.BillDetail billDetail = new HomeDataVO.BillDetail();
                    billDetail.setBillItemName(billItemDO.getItemName());
                    map.put("billItemId", billItemDO.getId());
                    Double detailAmount = billDOMapper.countBillAmountDynamic(map);
                    if (detailAmount != null) {
                        billDetail.setBillAmountByItem(new BigDecimal(detailAmount));
                    } else {
                        billDetail.setBillAmountByItem(null);
                    }
                    return billDetail;
                }).collect(Collectors.toList());
                unpaidBillDetailBySchool.put(sId+"-"+schoolDO.getSchoolName(), billUnpaidDetailList);
            }
        }
        homeDataVO.setUnpaidBillDetail(unpaidBillDetailBySchool);
        // 获得已缴费账单明细
        map.put("billStatus", BillConstant.BILL_STATUS_PAID);
        for (Integer sId : schoolIdList) {
            // 获取某学校下的所有billItem对象
            map.put("schoolId", sId);
            // 获取该学校名称
            SchoolDO schoolDO = schoolDOMapper.selectByPrimaryKey(sId);
            List<BillItemDO> billItemDOList = billItemDOMapper.listItemBySchoolDynamic(map);
            if (billItemDOList != null) {
                billPaidDetailList = billItemDOList.stream().map(billItemDO -> {
                    HomeDataVO.BillDetail billDetail = new HomeDataVO.BillDetail();
                    billDetail.setBillItemName(billItemDO.getItemName());
                    map.put("billItemId", billItemDO.getId());
                    Double detailAmount = billDOMapper.countBillAmountDynamic(map);
                    if (detailAmount != null) {
                        billDetail.setBillAmountByItem(new BigDecimal(detailAmount));
                    } else {
                        billDetail.setBillAmountByItem(null);
                    }
                    return billDetail;
                }).collect(Collectors.toList());
                paidBillDetailBySchool.put(sId+"-"+schoolDO.getSchoolName(), billPaidDetailList);
            }
        }
        homeDataVO.setPaidBillDetail(paidBillDetailBySchool);
        return homeDataVO;
    }


}
