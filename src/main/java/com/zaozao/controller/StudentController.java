package com.zaozao.controller;

import com.alibaba.fastjson.JSON;
import com.zaozao.Constant.AccountRoleConstant;
import com.zaozao.Constant.DeleteStatusConstant;
import com.zaozao.controller.viewobject.AdminVO;
import com.zaozao.controller.viewobject.StudentVO;
import com.zaozao.dao.StudentDOMapper;
import com.zaozao.dataobject.StudentDO;
import com.zaozao.error.BusinessException;
import com.zaozao.error.EmBusinessError;
import com.zaozao.response.CommonReturnType;
import com.zaozao.service.ClassService;
import com.zaozao.service.SchoolService;
import com.zaozao.service.StudentService;
import com.zaozao.service.model.ClassModel;
import com.zaozao.service.model.SchoolModel;
import com.zaozao.service.model.StudentModel;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Sidney 2019-01-10.
 */
@Controller("studentController")
@RequestMapping("/student")
@CrossOrigin(allowCredentials = "true", origins = "*")
public class StudentController extends BaseController {
    @Autowired
    private StudentDOMapper studentDOMapper;

    @Autowired
    private StudentService studentService;

    @Autowired
    private HttpServletRequest httpServletRequest;

    @Autowired
    private SchoolService schoolService;

    @Autowired
    private ClassService classService;

    @RequestMapping(value = "/get", method = {RequestMethod.GET})
    @ResponseBody
    public CommonReturnType getStudent(@RequestParam("schoolId") Integer schoolId,
            @RequestParam("classId") Integer classId, @RequestParam("studentNo") String studentNo) {
        StudentModel studentModel = studentService.getStudentBySchoolAndClassAndStudentNo(schoolId, classId, studentNo,
                DeleteStatusConstant.DELETE_STATUS_STAY);
        StudentVO studentVO = this.convertVOFromModel(studentModel);
        return CommonReturnType.create(studentVO);
    }

    /**
     * Model model,@RequestParam(defaultValue = "1", value = "pageNum") Integer pageNum
     */
    @RequestMapping(value = "/get/list", method = {RequestMethod.GET})
    @ResponseBody
    public CommonReturnType getStudentList() throws Exception {
        Boolean isLogin = (Boolean) httpServletRequest.getSession().getAttribute("IS_LOGIN");
        AdminVO loginUser =
                JSON.parseObject((String) httpServletRequest.getSession().getAttribute("LOGIN_USER"), AdminVO.class);
        List<StudentVO> studentVOList;
        //PageHelper.startPage(pageNum, PageSizeConstant.PAGE_SIZE_STUDENT);
        if (isLogin == null || !isLogin) {
            throw new BusinessException(EmBusinessError.USER_NOT_LOGIN);
        }
        Byte accountRole = loginUser.getAccountRole();
        if (accountRole == AccountRoleConstant.ACCOUNT_ROLE_SUPER_ADMIN) {
            List<StudentModel> studentModelList = studentService.listStudent(DeleteStatusConstant.DELETE_STATUS_STAY);
            studentVOList = studentModelList.stream().map(this::convertVOFromModel).collect(Collectors.toList());
        } else if (accountRole == AccountRoleConstant.ACCOUNT_ROLE_SCHOOL
                || accountRole == AccountRoleConstant.ACCOUNT_ROLE_SCHOOL_FINACIAL
                || accountRole == AccountRoleConstant.ACCOUNT_ROLE_SCHOOL_DEAN) {
            List<Integer> schoolIdList = loginUser.getSchoolId();
            if (CollectionUtils.isEmpty(schoolIdList)) {
                throw new BusinessException(EmBusinessError.SCHOOL_INFO_ERROR);
            }
            List<StudentModel> studentModelList =
                    studentService.listStudentBySchool(schoolIdList.get(0), DeleteStatusConstant.DELETE_STATUS_STAY);
            studentVOList = studentModelList.stream().map(this::convertVOFromModel).collect(Collectors.toList());
        } else if (accountRole == AccountRoleConstant.ACCOUNT_ROLE_ISV) {
            List<Integer> schoolIdList = loginUser.getSchoolId();
            if (CollectionUtils.isEmpty(schoolIdList)) {
                throw new BusinessException(EmBusinessError.ACCESS_VIOLATION_ERROR);
            }
            List<StudentModel> studentModelList =
                    studentService.listStudentBySchoolIdList(schoolIdList, DeleteStatusConstant.DELETE_STATUS_STAY);
            studentVOList = studentModelList.stream().map(this::convertVOFromModel).collect(Collectors.toList());
        } else {
            throw new BusinessException(EmBusinessError.ACCESS_VIOLATION_ERROR);
        }
        //PageInfo<StudentVO> pageInfo = new PageInfo<>(studentVOList);
        //model.addAttribute("pageInfo", pageInfo);
        return CommonReturnType.create(studentVOList);
    }



