package com.zaozao.service.impl;

import com.zaozao.Constant.DeleteStatusConstant;
import com.zaozao.dao.BillDOMapper;
import com.zaozao.dao.ClassDOMapper;
import com.zaozao.dao.StudentDOMapper;
import com.zaozao.dataobject.ClassDO;
import com.zaozao.dataobject.StudentDO;
import com.zaozao.error.BusinessException;
import com.zaozao.error.EmBusinessError;
import com.zaozao.service.BillService;
import com.zaozao.service.StudentService;
import com.zaozao.service.model.StudentModel;
import com.zaozao.validator.ValidationResult;
import com.zaozao.validator.ValidatorImpl;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.NumberToTextConverter;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Sidney 2019-01-10.
 */
@Service
public class StudentServiceImpl implements StudentService {

    @Autowired
    private StudentDOMapper studentDOMapper;

    @Autowired
    private ValidatorImpl validator;

    @Autowired
    private BillService billService;

    @Autowired
    private BillDOMapper billDOMapper;

    @Autowired
    private ClassDOMapper classDOMapper;

    @Override
    public StudentModel createStudent(StudentModel studentModel) throws BusinessException {
        //校验入参
        ValidationResult result = validator.validate(studentModel);
        if (result.isHasError()) {
            throw new BusinessException(EmBusinessError.PARAM_VALIDATION_ERROR, result.getErrMsg());
        }
        //转化Model-->dataobject
        StudentDO studentDO = this.convertDoFromItemModel(studentModel);
        //写入数据库
        studentDOMapper.insertSelective(studentDO);
        studentModel.setId(studentDO.getId());
        //返回创建完成的对象
        return this.getStudentByPrimaryKey(studentModel.getId());
    }

    @Override
    @Transactional(propagation = Propagation.SUPPORTS)
    public List<StudentModel> listStudentBySchoolAndClass(Integer schoolId, Integer classId, String deleteStatus) {
        List<StudentDO> studentDOList = studentDOMapper.listStudentBySchoolAndClass(schoolId, classId, deleteStatus);
        List<StudentModel> studentModelList = studentDOList.stream().map(studentDO -> {
            StudentModel studentModel = this.convertModelFromDataObject(studentDO);
            return studentModel;
        }).collect(Collectors.toList());
        return studentModelList;
    }

    @Override
    @Transactional(propagation = Propagation.SUPPORTS)
    public List<StudentModel> listStudent(String deleteStatus) {
        List<StudentDO> studentDOList = studentDOMapper.listStudent(deleteStatus);
        List<StudentModel> studentModelList = studentDOList.stream().map(studentDO -> {
            StudentModel studentModel = this.convertModelFromDataObject(studentDO);
            return studentModel;
        }).collect(Collectors.toList());
        return studentModelList;
    }

