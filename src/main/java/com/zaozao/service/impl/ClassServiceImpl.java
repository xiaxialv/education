package com.zaozao.service.impl;

import com.zaozao.Constant.DeleteStatusConstant;
import com.zaozao.dao.ClassDOMapper;
import com.zaozao.dataobject.ClassDO;
import com.zaozao.error.BusinessException;
import com.zaozao.error.EmBusinessError;
import com.zaozao.service.BillService;
import com.zaozao.service.ClassService;
import com.zaozao.service.StudentService;
import com.zaozao.service.model.ClassModel;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
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
import java.util.stream.Collectors;

/**
 * @author Sidney 2019-01-10.
 */
@Service
public class ClassServiceImpl implements ClassService {

    @Autowired
    private ClassDOMapper classDOMapper;

    @Autowired
    private StudentService studentService;

    @Autowired
    private BillService billService;

    @Override
    @Transactional(rollbackFor = Exception.class,propagation = Propagation.REQUIRED)
    public void createClass(ClassModel classModel) {
        ClassDO classDO = this.convertDoFromModel(classModel);
        classDOMapper.insertSelective(classDO);
        return;
    }

    @Override
    @Transactional(propagation = Propagation.SUPPORTS)
    public List<ClassModel> listClassBySchoolId(Integer schoolId,String deleteStatus) {
        List<ClassDO> classDOList = classDOMapper.listClassBySchoolId(schoolId,deleteStatus);
        List<ClassModel> classModelList = classDOList.stream().map(classDO -> {
            ClassModel classModel = this.convertModelFromDataObject(classDO);
            return classModel;
        }).collect(Collectors.toList());
        return classModelList;
    }

    @Override
    public List<ClassModel> listClassBySchoolIds(List<Integer> schoolIds, String deleteStatus) {
        List<ClassDO> classDOList = classDOMapper.listClassBySchoolIds(schoolIds,deleteStatus);
        List<ClassModel> classModelList = classDOList.stream().map(classDO -> {
            ClassModel classModel = this.convertModelFromDataObject(classDO);
            return classModel;
        }).collect(Collectors.toList());
        return classModelList;
    }

    @Override
    @Transactional(propagation = Propagation.SUPPORTS)
    public List<ClassModel> listClass(String deleteStatus) {
        List<ClassDO> classDOList = classDOMapper.listClass(deleteStatus);
        List<ClassModel> classModelList = classDOList.stream().map(classDO -> {
            ClassModel classModel = this.convertModelFromDataObject(classDO);
            return classModel;
        }).collect(Collectors.toList());
        return classModelList;
    }

    @Override
    @Transactional(propagation = Propagation.SUPPORTS)
    public ClassModel getClassById(Integer id) {
        ClassDO classDO = classDOMapper.selectByPrimaryKey(id);
        if (classDO==null) {
            return null;
        }
        ClassModel classModel = this.convertModelFromDataObject(classDO);
        return classModel;

    }

