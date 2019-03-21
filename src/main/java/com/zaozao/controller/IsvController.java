package com.zaozao.controller;

import com.alibaba.fastjson.JSON;
import com.zaozao.Constant.AccountRoleConstant;
import com.zaozao.annotation.Refresh;
import com.zaozao.controller.viewobject.AdminVO;
import com.zaozao.controller.viewobject.IsvVO;
import com.zaozao.dao.IsvAlipayInfoDOMapper;
import com.zaozao.dao.IsvWechatInfoDOMapper;
import com.zaozao.dataobject.IsvAlipayInfoDOWithBLOBs;
import com.zaozao.dataobject.IsvWechatInfoDO;
import com.zaozao.error.BusinessException;
import com.zaozao.error.EmBusinessError;
import com.zaozao.response.CommonReturnType;
import com.zaozao.service.IsvService;
import com.zaozao.service.model.IsvModel;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Sidney 2019-01-14.
 */
@Controller("isvController")
@RequestMapping("/isv")
@CrossOrigin(allowCredentials = "true", origins = "*")
public class IsvController extends BaseController {
    @Autowired
    private IsvService isvService;

    @Autowired
    private HttpServletRequest httpServletRequest;

    @Autowired
    private IsvWechatInfoDOMapper isvWechatInfoDOMapper;

    @Autowired
    private IsvAlipayInfoDOMapper isvAlipayInfoDOMapper;

    /**
     * isv服务商登录后获得自己的isv记录
     * 学校登录获得自己上属isv服务商信息
     *
     * @return isvVO
     */
    @RequestMapping(value = "/get", method = {RequestMethod.GET})
    @ResponseBody
    public CommonReturnType getStudent() throws Exception {
        Boolean isLogin = (Boolean) httpServletRequest.getSession().getAttribute("IS_LOGIN");
        AdminVO loginUser =
                JSON.parseObject((String) httpServletRequest.getSession().getAttribute("LOGIN_USER"), AdminVO.class);
        IsvVO isvVO;
        if (isLogin == null || !isLogin) {
            throw new BusinessException(EmBusinessError.USER_NOT_LOGIN);
        } else if (loginUser.getAccountRole() == AccountRoleConstant.ACCOUNT_ROLE_ISV
                || loginUser.getAccountRole() == AccountRoleConstant.ACCOUNT_ROLE_SCHOOL
                || loginUser.getAccountRole() == AccountRoleConstant.ACCOUNT_ROLE_SCHOOL_FINACIAL
                || loginUser.getAccountRole() == AccountRoleConstant.ACCOUNT_ROLE_SCHOOL_DEAN) {
            Integer isvId = loginUser.getIsvId();
            if (isvId == null) {
                throw new BusinessException(EmBusinessError.ISV_INFO_ERROR);
            }
            IsvModel isvModel = isvService.getIsvById(isvId);
            isvVO = this.convertVOFromModel(isvModel);
        } else {
            throw new BusinessException(EmBusinessError.ACCESS_VIOLATION_ERROR);
        }
        return CommonReturnType.create(isvVO);
    }

    /**
     * 超级管理员获得所有isv服务商
     * Model model,@RequestParam(defaultValue = "1", value = "pageNum") Integer pageNum
     */
    @RequestMapping(value = "/get/list", method = {RequestMethod.GET})
    @ResponseBody
    public CommonReturnType getIsvList() throws Exception {
        Boolean isLogin = (Boolean) httpServletRequest.getSession().getAttribute("IS_LOGIN");
        AdminVO loginUser =
                JSON.parseObject((String) httpServletRequest.getSession().getAttribute("LOGIN_USER"), AdminVO.class);
        if (isLogin == null || !isLogin) {
            throw new BusinessException(EmBusinessError.USER_NOT_LOGIN);
        } else if (loginUser.getAccountRole() != AccountRoleConstant.ACCOUNT_ROLE_SUPER_ADMIN) {
            throw new BusinessException(EmBusinessError.ACCESS_VIOLATION_ERROR);
        }
        //PageHelper.startPage(pageNum, PageSizeConstant.PAGE_SIZE_ISV);
        List<IsvModel> isvModelList = isvService.listIsv();
        List<IsvVO> isvVOList = isvModelList.stream().map(this::convertVOFromModel).collect(Collectors.toList());
        //PageInfo<IsvVO> pageInfo = new PageInfo<>(isvVOList);
        //model.addAttribute("pageInfo", pageInfo);
        return CommonReturnType.create(isvVOList);
    }