    // 所有角色动态查询公用一个方法,使用标记区分角色
    @Override
    public List<StudentModel> listStudentByDynamicQuery(Map<String, Object> requestMap) {
        // 得到前台传过来的值,如果前台传递的值为空则查询该条件的所有
        String name = requestMap.get("name").toString();
        String gender = requestMap.get("gender").toString();
        String studentNo = requestMap.get("studentNo").toString();
        // 传入年级-班级需转化成classId,在classDOMapper中动态查询一次符合条件的classIdList
        // 不传入参数NumberFormatException
        String gradeNum = requestMap.get("gradeNum").toString();
        String classNum = requestMap.get("classNum").toString();
        String schoolId = requestMap.get("schoolId").toString();
        String residence = requestMap.get("residence").toString();
        String schoolShuttle = requestMap.get("schoolShuttle").toString();
        String deleteStatus = requestMap.get("deleteStatus").toString();
        List<StudentModel> studentModelList = null;
        // 标记不同角色,转型错误
        Byte adminAuthorized = (Byte) requestMap.get("adminAuthorized");
        Integer schoolIdAuthorized = (Integer) requestMap.get("schoolIdAuthorized");
        List<Integer> schoolIdListAuthorized = (List<Integer>) requestMap.get("schoolIdListAuthorized");
        boolean emptyAllParam =
                "".equals(name) && "".equals(gender) && "".equals(studentNo) && "".equals(gradeNum) && ""
                        .equals(classNum) && "".equals(schoolId) && "".equals(residence) && "".equals(schoolShuttle);
        boolean emptySGCParam = "".equals(gradeNum) && "".equals(classNum) && "".equals(schoolId);
        if (adminAuthorized != null && adminAuthorized == 0) {
            // 超管动态查询学生
            if (emptyAllParam) {
                studentModelList = this.listStudent(deleteStatus);
            } else {
                List<ClassDO> classDOList = classDOMapper.listClassByDynamicQuery(requestMap);
                if (classDOList.size()>0) {
                    List<Integer> classIdList = classDOList.stream().map(ClassDO::getId).collect(Collectors.toList());
                    requestMap.put("classIdList", classIdList);
                } else {
                    Integer classId=0;
                    requestMap.put("classId", classId.toString());
                }
                List<StudentDO> studentDOList = studentDOMapper.listStudentByDynamicQuery(requestMap);
                studentModelList = this.changeDOList(studentDOList);
            }
        } else if (schoolIdAuthorized != null && schoolIdAuthorized > 0) {
            // 学校动态查询学生
            if (emptyAllParam) {
                // 什么参数都不填返回该学校所有学生
                studentModelList = this.listStudentBySchool(schoolIdAuthorized, deleteStatus);
            } else {
                // 填写了查询条件,需要默认将学校id传入:
                requestMap.put("schoolId", schoolIdAuthorized);
                // 学校-年级-班级填写了任一个值,就进入,默认在该学校下所有班级中查找学生,需要将该学校所有classIdList存入条件
                // 学校-年级-班级均不填写,就根据该学校id返回该学校下所有的classIdlIst
                //                    List<Integer> classIdList = new ArrayList<>();
                //                    classIdList.add(schoolIdAuthorized);
                List<ClassDO> classDOList = classDOMapper.listClassByDynamicQuery(requestMap);
                List<Integer> classIdList = classDOList.stream().map(ClassDO::getId).collect(Collectors.toList());
                requestMap.put("classIdList", classIdList);
                List<StudentDO> studentDOList = studentDOMapper.listStudentByDynamicQuery(requestMap);
                studentModelList = this.changeDOList(studentDOList);
            }
        } else if (schoolIdListAuthorized != null && schoolIdListAuthorized.size() > 0) {
            // isv动态查询学生
            if (emptyAllParam) {
                studentModelList = this.listStudentBySchoolIdList(schoolIdListAuthorized, deleteStatus);
            } else {
                if ("".equals(schoolId)) {
                    requestMap.put("schoolIdList", schoolIdListAuthorized);
                }
                // 如果查询条件里classIdList为空
                List<ClassDO> classDOList = classDOMapper.listClassByDynamicQuery(requestMap);
                if (classDOList.size()>0) {
                    List<Integer> classIdList = classDOList.stream().map(ClassDO::getId).collect(Collectors.toList());
                    requestMap.put("classIdList", classIdList);
                } else {
                    Integer classId=0;
                    requestMap.put("classId", classId);
                }
                List<StudentDO> studentDOList = studentDOMapper.listStudentByDynamicQuery(requestMap);
                studentModelList = this.changeDOList(studentDOList);
            }
        }
        return studentModelList;
    }

    @Override
    @Transactional(propagation = Propagation.SUPPORTS)
    public List<StudentModel> listStudentBySchool(Integer schoolId, String deleteStatus) {
        List<StudentDO> studentDOList = studentDOMapper.listStudentBySchool(schoolId, deleteStatus);
        List<StudentModel> studentModelList = studentDOList.stream().map(studentDO -> {
            StudentModel studentModel = this.convertModelFromDataObject(studentDO);
            return studentModel;
        }).collect(Collectors.toList());
        return studentModelList;
    }

