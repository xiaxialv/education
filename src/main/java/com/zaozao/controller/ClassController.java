package com.zaozao.controller;

import com.alibaba.fastjson.JSON;
import com.zaozao.Constant.AccountRoleConstant;
import com.zaozao.Constant.DeleteStatusConstant;
import com.zaozao.controller.viewobject.AdminVO;
import com.zaozao.controller.viewobject.ClassVO;
import com.zaozao.dao.ClassDOMapper;
import com.zaozao.dataobject.ClassDO;
import com.zaozao.error.BusinessException;
import com.zaozao.error.EmBusinessError;
import com.zaozao.response.CommonReturnType;
import com.zaozao.service.ClassService;
import com.zaozao.service.IsvService;
import com.zaozao.service.SchoolService;
import com.zaozao.service.StudentService;
import com.zaozao.service.model.ClassModel;
import com.zaozao.service.model.IsvModel;
import com.zaozao.service.model.SchoolModel;
import org.apache.poi.hssf.usermodel.*;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Sidney 2019-01-10.
 */
@Controller("classController")
@RequestMapping("/class")
@CrossOrigin(allowCredentials = "true", origins = "*")
public class ClassController extends BaseController {
    @Autowired
    private ClassService classService;

    @Autowired
    private HttpServletRequest httpServletRequest;

    @Autowired
    private StudentService studentService;

    @Autowired
    private SchoolService schoolService;

    @Autowired
    private ClassDOMapper classDOMapper;

    @Autowired
    private IsvService isvService;


    /**
     * Model model,@RequestParam(defaultValue = "1", value = "pageNum") Integer pageNum
     */
    @RequestMapping(value = "/get", method = {RequestMethod.GET})
    @ResponseBody
    public CommonReturnType getClassList() throws Exception {
        Boolean isLogin = (Boolean) httpServletRequest.getSession().getAttribute("IS_LOGIN");
        AdminVO loginUser =
                JSON.parseObject((String) httpServletRequest.getSession().getAttribute("LOGIN_USER"), AdminVO.class);
        List<ClassVO> classVOList = null;
        //PageHelper.startPage(pageNum, PageSizeConstant.PAGE_SIZE_CLASS);
        if (isLogin == null || !isLogin) {
            throw new BusinessException(EmBusinessError.USER_NOT_LOGIN);
        }
        Byte accountRole = loginUser.getAccountRole();
        if (accountRole == AccountRoleConstant.ACCOUNT_ROLE_SUPER_ADMIN) {
            List<ClassModel> classModelList = classService.listClass(DeleteStatusConstant.DELETE_STATUS_STAY);
            classVOList = classModelList.stream().map(this::convertVOFromModel).collect(Collectors.toList());
        } else if (accountRole == AccountRoleConstant.ACCOUNT_ROLE_SCHOOL
                || accountRole == AccountRoleConstant.ACCOUNT_ROLE_SCHOOL_FINACIAL
                || accountRole == AccountRoleConstant.ACCOUNT_ROLE_SCHOOL_DEAN) {
            List<Integer> schoolIdList = loginUser.getSchoolId();
            if (CollectionUtils.isEmpty(schoolIdList)) {
                throw new BusinessException(EmBusinessError.SCHOOL_INFO_ERROR);
            }
            Integer schoolId = schoolIdList.get(0);
            List<ClassModel> classModelList =
                    classService.listClassBySchoolId(schoolId, DeleteStatusConstant.DELETE_STATUS_STAY);
            classVOList = classModelList.stream().map(this::convertVOFromModel).collect(Collectors.toList());
        } else if (accountRole == AccountRoleConstant.ACCOUNT_ROLE_ISV) {
            List<Integer> schoolIdList = loginUser.getSchoolId();
            if (CollectionUtils.isEmpty(schoolIdList)) {
                throw new BusinessException(EmBusinessError.SCHOOL_INFO_ERROR);
            }
            List<ClassModel> classModelList =
                    classService.listClassBySchoolIds(schoolIdList, DeleteStatusConstant.DELETE_STATUS_STAY);
            classVOList = classModelList.stream().map(this::convertVOFromModel).collect(Collectors.toList());

        } else {
            throw new BusinessException(EmBusinessError.ACCESS_VIOLATION_ERROR);
        }
        //PageInfo<ClassVO> pageInfo = new PageInfo<>(classVOList);
        //model.addAttribute("pageInfo", pageInfo);
        return CommonReturnType.create(classVOList);
    }