    @Override
    @Transactional(rollbackFor = Exception.class,propagation = Propagation.REQUIRED)
    public void updateClass(ClassModel classModel) {
        ClassDO classDO = this.convertDoFromModel(classModel);
        classDOMapper.updateByPrimaryKeySelective(classDO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class,propagation = Propagation.REQUIRED)
    public void upgradeClass(Integer schoolId,Integer schoolSystem,String deleteStatus) {
        // 根据学校编号和grade_num=学制查询出所有未删除状态下的即将毕业的班级,并对其进行软删除
        List<ClassDO> classDOListGraduate = classDOMapper.selectWhenGraduate(schoolId, schoolSystem, deleteStatus);
        List<Integer> classIdListGraduate = classDOListGraduate.stream().map(ClassDO::getId).collect(Collectors.toList());
        for (Integer id : classIdListGraduate) {
            this.softDelClass(id);
        }
        studentService.softDeleteStudentByClassIdList(classIdListGraduate);
        billService.softDeleteBillByClassIdList(classIdListGraduate);
        // 根据学校编号和grade_num<学制查询出所有未删除状态下的即将升级的班级,并对其grade_num进行升级
        List<ClassDO> classDOListUpgrade = classDOMapper.selectWhenUpgrade(schoolId, schoolSystem, deleteStatus);
        List<Integer> classIdListUpgrade = classDOListUpgrade.stream().map(ClassDO::getId).collect(Collectors.toList());
        for (Integer id : classIdListUpgrade) {
            ClassDO classDO = classDOMapper.selectByPrimaryKey(id);
            Integer gradeNum = classDO.getGradeNum()+1;
            classDO.setGradeNum(gradeNum);
            classDOMapper.updateByPrimaryKeySelective(classDO);
        }

    }

    @Override
    @Transactional(rollbackFor = Exception.class,propagation = Propagation.REQUIRED)
    public void softDeleteClass(Integer id) {
        // 将学生数量为0的班级可以进行硬删除,
        Integer classSize = studentService.studentAmountByClass(id, DeleteStatusConstant.DELETE_STATUS_STAY);
        if (classSize == 0) {
            this.deleteClass(id);
        } else {
            // 学生数量不为0的班级进行软删除,
            this.softDelClass(id);
            // 将学生记录,账单记录标记delete_status
            List<Integer> classIdList = new ArrayList<>();
            classIdList.add(id);
            studentService.softDeleteStudentByClassIdList(classIdList);
            billService.softDeleteBillByClassIdList(classIdList);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class,propagation = Propagation.REQUIRED)
    public void deleteClass(Integer id) {
        classDOMapper.deleteByPrimaryKey(id);
    }


    @Override
    @Transactional(rollbackFor = Exception.class,propagation = Propagation.REQUIRED)
    public void deleteClassBySchoolId(Integer schoolId) {
        classDOMapper.deleteBySchoolId(schoolId);
    }



    @Override
    @Transactional(rollbackFor = Exception.class,propagation = Propagation.REQUIRED)
    public boolean batchImport(String fileName, MultipartFile file,Integer schoolId) throws Exception {
        boolean notNull = false;
        List<ClassDO> classDOList = new ArrayList<ClassDO>();
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
        ClassDO classDO;
        for (int r = 1; r <= sheet.getLastRowNum(); r++) {
            Row row = sheet.getRow(r);
            if (row == null) {
                continue;
            }
            classDO = new ClassDO();
            //            if( row.getCell(0).getCellType() !=1){
            //                throw new BusinessException((EmBusinessError.EXCEL_ROWTYPE_ERROR.setErrMsg("导入失败(第"+(r+1)+"行,"
            //                        + "班级号请设为文本格式)")));
            //            }

            // 将数字类型的单元格取出来转成String再转成Integer
            Integer gradeNum = Integer.valueOf(NumberToTextConverter.toText(row.getCell(0).getNumericCellValue()));
            if (gradeNum == null || gradeNum == 0) {
                throw new BusinessException(
                        EmBusinessError.EXCEL_ROW_TYPE_ERROR.setErrMsg("导入失败(第" + (r + 1) + "行,年级号未填写且不能为0)"));
            }
            Integer classNum = Integer.valueOf(NumberToTextConverter.toText(row.getCell(1).getNumericCellValue()));
            if (classNum == null || classNum == 0) {
                throw new BusinessException(
                        EmBusinessError.EXCEL_ROW_TYPE_ERROR.setErrMsg("导入失败(第" + (r + 1) + "行,班级号未填写且不能为0)"));
            }
//            Integer schoolId = Integer.valueOf(NumberToTextConverter.toText(row.getCell(2).getNumericCellValue()));
//            if (schoolId == null || schoolId == 0) {
//                throw new BusinessException(
//                        EmBusinessError.EXCEL_ROW_TYPE_ERROR.setErrMsg("导入失败(第" + (r + 1) + "行,学校编号未填写且不能为0)"));
//            }
            String headTeacherName = row.getCell(2).getStringCellValue();
            if (headTeacherName == null || headTeacherName.isEmpty()) {
                throw new BusinessException(
                        EmBusinessError.EXCEL_ROW_TYPE_ERROR.setErrMsg("导入失败(第" + (r + 1) + "行,班主任姓名未填写)"));
            }
            classDO.setGradeNum(gradeNum);
            classDO.setClassNum(classNum);
            classDO.setSchoolId(schoolId);
            classDO.setHeadTeacherName(headTeacherName);
            classDOList.add(classDO);
        }
        for (ClassDO classRecord : classDOList) {
            Integer gradeNum = classRecord.getGradeNum();
            Integer classNum = classRecord.getClassNum();
            int selectResult = classDOMapper.selectAmountByGradeAndClassAndSchoolId(gradeNum, classNum, schoolId,
                    DeleteStatusConstant.DELETE_STATUS_STAY);
            if (selectResult == 0) {
                //不存在则进行增加
                classDOMapper.insertSelective(classRecord);
            } else {
                //已存在则进行更新
                classDOMapper.updateByClassAndSchoolSelective(classRecord);
            }
        }
        return notNull;
    }

    private ClassModel convertModelFromDataObject(ClassDO classDO) {
        ClassModel classModel = new ClassModel();
        BeanUtils.copyProperties(classDO, classModel);
        return classModel;
    }

    private ClassDO convertDoFromModel(ClassModel classModel) {
        if (classModel == null) {
            return null;
        }
        ClassDO classDO = new ClassDO();
        BeanUtils.copyProperties(classModel, classDO);
        return classDO;
    }

    private void softDelClass(Integer id) {
        ClassDO classDO = classDOMapper.selectByPrimaryKey(id);
        classDO.setDeleteStatus(DeleteStatusConstant.createDeleteStatus());
        classDOMapper.updateByPrimaryKeySelective(classDO);
    }
}