    @RequestMapping(value = "/update", method = {RequestMethod.POST}, consumes = {CONTENT_TYPE_FORMED})
    @ResponseBody
    public CommonReturnType updateIsvInfo(@RequestParam("id") Integer id,
            @RequestParam("companyName") String companyName, @RequestParam("companyAddress") String companyAddress,
            @RequestParam("companyContact") String companyContact, @RequestParam("companyTel") String companyTel)
            throws Exception {
        Boolean isLogin = (Boolean) httpServletRequest.getSession().getAttribute("IS_LOGIN");
        AdminVO loginUser =
                JSON.parseObject((String) httpServletRequest.getSession().getAttribute("LOGIN_USER"), AdminVO.class);
        if (isLogin == null || !isLogin) {
            throw new BusinessException(EmBusinessError.USER_NOT_LOGIN);
        } else if (loginUser.getAccountRole() == AccountRoleConstant.ACCOUNT_ROLE_ISV) {
            Integer isvId = loginUser.getIsvId();
            if (isvId == null) {
                throw new BusinessException(EmBusinessError.ISV_INFO_ERROR);
            } else if (!isvId.equals(id)) {
                throw new BusinessException(EmBusinessError.ACCESS_VIOLATION_ERROR);
            } else {
                IsvModel isvModel = new IsvModel();
                isvModel.setId(id);
                isvModel.setCompanyName(companyName);
                isvModel.setCompanyAddress(companyAddress);
                isvModel.setCompanyContact(companyContact);
                isvModel.setCompanyTel(companyTel);
                isvService.updateIsv(isvModel);
            }
        } else {
            throw new BusinessException(EmBusinessError.ACCESS_VIOLATION_ERROR);
        }
        return CommonReturnType.create(null);
    }

    @RequestMapping(value = "/create", method = {RequestMethod.POST}, consumes = {CONTENT_TYPE_FORMED})
    @ResponseBody
    public CommonReturnType createIsv(@RequestParam("companyName") String companyName,
            @RequestParam("companyAddress") String companyAddress,
            @RequestParam("companyContact") String companyContact, @RequestParam("companyTel") String companyTel)
            throws Exception {
        Boolean isLogin = (Boolean) httpServletRequest.getSession().getAttribute("IS_LOGIN");
        AdminVO loginUser =
                JSON.parseObject((String) httpServletRequest.getSession().getAttribute("LOGIN_USER"), AdminVO.class);
        if (isLogin == null || !isLogin) {
            throw new BusinessException(EmBusinessError.USER_NOT_LOGIN);
        } else if (AccountRoleConstant.ACCOUNT_ROLE_ISV == loginUser.getAccountRole()) {
            //            IsvModel isvByAdminId = isvService.getIsvByAdminId(loginUser.getId());
            //            if (isvByAdminId != null) {
            //                throw new BusinessException(EmBusinessError.ISV_INFO_EXIST);
            //            } else {
            if (loginUser.getIsvId() == null) {
                IsvModel isvModel = new IsvModel();
                isvModel.setCompanyName(companyName);
                isvModel.setCompanyAddress(companyAddress);
                isvModel.setCompanyContact(companyContact);
                isvModel.setCompanyTel(companyTel);
                isvModel.setAdminId(loginUser.getId());
                IsvModel isvModelReturn = isvService.createIsv(isvModel);
                IsvVO isvVO = this.convertVOFromModel(isvModelReturn);
                loginUser.setIsvId(isvVO.getId());
                // 1.28注释由于isv登录后有学校记录却在学校接口显示为null--取消注释
                this.httpServletRequest.getSession().setAttribute("LOGIN_USER", JSON.toJSONString(loginUser));
            } else {
                // 已有isv记录则不可以重复创建
                throw new BusinessException(EmBusinessError.ISV_INFO_EXIST);
            }
        } else {
            throw new BusinessException(EmBusinessError.ACCESS_VIOLATION_ERROR);
        }
        return CommonReturnType.create(loginUser);
    }