    @Override
    @Transactional(propagation = Propagation.SUPPORTS)
    public List<StudentModel> listStudentBySchoolIdList(List<Integer> schoolIdList, String deleteStatus) {
        List<StudentDO> studentDOList = studentDOMapper.listStudentBySchoolIdList(schoolIdList, deleteStatus);
        List<StudentModel> studentModelList = studentDOList.stream().map(studentDO -> {
            StudentModel studentModel = this.convertModelFromDataObject(studentDO);
            return studentModel;
        }).collect(Collectors.toList());
        return studentModelList;
    }

    @Override
    @Transactional(propagation = Propagation.SUPPORTS)
    public StudentModel getStudentBySchoolAndClassAndStudentNo(Integer schoolId, Integer classId, String studentNo,
            String deleteStatus) {
        StudentDO studentDO =
                studentDOMapper.getStudentBySchoolAndClassAndStudentNo(schoolId, classId, studentNo, deleteStatus);
        StudentModel studentModel = this.convertModelFromDataObject(studentDO);
        return studentModel;
    }

    @Override
    @Transactional(propagation = Propagation.SUPPORTS)
    public StudentModel getStudentByStudentNo(Integer schoolId, String studentNo, String deleteStatus) {
        StudentDO studentDO = studentDOMapper.getStudentByStudentNo(schoolId, studentNo, deleteStatus);
        if (studentDO == null) {
            return null;
        }
        StudentModel studentModel = this.convertModelFromDataObject(studentDO);
        return studentModel;
    }