    /**
     * 动态查询学生记录
     * @param requestMap 查询条件
     * @return 学生记录集合
     * @throws Exception 异常
     */
    @RequestMapping(value = "/query/list", method = {RequestMethod.POST}, headers = "Accept=application/json")
    @ResponseBody
    public CommonReturnType queryStudentList(@RequestBody Map<String, Object> requestMap) throws Exception {
        Boolean isLogin = (Boolean) httpServletRequest.getSession().getAttribute("IS_LOGIN");
        AdminVO loginUser =
                JSON.parseObject((String) httpServletRequest.getSession().getAttribute("LOGIN_USER"), AdminVO.class);
        List<StudentVO> studentVOList;
        //PageHelper.startPage(pageNum, PageSizeConstant.PAGE_SIZE_STUDENT);
        if (isLogin == null || !isLogin) {
            throw new BusinessException(EmBusinessError.USER_NOT_LOGIN);
        }
        Byte accountRole = loginUser.getAccountRole();
        requestMap.put("deleteStatus", DeleteStatusConstant.DELETE_STATUS_STAY);
        if (accountRole == AccountRoleConstant.ACCOUNT_ROLE_SUPER_ADMIN) {
            requestMap.put("adminAuthorized", AccountRoleConstant.ACCOUNT_ROLE_SUPER_ADMIN);
            List<StudentModel> studentModelList = studentService.listStudentByDynamicQuery(requestMap);
            if (studentModelList == null) {
                return CommonReturnType.create(null);
            } else {
                studentVOList = studentModelList.stream().map(this::convertVOFromModel).collect(Collectors.toList());
            }
        } else if (accountRole == AccountRoleConstant.ACCOUNT_ROLE_SCHOOL
                || accountRole == AccountRoleConstant.ACCOUNT_ROLE_SCHOOL_FINACIAL
                || accountRole == AccountRoleConstant.ACCOUNT_ROLE_SCHOOL_DEAN) {
            List<Integer> schoolIdList = loginUser.getSchoolId();
            if (CollectionUtils.isEmpty(schoolIdList)) {
                throw new BusinessException(EmBusinessError.SCHOOL_INFO_ERROR);
            }
            Integer schoolId = schoolIdList.get(0);
            requestMap.put("schoolIdAuthorized", schoolId);
            List<StudentModel> studentModelList = studentService.listStudentByDynamicQuery(requestMap);
            if (studentModelList == null) {
                return CommonReturnType.create(null);
            } else {
                studentVOList = studentModelList.stream().map(this::convertVOFromModel).collect(Collectors.toList());
            }
        } else if (accountRole == AccountRoleConstant.ACCOUNT_ROLE_ISV) {
            List<Integer> schoolIdList = loginUser.getSchoolId();
            if (CollectionUtils.isEmpty(schoolIdList)) {
                throw new BusinessException(EmBusinessError.ACCESS_VIOLATION_ERROR);
            }
            requestMap.put("schoolIdListAuthorized", schoolIdList);
            List<StudentModel> studentModelList = studentService.listStudentByDynamicQuery(requestMap);
            if (studentModelList == null) {
                return CommonReturnType.create(null);
            } else {
                studentVOList = studentModelList.stream().map(this::convertVOFromModel).collect(Collectors.toList());
            }
        } else {
            throw new BusinessException(EmBusinessError.ACCESS_VIOLATION_ERROR);
        }
        //PageInfo<StudentVO> pageInfo = new PageInfo<>(studentVOList);
        //model.addAttribute("pageInfo", pageInfo);
        return CommonReturnType.create(studentVOList);
    }