    @RequestMapping(value = "/dynamic", method = {RequestMethod.GET})
    @ResponseBody
    public CommonReturnType getSchoolGradeClass() throws Exception {
        Boolean isLogin = (Boolean) httpServletRequest.getSession().getAttribute("IS_LOGIN");
        AdminVO loginUser =
                JSON.parseObject((String) httpServletRequest.getSession().getAttribute("LOGIN_USER"), AdminVO.class);
        List<ClassVO> classVOList = null;
        //PageHelper.startPage(pageNum, PageSizeConstant.PAGE_SIZE_CLASS);
        if (isLogin == null || !isLogin) {
            throw new BusinessException(EmBusinessError.USER_NOT_LOGIN);
        }
        Byte accountRole = loginUser.getAccountRole();
        if (accountRole == AccountRoleConstant.ACCOUNT_ROLE_SUPER_ADMIN) {
            List<ClassModel> classModelList = classService.listClass(DeleteStatusConstant.DELETE_STATUS_STAY);
            classVOList = classModelList.stream().map(this::convertVOFromModel).collect(Collectors.toList());
        } else if (accountRole == AccountRoleConstant.ACCOUNT_ROLE_SCHOOL
                || accountRole == AccountRoleConstant.ACCOUNT_ROLE_SCHOOL_FINACIAL
                || accountRole == AccountRoleConstant.ACCOUNT_ROLE_SCHOOL_DEAN) {
            List<Integer> schoolIdList = loginUser.getSchoolId();
            if (CollectionUtils.isEmpty(schoolIdList)) {
                throw new BusinessException(EmBusinessError.SCHOOL_INFO_ERROR);
            }
            Integer schoolId = schoolIdList.get(0);
            List<ClassModel> classModelList =
                    classService.listClassBySchoolId(schoolId, DeleteStatusConstant.DELETE_STATUS_STAY);
            classVOList = classModelList.stream().map(this::convertVOFromModel).collect(Collectors.toList());
        } else if (accountRole == AccountRoleConstant.ACCOUNT_ROLE_ISV) {
            List<Integer> schoolIdList = loginUser.getSchoolId();
            if (CollectionUtils.isEmpty(schoolIdList)) {
                throw new BusinessException(EmBusinessError.SCHOOL_INFO_ERROR);
            }
            for (Integer schoolId : schoolIdList) {
                List<ClassModel> classModelList =
                        classService.listClassBySchoolId(schoolId, DeleteStatusConstant.DELETE_STATUS_STAY);
                classVOList = classModelList.stream().map(this::convertVOFromModel).collect(Collectors.toList());
            }
        } else {
            throw new BusinessException(EmBusinessError.ACCESS_VIOLATION_ERROR);
        }
        //PageInfo<ClassVO> pageInfo = new PageInfo<>(classVOList);
        //model.addAttribute("pageInfo", pageInfo);
        return CommonReturnType.create(classVOList);
    }