    @Override
    @Transactional(propagation = Propagation.SUPPORTS)
    public StudentModel getStudentByPrimaryKey(Integer id) {
        StudentDO studentDO = studentDOMapper.selectByPrimaryKey(id);
        StudentModel studentModel = this.convertModelFromDataObject(studentDO);
        return studentModel;
    }


    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
    public StudentModel updateStudentInfo(StudentModel studentModel, String deleteStatus) {
        StudentDO studentDO = this.convertDoFromItemModel(studentModel);
        studentDOMapper.updateByPrimaryKeySelective(studentDO);
        return this.getStudentByPrimaryKey(studentModel.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
    public void softDeleteStudent(Integer id) {
        // 将学生记录软删除,账单记录标记delete_status(判断是否有账单记录有则将账单记录软删除)
        this.softDelStudent(id);
        int selectResult = billDOMapper.selectAmountByStudentId(id, DeleteStatusConstant.DELETE_STATUS_STAY);
        if (selectResult > 0) {
            billService.softDeleteBillByStudentId(id);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
    public void softDeleteStudentBatch(List<Integer> idList) {
        // 学生批量软删除
        List<StudentDO> studentDOList = studentDOMapper.selectByPrimaryKeyList(idList);
        for (StudentDO studentDO : studentDOList) {
            this.softDeleteStudent(studentDO.getId());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
    public void softDeleteStudentByClassIdList(List<Integer> classIdList) {
        List<StudentDO> studentDOList =
                studentDOMapper.listStudentByClassIdList(classIdList, DeleteStatusConstant.DELETE_STATUS_STAY);
        for (StudentDO studentDO : studentDOList) {
            this.softDelStudent(studentDO.getId());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
    public void deleteStudentBySchoolId(Integer schoolId) {
        studentDOMapper.deleteStudentBySchoolId(schoolId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
    public boolean batchImport(String fileName, MultipartFile file, Integer schoolId, Integer classId)
            throws Exception {
        boolean notNull = false;
        List<StudentDO> studentDOList = new ArrayList<>();
        if (!fileName.matches("^.+\\.(?i)(xls)$") && !fileName.matches("^.+\\.(?i)(xlsx)$")) {
            throw new BusinessException(EmBusinessError.FILE_TYPE_ERROR);
        }
        boolean isExcel2003 = true;
        if (fileName.matches("^.+\\.(?i)(xlsx)$")) {
            isExcel2003 = false;
        }
        InputStream is = file.getInputStream();
        Workbook wb;
        if (isExcel2003) {
            wb = new HSSFWorkbook(is);
        } else {
            wb = new XSSFWorkbook(is);
        }
        Sheet sheet = wb.getSheetAt(0);
        if (sheet != null) {
            notNull = true;
        }
        StudentDO studentDO;
        for (int r = 1; r <= sheet.getLastRowNum(); r++) {
            Row row = sheet.getRow(r);
            if (row == null) {
                continue;
            }
            studentDO = new StudentDO();
            if (row.getCell(0).getCellType() != 1) {
                throw new BusinessException(
                        (EmBusinessError.EXCEL_ROW_TYPE_ERROR.setErrMsg("导入失败(第" + (r + 1) + "行," + "姓名请设为文本格式)")));
            }
            String name = row.getCell(0).getStringCellValue();
            if (name == null || name.isEmpty()) {
                throw new BusinessException(
                        EmBusinessError.EXCEL_ROW_TYPE_ERROR.setErrMsg("导入失败(第" + (r + 1) + "行,姓名未填写)"));
            }
            //            row.getCell(1).setCellType(Cell.CELL_TYPE_STRING);
            String gender = NumberToTextConverter.toText(row.getCell(1).getNumericCellValue());
            if (gender == null || gender.isEmpty()) {
                throw new BusinessException(EmBusinessError.EXCEL_ROW_TYPE_ERROR
                        .setErrMsg("导入失败(第" + (r + 1) + "行,性别未填写," + "请输入0或1,1表示男/0表示女)"));
            }
            String studentIdentity = null;
            if (row.getCell(2) != null) {
                row.getCell(2).setCellType(Cell.CELL_TYPE_STRING);
                studentIdentity = row.getCell(2).getStringCellValue();
            }
            if (studentIdentity == null || studentIdentity.isEmpty()) {
                throw new BusinessException(
                        EmBusinessError.EXCEL_ROW_TYPE_ERROR.setErrMsg("导入失败(第" + (r + 1) + "行,身份证号未填写)"));
            }
            String studentNo = null;
            if (row.getCell(3) != null) {
                row.getCell(3).setCellType(Cell.CELL_TYPE_STRING);
                studentNo = row.getCell(3).getStringCellValue();
            }
            if (studentNo == null || studentNo.isEmpty()) {
                throw new BusinessException(
                        EmBusinessError.EXCEL_ROW_TYPE_ERROR.setErrMsg("导入失败(第" + (r + 1) + "行,学号未填写)"));
            }

            //            String classId = NumberToTextConverter.toText(row.getCell(3).getNumericCellValue());
            //            if(classId==null || classId.isEmpty()){
            //                throw new BusinessException(EmBusinessError.EXCEL_ROW_TYPE_ERROR.setErrMsg("导入失败(第"+(r+1)+"行,班级编号未填写)"));
            //            }
            //            String schoolId = NumberToTextConverter.toText(row.getCell(4).getNumericCellValue());
            //            if(schoolId==null || schoolId.isEmpty()){
            //                throw new BusinessException(EmBusinessError.EXCEL_ROW_TYPE_ERROR.setErrMsg("导入失败(第"+(r+1)+"行,学校编号未填写)"));
            //            }
            String parentPhoneNumber = null;
            if (row.getCell(4) != null) {
                row.getCell(4).setCellType(Cell.CELL_TYPE_STRING);
                parentPhoneNumber = row.getCell(4).getStringCellValue();
            }
            if (parentPhoneNumber == null || parentPhoneNumber.isEmpty()) {
                throw new BusinessException(
                        EmBusinessError.EXCEL_ROW_TYPE_ERROR.setErrMsg("导入失败(第" + (r + 1) + "行," + "父母手机号未填写)"));
            }
            String parentName = row.getCell(5).getStringCellValue();
            if (parentName == null || parentName.isEmpty()) {
                throw new BusinessException(
                        EmBusinessError.EXCEL_ROW_TYPE_ERROR.setErrMsg("导入失败(第" + (r + 1) + "行,父母姓名未填写)"));
            }
            String guardian = row.getCell(6).getStringCellValue();
            if (guardian == null || guardian.isEmpty()) {
                throw new BusinessException(
                        EmBusinessError.EXCEL_ROW_TYPE_ERROR.setErrMsg("导入失败(第" + (r + 1) + "行,监护人未填写)"));
            }
            String residence = NumberToTextConverter.toText(row.getCell(7).getNumericCellValue());
            if (residence == null || residence.isEmpty()) {
                throw new BusinessException(EmBusinessError.EXCEL_ROW_TYPE_ERROR
                        .setErrMsg("导入失败(第" + (r + 1) + "行,是否住校未填写," + "请输入0或1,0表示不住校/1表示住校)"));
            }
            String schoolShuttle = NumberToTextConverter.toText(row.getCell(8).getNumericCellValue());
            if (schoolShuttle == null || schoolShuttle.isEmpty()) {
                throw new BusinessException(EmBusinessError.EXCEL_ROW_TYPE_ERROR
                        .setErrMsg("导入失败(第" + (r + 1) + "行,是否校车接送未填写," + "请输入0或1,0表示不需要校车接送/1表示需要校车接送)"));
            }

            //            //日期
            //            Date date;
            //            if(row.getCell(3).getCellType() !=0){
            //                throw new MyException("导入失败(第"+(r+1)+"行,入职日期格式不正确或未填写)");
            //            }else{
            //                date = row.getCell(3).getDateCellValue();
            //            }
            //            String des = row.getCell(4).getStringCellValue();

            studentDO.setName(name);
            studentDO.setGender(new Byte(gender));
            studentDO.setStudentIdentity(studentIdentity);
            studentDO.setStudentNo(studentNo);
            studentDO.setClassId(classId);
            studentDO.setSchoolId(schoolId);
            studentDO.setParentPhoneNum(parentPhoneNumber);
            studentDO.setParentName(parentName);
            studentDO.setGuardian(guardian);
            studentDO.setResidence(new Byte(residence));
            studentDO.setSchoolShuttle(new Byte(schoolShuttle));
            studentDOList.add(studentDO);
        }
        for (StudentDO studentRecord : studentDOList) {
            String studentNo = studentRecord.getStudentNo();
            int selectResult = studentDOMapper
                    .selectAmountByStudentNo(schoolId, classId, studentNo, DeleteStatusConstant.DELETE_STATUS_STAY);
            if (selectResult == 0) {
                // 不存在则进行增加
                studentDOMapper.insertSelective(studentRecord);
            } else {
                // 已存在则进行更新
                StudentDO studentByStudentNo = studentDOMapper
                        .getStudentByStudentNo(schoolId, studentNo, DeleteStatusConstant.DELETE_STATUS_STAY);
                studentRecord.setId(studentByStudentNo.getId());
                studentDOMapper.updateByPrimaryKeySelective(studentRecord);
            }
        }
        return notNull;
    }

    @Override
    @Transactional(propagation = Propagation.SUPPORTS)
    public Integer studentAmountByClass(Integer classId, String deleteStatus) {
        return studentDOMapper.selectAmountByClass(classId, deleteStatus);
    }


    private StudentModel convertModelFromDataObject(StudentDO studentDO) {
        StudentModel studentModel = new StudentModel();
        BeanUtils.copyProperties(studentDO, studentModel);
        return studentModel;
    }

    private StudentDO convertDoFromItemModel(StudentModel studentModel) {
        if (studentModel == null) {
            return null;
        }
        StudentDO studentDO = new StudentDO();
        BeanUtils.copyProperties(studentModel, studentDO);
        return studentDO;
    }

    private void softDelStudent(Integer id) {
        // 软删除
        StudentDO studentDO = studentDOMapper.selectByPrimaryKey(id);
        studentDO.setDeleteStatus(DeleteStatusConstant.createDeleteStatus());
        studentDOMapper.updateByPrimaryKeySelective(studentDO);
    }

    private List<StudentModel> changeDOList(List<StudentDO> studentDOList) {
        return studentDOList.stream().map(this::convertModelFromDataObject).collect(Collectors.toList());
    }

}