    @RequestMapping(value = "/update", method = {RequestMethod.POST}, consumes = {CONTENT_TYPE_FORMED})
    @ResponseBody
    public CommonReturnType updateStudentInfo(@RequestParam("id") Integer id,
            @RequestParam("studentIdentity") String studentIdentity, @RequestParam("studentNo") String studentNo,
            @RequestParam("name") String name, @RequestParam("gender") Byte gender,
            @RequestParam("classId") Integer classId, @RequestParam("schoolId") Integer schoolId,
            @RequestParam("parentPhoneNum") String parentPhoneNum, @RequestParam("parentName") String parentName,
            @RequestParam("guardian") String guardian, @RequestParam("residence") Byte residence,
            @RequestParam("schoolShuttle") Byte schoolShuttle) throws Exception {
        Boolean isLogin = (Boolean) httpServletRequest.getSession().getAttribute("IS_LOGIN");
        AdminVO loginUser =
                JSON.parseObject((String) httpServletRequest.getSession().getAttribute("LOGIN_USER"), AdminVO.class);
        StudentVO studentVO;
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
                StudentModel studentModel = new StudentModel();
                studentModel.setId(id);
                studentModel.setStudentIdentity(studentIdentity);
                studentModel.setStudentNo(studentNo);
                studentModel.setName(name);
                studentModel.setGender(gender);
                studentModel.setSchoolId(schoolId);
                studentModel.setClassId(classId);
                studentModel.setParentPhoneNum(parentPhoneNum);
                studentModel.setParentName(parentName);
                studentModel.setGuardian(guardian);
                studentModel.setResidence(residence);
                studentModel.setSchoolShuttle(schoolShuttle);
                StudentModel studentModelReturn =
                        studentService.updateStudentInfo(studentModel, DeleteStatusConstant.DELETE_STATUS_STAY);
                studentVO = this.convertVOFromModel(studentModelReturn);
            }
        } else {
            throw new BusinessException(EmBusinessError.ACCESS_VIOLATION_ERROR);
        }
        return CommonReturnType.create(studentVO);
    }

    @RequestMapping(value = "/create", method = {RequestMethod.POST}, consumes = {CONTENT_TYPE_FORMED})
    @ResponseBody
    public CommonReturnType createStudent(@RequestParam("studentIdentity") String studentIdentity,
            @RequestParam("studentNo") String studentNo,
            @RequestParam("name") String name, @RequestParam("gender") Byte gender,
            @RequestParam("classId") Integer classId, @RequestParam("parentPhoneNum") String parentPhoneNum,
            @RequestParam("parentName") String parentName, @RequestParam("guardian") String guardian,
            @RequestParam("residence") Byte residence, @RequestParam("schoolShuttle") Byte schoolShuttle)
            throws Exception {
        Boolean isLogin = (Boolean) httpServletRequest.getSession().getAttribute("IS_LOGIN");
        AdminVO loginUser =
                JSON.parseObject((String) httpServletRequest.getSession().getAttribute("LOGIN_USER"), AdminVO.class);
        StudentVO studentVO;
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
                ClassModel classById = classService.getClassById(classId);
                if (classById == null) {
                    throw new BusinessException(EmBusinessError.CLASS_NUEXIST_ERROR);
                }
                StudentModel studentModel = new StudentModel();
                studentModel.setStudentIdentity(studentIdentity);
                studentModel.setStudentNo(studentNo);
                studentModel.setName(name);
                studentModel.setGender(gender);
                studentModel.setClassId(classId);
                studentModel.setSchoolId(schoolIdList.get(0));
                studentModel.setParentPhoneNum(parentPhoneNum);
                studentModel.setParentName(parentName);
                studentModel.setGuardian(guardian);
                studentModel.setResidence(residence);
                studentModel.setSchoolShuttle(schoolShuttle);
                StudentModel studentModelReturn = studentService.createStudent(studentModel);
                studentVO = this.convertVOFromModel(studentModelReturn);
            }
        } else {
            throw new BusinessException(EmBusinessError.ACCESS_VIOLATION_ERROR);
        }
        return CommonReturnType.create(studentVO);
    }

    /**
     * 做软删除
     * @param id       学生编号
     * @param schoolId 学校编号
     */
    @RequestMapping(value = "/delete", method = {RequestMethod.POST}, consumes = {CONTENT_TYPE_FORMED})
    @ResponseBody
    public CommonReturnType deleteStudent(@RequestParam("id") Integer id, @RequestParam("schoolId") Integer schoolId)
            throws Exception {
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
                // 将学生记录软删除,账单记录标记delete_status
                studentService.softDeleteStudent(id);
            }
        } else {
            throw new BusinessException(EmBusinessError.ACCESS_VIOLATION_ERROR);
        }
        return CommonReturnType.create(null);
    }

    /**
     * 做批量软删除
     * @param ids      学生编号拼接的字符串
     * @param schoolId 学校编号
     */
    @RequestMapping(value = "/delete/batch", method = {RequestMethod.POST}, consumes = {CONTENT_TYPE_FORMED})
    @ResponseBody
    public CommonReturnType deleteStudentBatch(@RequestParam("ids") String ids,
            @RequestParam("schoolId") Integer schoolId) throws Exception {
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
                // 接收包含账单id的字符串，并将它分割成字符串数组
                String[] idString = ids.split(",");
                // 将字符串数组转为List<Integer> 类型
                List<Integer> idList = new ArrayList<Integer>();
                for (String str : idString) {
                    idList.add(new Integer(str));
                }
                // 调用service层的批量删除函数将学生记录软删除,账单记录标记delete_status
                studentService.softDeleteStudentBatch(idList);
            }
        } else {
            throw new BusinessException(EmBusinessError.ACCESS_VIOLATION_ERROR);
        }
        return CommonReturnType.create(null);
    }

    /**
     * 导入学生excel表
     */
    @RequestMapping(value = "/import", method = {RequestMethod.POST}, consumes = {CONTENT_TYPE_FILE})
    @ResponseBody
    public boolean importStudent(@RequestParam("file") MultipartFile file, @RequestParam("classId") Integer classId)
            throws BusinessException {
        boolean a = false;
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
            String fileName = file.getOriginalFilename();
            try {
                a = studentService.batchImport(fileName, file, schoolId, classId);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return a;
        } else {
            throw new BusinessException(EmBusinessError.ACCESS_VIOLATION_ERROR);
        }
    }


    /**
     * 导出学生excel表
     */
    @RequestMapping(value = "/export", method = RequestMethod.GET)
    public void exportStudent(HttpServletRequest request, HttpServletResponse response,
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
            HSSFSheet sheet = workbook.createSheet("学生信息表");
            List<StudentDO> studentDOListList = studentDOMapper
                    .listStudentBySchoolAndClass(schoolId, classId, DeleteStatusConstant.DELETE_STATUS_STAY);
            //根据classId查询班级-年级
            ClassModel classById = classService.getClassById(classId);
            String className = classById.getGradeNum().toString() + "(" + classById.getClassNum().toString() + ")班";
            //设置要导出的文件的名字
            String fileName = className + "学生信息表";
            //新增数据行，并且设置单元格数据
            int rowNum = 1;
            String[] headers = {"编号", "姓名", "性别", "身份证号","学号", "班级", "学校编号", "父母手机号", "父母姓名", "监护人", "是否住校", "是否接送"};
            //headers表示excel表中第一行的表头
            HSSFRow row = sheet.createRow(0);
            //在excel表中添加表头
            for (int i = 0; i < headers.length; i++) {
                HSSFCell cell = row.createCell(i);
                HSSFRichTextString text = new HSSFRichTextString(headers[i]);
                cell.setCellValue(text);
            }
            //在表中存放查询到的数据放入对应的列
            for (StudentDO studentDO : studentDOListList) {
                HSSFRow row1 = sheet.createRow(rowNum);
                row1.createCell(0).setCellValue(studentDO.getId());
                row1.createCell(1).setCellValue(studentDO.getName());
                row1.createCell(2).setCellValue(studentDO.getGender());
                row1.createCell(3).setCellValue(studentDO.getStudentIdentity());
                row1.createCell(4).setCellValue(studentDO.getStudentNo());
                row1.createCell(5).setCellValue(studentDO.getClassId());
                row1.createCell(6).setCellValue(className);
                row1.createCell(7).setCellValue(studentDO.getSchoolId());
                row1.createCell(8).setCellValue(studentDO.getParentPhoneNum());
                row1.createCell(9).setCellValue(studentDO.getParentName());
                row1.createCell(10).setCellValue(studentDO.getGuardian());
                row1.createCell(11).setCellValue(studentDO.getResidence());
                row1.createCell(12).setCellValue(studentDO.getSchoolShuttle());
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
     * 导出学生excel表模板
     */
    @RequestMapping(value = "/export/template", method = RequestMethod.GET)
    public void exportStudentTemplate(HttpServletRequest request, HttpServletResponse response)
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
            Integer schoolId = schoolIdList.get(0);
            HSSFWorkbook workbook = new HSSFWorkbook();
            HSSFSheet sheet = workbook.createSheet("学生信息表");
            //设置要导出的文件的名字
            String fileName = "学生信息表(模板)" + ".xls";
            //新增数据行，并且设置单元格数据
            int rowNum = 1;
            String[] headers = {"姓名", "性别", "身份证号","学号", "父母手机号", "父母姓名", "监护人", "是否住校", "是否接送"};
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

    private StudentVO convertVOFromModel(StudentModel studentModel) {
        if (studentModel == null) {
            return null;
        }
        StudentVO studentVO = new StudentVO();
        BeanUtils.copyProperties(studentModel, studentVO);
        SchoolModel schoolModel = schoolService.getSchoolById(studentModel.getSchoolId());
        studentVO.setSchoolName(schoolModel.getSchoolName());
        ClassModel classModel = classService.getClassById(studentModel.getClassId());
        studentVO.setClassName(classModel.getGradeNum().toString() + "(" + classModel.getClassNum().toString() + ")班");
        return studentVO;
    }
}