    /**
     * 编辑isv服务商微信相关配置
     *
     * @param id 此处id为isv记录的id
     * @param mchId
     * @param appIdOfficialAccount
     * @param appSecretOfficialAccount
     * @param appIdApplet
     * @param appSecretApplet
     * @param mchKey
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/wechat/config/edit", method = {RequestMethod.POST}, consumes = {CONTENT_TYPE_FORMED})
    @ResponseBody
    public CommonReturnType isvWechatConfigEdit(@RequestParam("id") Integer id, @RequestParam("mchId") String mchId,
            @RequestParam("appIdOfficialAccount") String appIdOfficialAccount,
            @RequestParam("appSecretOfficialAccount") String appSecretOfficialAccount,
            @RequestParam("appIdApplet") String appIdApplet, @RequestParam("appSecretApplet") String appSecretApplet,
            @RequestParam("mchKey") String mchKey, @RequestParam("notifyUrl") String notifyUrl) throws Exception {
        Boolean isLogin = (Boolean) httpServletRequest.getSession().getAttribute("IS_LOGIN");
        AdminVO loginUser =
                JSON.parseObject((String) httpServletRequest.getSession().getAttribute("LOGIN_USER"), AdminVO.class);
        if (isLogin == null || !isLogin) {
            throw new BusinessException(EmBusinessError.USER_NOT_LOGIN);
        } else if (loginUser.getAccountRole() == AccountRoleConstant.ACCOUNT_ROLE_ISV) {
            Integer isvId = loginUser.getIsvId();
            if (isvId == null) {
                throw new BusinessException(EmBusinessError.ISV_INFO_ERROR);
            } else if (!isvId.equals(id)) {
                throw new BusinessException(EmBusinessError.ACCESS_VIOLATION_ERROR);
            } else {
                IsvWechatInfoDO isvWechatInfoDOExist = isvWechatInfoDOMapper.selectByIsvId(id);
                IsvWechatInfoDO isvWechatInfoDO = new IsvWechatInfoDO();
                isvWechatInfoDO.setIsvId(id);
                isvWechatInfoDO.setAppIdOfficialAccount(appIdOfficialAccount);
                isvWechatInfoDO.setAppSecretOfficialAccount(appSecretOfficialAccount);
                isvWechatInfoDO.setAppIdApplet(appIdApplet);
                isvWechatInfoDO.setAppSecretApplet(appSecretApplet);
                isvWechatInfoDO.setMchId(mchId);
                isvWechatInfoDO.setMchKey(mchKey);
                isvWechatInfoDO.setNotifyUrl(notifyUrl);
                if (isvWechatInfoDOExist != null) {
                    // 已有记录修改操作
                    isvWechatInfoDO.setId(isvWechatInfoDOExist.getId());
                    int i = isvWechatInfoDOMapper.updateByPrimaryKeySelective(isvWechatInfoDO);
                    if (i==1) {
                        return CommonReturnType.create("修改成功");
                    }
                } else {
                    // 未有记录新建操作
                    int i = isvWechatInfoDOMapper.insertSelective(isvWechatInfoDO);
                    if (i==1) {
                        return CommonReturnType.create("保存成功");
                    }
                }
            }
        } else {
            throw new BusinessException(EmBusinessError.ACCESS_VIOLATION_ERROR);
        }
        return CommonReturnType.create(null);
    }

    /**
     * 查看isv服务商微信相关配置
     *
     * @param id isv表中记录的id
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/wechat/config/search", method = {RequestMethod.POST}, consumes = {CONTENT_TYPE_FORMED})
    @ResponseBody
    public CommonReturnType isvWechatConfigSearch(@RequestParam("id") Integer id) throws Exception {
        Boolean isLogin = (Boolean) httpServletRequest.getSession().getAttribute("IS_LOGIN");
        AdminVO loginUser =
                JSON.parseObject((String) httpServletRequest.getSession().getAttribute("LOGIN_USER"), AdminVO.class);
        if (isLogin == null || !isLogin) {
            throw new BusinessException(EmBusinessError.USER_NOT_LOGIN);
        } else if (loginUser.getAccountRole() == AccountRoleConstant.ACCOUNT_ROLE_SUPER_ADMIN) {
            IsvWechatInfoDO isvWechatInfoDO = isvWechatInfoDOMapper.selectByIsvId(id);
            if (isvWechatInfoDO != null) {
                return CommonReturnType.create(isvWechatInfoDO);
            } else {
                return CommonReturnType.create(null);
            }
        } else if (loginUser.getAccountRole() == AccountRoleConstant.ACCOUNT_ROLE_ISV) {
            Integer isvId = loginUser.getIsvId();
            if (isvId == null) {
                throw new BusinessException(EmBusinessError.ISV_INFO_ERROR);
            } else if (!isvId.equals(id)) {
                throw new BusinessException(EmBusinessError.ACCESS_VIOLATION_ERROR);
            } else {
                IsvWechatInfoDO isvWechatInfoDO = isvWechatInfoDOMapper.selectByIsvId(id);
                if (isvWechatInfoDO != null) {
                    return CommonReturnType.create(isvWechatInfoDO);
                } else {
                    return CommonReturnType.create(null);
                }
            }
        } else {
            throw new BusinessException(EmBusinessError.ACCESS_VIOLATION_ERROR);
        }
    }

    /**
     * 编辑isv服务商支付宝相关配置
     *
     * @param id              此处id为isv记录的id
     * @param isvPid
     * @param appIdApplet
     * @param appPublicKey
     * @param appPrivateKey
     * @param alipayPublicKey
     * @return
     * @throws Exception
     */
    @Refresh
    @RequestMapping(value = "/alipay/config/edit", method = {RequestMethod.POST}, consumes = {CONTENT_TYPE_FORMED})
    @ResponseBody
    public CommonReturnType isvAlipayConfigEdit(@RequestParam("id") Integer id, @RequestParam("isvPid") String isvPid,
            @RequestParam("appIdApplet") String appIdApplet, @RequestParam("appPublicKey") String appPublicKey,
            @RequestParam("appPrivateKey") String appPrivateKey,
            @RequestParam("alipayPublicKey") String alipayPublicKey) throws Exception {
        Boolean isLogin = (Boolean) httpServletRequest.getSession().getAttribute("IS_LOGIN");
        AdminVO loginUser =
                JSON.parseObject((String) httpServletRequest.getSession().getAttribute("LOGIN_USER"), AdminVO.class);
        if (isLogin == null || !isLogin) {
            throw new BusinessException(EmBusinessError.USER_NOT_LOGIN);
        } else if (loginUser.getAccountRole() == AccountRoleConstant.ACCOUNT_ROLE_ISV) {
            Integer isvId = loginUser.getIsvId();
            if (isvId == null) {
                throw new BusinessException(EmBusinessError.ISV_INFO_ERROR);
            } else if (!isvId.equals(id)) {
                throw new BusinessException(EmBusinessError.ACCESS_VIOLATION_ERROR);
            } else {
                IsvAlipayInfoDOWithBLOBs isvAlipayInfoDOWithBLOBsExist = isvAlipayInfoDOMapper.selectByIsvId(id);
                IsvAlipayInfoDOWithBLOBs isvAlipayInfoDO = new IsvAlipayInfoDOWithBLOBs();
                isvAlipayInfoDO.setIsvId(id);
                isvAlipayInfoDO.setIsvPid(isvPid);
                isvAlipayInfoDO.setAppIdApplet(appIdApplet);
                isvAlipayInfoDO.setAppPublicKey(appPublicKey);
                isvAlipayInfoDO.setAppPrivateKey(appPrivateKey);
                isvAlipayInfoDO.setAlipayPublicKey(alipayPublicKey);
                if (isvAlipayInfoDOWithBLOBsExist != null) {
                    // 已有记录修改操作
                    isvAlipayInfoDO.setId(isvAlipayInfoDOWithBLOBsExist.getId());
                    int i = isvAlipayInfoDOMapper.updateByPrimaryKeySelective(isvAlipayInfoDO);
                    if (i==1) {
                        return CommonReturnType.create("修改成功");
                    }
                } else {
                    // 未有记录新建操作
                    int i = isvAlipayInfoDOMapper.insertSelective(isvAlipayInfoDO);
                    if (i==1) {
                        return CommonReturnType.create("保存成功");
                    }
                }
            }
        } else {
            throw new BusinessException(EmBusinessError.ACCESS_VIOLATION_ERROR);
        }
        // TODO 编辑成功返回保存成功
        return CommonReturnType.create(null);
    }