    @RequestMapping(value = "/create", method = {RequestMethod.POST}, consumes = {CONTENT_TYPE_FORMED})
    @ResponseBody
    public CommonReturnType createClass(@RequestParam(value = "gradeNum") Integer gradeNum,
            @RequestParam(value = "classNum") Integer classNum,
            @RequestParam(value = "headTeacherName") String headTeacherName) throws Exception {
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
            SchoolModel schoolById = schoolService.getSchoolById(schoolId);
            Integer schoolSystem = schoolById.getSchoolSystem();
            int result = classDOMapper.selectAmountByGradeAndClassAndSchoolId(gradeNum, classNum, schoolId,
                    DeleteStatusConstant.DELETE_STATUS_STAY);
            if (gradeNum > schoolSystem) {
                throw new BusinessException(EmBusinessError.CLASS_SYSTEM_ERROR);
            } else if (result > 0) {
                throw new BusinessException(EmBusinessError.CLASS_EXIST_ERROR);
            } else {
                ClassModel classModel = new ClassModel();
                classModel.setGradeNum(gradeNum);
                classModel.setClassNum(classNum);
                classModel.setHeadTeacherName(headTeacherName);
                classModel.setSchoolId(schoolId);
                classService.createClass(classModel);
            }
        } else {
            throw new BusinessException(EmBusinessError.ACCESS_VIOLATION_ERROR);
        }
        return CommonReturnType.create(null);
    }

    @RequestMapping(value = "/update", method = {RequestMethod.POST}, consumes = {CONTENT_TYPE_FORMED})
    @ResponseBody
    public CommonReturnType updateClassById(@RequestParam(value = "id") Integer id,
            @RequestParam(value = "schoolId") Integer schoolId, @RequestParam(value = "gradeNum") Integer gradeNum,
            @RequestParam(value = "classNum") Integer classNum,
            @RequestParam(value = "headTeacherName") String headTeacherName) throws Exception {
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
            } else if (!schoolIdList.contains(schoolId)) {
                throw new BusinessException(EmBusinessError.ACCESS_VIOLATION_ERROR);
            } else {
                ClassModel classModel = new ClassModel();
                classModel.setId(id);
                classModel.setGradeNum(gradeNum);
                classModel.setClassNum(classNum);
                classModel.setHeadTeacherName(headTeacherName);
                classService.updateClass(classModel);
            }
        } else {
            throw new BusinessException(EmBusinessError.ACCESS_VIOLATION_ERROR);
        }
        return CommonReturnType.create(null);
    }

    /**
     * 班级一键升级,将当前学校的最高gradeNum对应的所有classId软删除
     */
    @RequestMapping(value = "/upgrade", method = {RequestMethod.GET})
    @ResponseBody
    public CommonReturnType upgradeClassById() throws Exception {
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
            } else {
                // 获得当前学校的学制,如6年制
                Integer schoolId = schoolIdList.get(0);
                SchoolModel schoolModel = schoolService.getSchoolById(schoolId);
                Integer schoolSystem = schoolModel.getSchoolSystem();
                classService.upgradeClass(schoolId, schoolSystem, DeleteStatusConstant.DELETE_STATUS_STAY);
            }
        } else {
            throw new BusinessException(EmBusinessError.ACCESS_VIOLATION_ERROR);
        }
        return CommonReturnType.create(null);
    }

    @RequestMapping(value = "/delete", method = {RequestMethod.POST}, consumes = {CONTENT_TYPE_FORMED})
    @ResponseBody
    public CommonReturnType deleteClassById(@RequestParam(value = "id") Integer id,
            @RequestParam(value = "schoolId") Integer schoolId) throws Exception {
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
            } else if (!schoolIdList.get(0).equals(schoolId)) {
                throw new BusinessException(EmBusinessError.ACCESS_VIOLATION_ERROR);
            } else {
                // 根据班级是否有学生选择硬删除或软删除
                classService.softDeleteClass(id);
            }
        } else {
            throw new BusinessException(EmBusinessError.ACCESS_VIOLATION_ERROR);
        }
        return CommonReturnType.create(null);
    }

    /**
     * 导入班级excel表
     *
     * @ PostMapping("/import")
     */
    @RequestMapping(value = "/import", method = {RequestMethod.POST}, consumes = {CONTENT_TYPE_FILE})
    @ResponseBody
    public boolean importClass(@RequestParam("file") MultipartFile file) throws BusinessException {
        Boolean isLogin = (Boolean) httpServletRequest.getSession().getAttribute("IS_LOGIN");
        AdminVO loginUser =
                JSON.parseObject((String) httpServletRequest.getSession().getAttribute("LOGIN_USER"), AdminVO.class);
        boolean result = false;
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
            String fileName = file.getOriginalFilename();
            try {
                result = classService.batchImport(fileName, file, schoolId);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return result;
        } else {
            throw new BusinessException(EmBusinessError.ACCESS_VIOLATION_ERROR);
        }
    }

    /**
     * 导出班级excel表
     */
    @RequestMapping(value = "/export", method = RequestMethod.GET)
    public void exportClass(HttpServletRequest request, HttpServletResponse response)
            throws IOException, BusinessException {
        Boolean isLogin = (Boolean) httpServletRequest.getSession().getAttribute("IS_LOGIN");
        AdminVO loginUser =
                JSON.parseObject((String) httpServletRequest.getSession().getAttribute("LOGIN_USER"), AdminVO.class);
        if (isLogin == null || !isLogin) {
            throw new BusinessException(EmBusinessError.USER_NOT_LOGIN);
        }
        Byte accountRole = loginUser.getAccountRole();
        if (AccountRoleConstant.ACCOUNT_ROLE_SCHOOL == accountRole
                || AccountRoleConstant.ACCOUNT_ROLE_SCHOOL_DEAN == accountRole
                || AccountRoleConstant.ACCOUNT_ROLE_SCHOOL_FINACIAL == accountRole) {
            List<Integer> schoolIdList = loginUser.getSchoolId();
            if (CollectionUtils.isEmpty(schoolIdList)) {
                throw new BusinessException(EmBusinessError.SCHOOL_INFO_ERROR);
            }
            HSSFWorkbook workbook = new HSSFWorkbook();
            HSSFSheet sheet = workbook.createSheet("班级信息表");
            List<ClassDO> classDOList =
                    classDOMapper.listClassBySchoolId(schoolIdList.get(0), DeleteStatusConstant.DELETE_STATUS_STAY);
            // 设置要导出的文件的名字
            String fileName = "班级信息表";
            // 清除首部的空白行
            response.reset();
            // 新增数据行，并且设置单元格数据
            int rowNum = 1;
            String[] headers = {"班级编号", "年级号", "班级号", "学校编号", "班主任姓名"};
            // headers表示excel表中第一行的表头
            HSSFRow row = sheet.createRow(0);
            // 在excel表中添加表头
            for (int i = 0; i < headers.length; i++) {
                HSSFCell cell = row.createCell(i);
                HSSFRichTextString text = new HSSFRichTextString(headers[i]);
                cell.setCellValue(text);
            }
            // 在表中存放查询到的数据放入对应的列
            for (ClassDO classDO : classDOList) {
                HSSFRow row1 = sheet.createRow(rowNum);
                row1.createCell(0).setCellValue(classDO.getId());
                row1.createCell(1).setCellValue(classDO.getGradeNum());
                row1.createCell(2).setCellValue(classDO.getClassNum());
                row1.createCell(3).setCellValue(classDO.getSchoolId());
                row1.createCell(4).setCellValue(classDO.getHeadTeacherName());
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
            //            response.setContentType("application/octet-stream");以流的形式下载文件,这样可以实现任意格式的文件下载
            //            response.setHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode(fileName, "utf-8"));
            response.flushBuffer();
            workbook.write(response.getOutputStream());
        } else {
            throw new BusinessException(EmBusinessError.ACCESS_VIOLATION_ERROR);
        }
    }


    /**
     * 导出班级excel表模板
     */
    @RequestMapping(value = "/export/template", method = RequestMethod.GET)
    public void exportClassTemplate(HttpServletRequest request, HttpServletResponse response)
            throws IOException, BusinessException {
        Boolean isLogin = (Boolean) httpServletRequest.getSession().getAttribute("IS_LOGIN");
        AdminVO loginUser =
                JSON.parseObject((String) httpServletRequest.getSession().getAttribute("LOGIN_USER"), AdminVO.class);
        if (isLogin == null || !isLogin) {
            throw new BusinessException(EmBusinessError.USER_NOT_LOGIN);
        }
        Byte accountRole = loginUser.getAccountRole();
        if (AccountRoleConstant.ACCOUNT_ROLE_SCHOOL == accountRole
                || AccountRoleConstant.ACCOUNT_ROLE_SCHOOL_DEAN == accountRole
                || AccountRoleConstant.ACCOUNT_ROLE_SCHOOL_FINACIAL == accountRole) {
            List<Integer> schoolIdList = loginUser.getSchoolId();
            if (CollectionUtils.isEmpty(schoolIdList)) {
                throw new BusinessException(EmBusinessError.SCHOOL_INFO_ERROR);
            }
            HSSFWorkbook workbook = new HSSFWorkbook();
            HSSFSheet sheet = workbook.createSheet("班级信息表");
            // 设置要导出的文件的名字
            String fileName = "班级信息表(模板)";
            // 清除首部的空白行
            response.reset();
            // 新增数据行，并且设置单元格数据
            int rowNum = 1;
            String[] headers = {"年级号", "班级号", "班主任姓名"};
            // headers表示excel表中第一行的表头
            HSSFRow row = sheet.createRow(0);
            // 在excel表中添加表头
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
            //            response.setContentType("application/octet-stream");以流的形式下载文件,这样可以实现任意格式的文件下载
            //            response.setHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode(fileName, "utf-8"));
            response.flushBuffer();
            workbook.write(response.getOutputStream());
        } else {
            throw new BusinessException(EmBusinessError.ACCESS_VIOLATION_ERROR);
        }
    }

    /**
     * schoolId-->schoolName +isvName +classSize
     */
    private ClassVO convertVOFromModel(ClassModel classModel) {
        if (classModel == null) {
            return null;
        }
        ClassVO classVO = new ClassVO();
        BeanUtils.copyProperties(classModel, classVO);
        SchoolModel schoolById = schoolService.getSchoolById(classModel.getSchoolId());
        IsvModel isvById = isvService.getIsvById(schoolById.getIsvId());
        classVO.setIsvName(isvById.getCompanyName());
        classVO.setClassSize(
                studentService.studentAmountByClass(classModel.getId(), DeleteStatusConstant.DELETE_STATUS_STAY));
        classVO.setSchoolName(schoolById.getSchoolName());
        return classVO;
    }
}