    /**
     * 查看isv服务商支付宝相关配置
     *
     * @param id isv表中记录的id
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/alipay/config/search", method = {RequestMethod.POST}, consumes = {CONTENT_TYPE_FORMED})
    @ResponseBody
    public CommonReturnType isvAlipayConfigSearch(@RequestParam("id") Integer id) throws Exception {
        Boolean isLogin = (Boolean) httpServletRequest.getSession().getAttribute("IS_LOGIN");
        AdminVO loginUser =
                JSON.parseObject((String) httpServletRequest.getSession().getAttribute("LOGIN_USER"), AdminVO.class);
        if (isLogin == null || !isLogin) {
            throw new BusinessException(EmBusinessError.USER_NOT_LOGIN);
        } else if (loginUser.getAccountRole() == AccountRoleConstant.ACCOUNT_ROLE_SUPER_ADMIN) {
            IsvAlipayInfoDOWithBLOBs isvAlipayInfoDOWithBLOBs = isvAlipayInfoDOMapper.selectByIsvId(id);
            if (isvAlipayInfoDOWithBLOBs != null) {
                return CommonReturnType.create(isvAlipayInfoDOWithBLOBs);
            } else {
                return CommonReturnType.create(null);
            }
        } else if (loginUser.getAccountRole() == AccountRoleConstant.ACCOUNT_ROLE_ISV) {
            Integer isvId = loginUser.getIsvId();
            if (isvId == null) {
                throw new BusinessException(EmBusinessError.ISV_INFO_ERROR);
            } else if (!isvId.equals(id)) {
                throw new BusinessException(EmBusinessError.ACCESS_VIOLATION_ERROR);
            } else {
                IsvAlipayInfoDOWithBLOBs isvAlipayInfoDOWithBLOBs = isvAlipayInfoDOMapper.selectByIsvId(id);
                if (isvAlipayInfoDOWithBLOBs != null) {
                    return CommonReturnType.create(isvAlipayInfoDOWithBLOBs);
                } else {
                    return CommonReturnType.create(null);
                }
            }
        } else {
            throw new BusinessException(EmBusinessError.ACCESS_VIOLATION_ERROR);
        }
    }

    /**
     * 删除isv记录,只有超管类型账号有权删除其下属isv记录,对应isv管理员
     * 及其学校记录,所有管理员,对应的班级.学生.账单,账单类型记录
     * 硬删除:说明该服务商退出系统使用
     *
     * @param id isv记录的id
     */
    @RequestMapping(value = "/delete", method = {RequestMethod.POST}, consumes = {CONTENT_TYPE_FORMED})
    @ResponseBody
    public CommonReturnType deleteIsv(@RequestParam("id") Integer id) throws Exception {
        Boolean isLogin = (Boolean) httpServletRequest.getSession().getAttribute("IS_LOGIN");
        AdminVO loginUser =
                JSON.parseObject((String) httpServletRequest.getSession().getAttribute("LOGIN_USER"), AdminVO.class);
        if (isLogin == null || !isLogin) {
            throw new BusinessException(EmBusinessError.USER_NOT_LOGIN);
        } else if (loginUser.getAccountRole() != AccountRoleConstant.ACCOUNT_ROLE_SUPER_ADMIN) {
            throw new BusinessException(EmBusinessError.ACCESS_VIOLATION_ERROR);
        }
        isvService.deleteIsvById(id);
        return CommonReturnType.create(null);
    }

    private IsvVO convertVOFromModel(IsvModel isvModel) {
        if (isvModel == null) {
            return null;
        }
        IsvVO isvVO = new IsvVO();
        BeanUtils.copyProperties(isvModel, isvVO);
        return isvVO;
    }

}
