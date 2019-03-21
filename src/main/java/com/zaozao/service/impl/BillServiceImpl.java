package com.zaozao.service.impl;

import com.zaozao.Constant.BillConstant;
import com.zaozao.Constant.DeleteStatusConstant;
import com.zaozao.dao.BillDOMapper;
import com.zaozao.dao.BillItemDOMapper;
import com.zaozao.dataobject.BillDO;
import com.zaozao.dataobject.BillItemDO;
import com.zaozao.error.BusinessException;
import com.zaozao.error.EmBusinessError;
import com.zaozao.service.BillService;
import com.zaozao.service.ClassService;
import com.zaozao.service.SchoolService;
import com.zaozao.service.StudentService;
import com.zaozao.service.model.BillModel;
import com.zaozao.service.model.ClassModel;
import com.zaozao.service.model.SchoolModel;
import com.zaozao.service.model.StudentModel;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.NumberToTextConverter;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.joda.time.DateTime;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Sidney 2019-01-10.
 */
@Service
public class BillServiceImpl implements BillService {

    @Autowired
    private BillDOMapper billDOMapper;

    @Autowired
    private BillItemDOMapper billItemDOMapper;

    @Autowired
    private SchoolService schoolService;

    @Autowired
    private StudentService studentService;

    @Autowired
    private ClassService classService;


    @Override
    @Transactional(propagation = Propagation.SUPPORTS)
    public List<BillModel> listBillBySchoolAndClassAndStudentNo(Integer schoolId, Integer classId, String studentNo,
            Byte billStatus, String deleteStatus) {
        List<BillDO> billDOList = billDOMapper
                .listBillBySchoolAndClassAndStudentNo(schoolId, classId, studentNo, billStatus, deleteStatus);
        return billDOList.stream().map(this::convertModelFromDataObject).collect(Collectors.toList());
    }

    @Override
    @Transactional(propagation = Propagation.SUPPORTS)
    public List<BillModel> listBill(String deleteStatus) {
        List<BillDO> billDOList = billDOMapper.listBill(deleteStatus);
        return billDOList.stream().map(this::convertModelFromDataObject).collect(Collectors.toList());
    }

    @Override
    public List<BillModel> listBillByDynamicQuery(Integer schoolId, Integer gradeNum, Integer classNum,
            String studentNo, String studentName, String createDate, String payDate, String billNum, Byte payType,
            Byte billStatus,Byte synStatus, String billName, Integer billItemId,String deleteStatusStay) {
        List<BillDO> billDOList = billDOMapper
                .listBillByDynamicQuery(schoolId, gradeNum, classNum, studentNo, studentName, createDate, payDate,
                        billNum, payType, billStatus, synStatus, billName, billItemId, deleteStatusStay);
        return billDOList.stream().map(this::convertModelFromDataObject).collect(Collectors.toList());
    }

    @Override
    @Transactional(propagation = Propagation.SUPPORTS)
    public List<BillModel> listBillLike(String deleteStatus) {
        List<BillDO> billDOList = billDOMapper.listBillLike(deleteStatus);
        return billDOList.stream().map(this::convertModelFromDataObject).collect(Collectors.toList());
    }

    @Override
    @Transactional(propagation = Propagation.SUPPORTS)
    public List<BillModel> listBillBySchoolIdList(List<Integer> schoolIdList, String deleteStatus) {
        List<BillDO> billDOList = billDOMapper.listBillBySchoolIdList(schoolIdList, deleteStatus);
        return billDOList.stream().map(this::convertModelFromDataObject).collect(Collectors.toList());
    }

    @Override
    @Transactional(propagation = Propagation.SUPPORTS)
    public List<BillModel> listBillBySchoolIdListLike(List<Integer> schoolIdList, String deleteStatus) {
        List<BillDO> billDOList = billDOMapper.listBillBySchoolIdListLike(schoolIdList, deleteStatus);
        return billDOList.stream().map(this::convertModelFromDataObject).collect(Collectors.toList());
    }

    @Override
    @Transactional(propagation = Propagation.SUPPORTS)
    public List<BillModel> listBillBySchoolId(Integer schoolId, String deleteStatus) {
        List<BillDO> billDOList = billDOMapper.listBillBySchoolId(schoolId, deleteStatus);
        return billDOList.stream().map(this::convertModelFromDataObject).collect(Collectors.toList());
    }

    @Override
    @Transactional(propagation = Propagation.SUPPORTS)
    public List<BillModel> listBillBySchoolIdLike(Integer schoolId, String deleteStatus) {
        List<BillDO> billDOList = billDOMapper.listBillBySchoolIdLike(schoolId, deleteStatus);
        return billDOList.stream().map(this::convertModelFromDataObject).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
    public void createBill(BillModel billModel) {
        BillDO billDO = this.convertDOFromModel(billModel);
        billDOMapper.insertSelective(billDO);
        //        billModel.setId(billDO.getId());
        //        return this.getBillById(billModel.getId());
    }

    @Override
    @Transactional(propagation = Propagation.SUPPORTS)
    public BillModel getBillById(Integer id) {
        BillDO billDO = billDOMapper.selectByPrimaryKey(id);
        return this.convertModelFromDataObject(billDO);
    }

    @Override
    public void updateBill(BillModel billModel) {
        BillDO billDO = this.convertDOFromModel(billModel);
        billDOMapper.updateByPrimaryKeySelective(billDO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
    public void softDeleteBill(Integer id) {
        // 账单软删除
        BillDO billDO = billDOMapper.selectByPrimaryKey(id);
        billDO.setDeleteStatus(DeleteStatusConstant.createDeleteStatus());
        billDOMapper.updateByPrimaryKeySelective(billDO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
    public void softDeleteBillBatch(List<Integer> idList) {
        // 账单批量软删除
        List<BillDO> billDOList = billDOMapper.selectByPrimaryKeyList(idList);
        for (BillDO billDO : billDOList) {
            this.softDeleteBill(billDO.getId());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
    public void softDeleteBillByClassIdList(List<Integer> classIdList) {
        List<BillDO> billDOList =
                billDOMapper.listBillByClassIdList(classIdList, DeleteStatusConstant.DELETE_STATUS_STAY);
        for (BillDO billDO : billDOList) {
            this.softDeleteBill(billDO.getId());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
    public void softDeleteBillByStudentId(Integer studentId) {
        List<BillDO> billDOList = billDOMapper.listBillByStudentId(studentId, DeleteStatusConstant.DELETE_STATUS_STAY
                , BillConstant.BILL_STATUS_UNPAID);
        for (BillDO billDO : billDOList) {
            this.softDeleteBill(billDO.getId());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
    public void deleteBillBySchoolId(Integer schoolId) {
        billDOMapper.deleteBySchoolId(schoolId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
    public void createBillItem(BillItemDO billItemDO) {
        billItemDOMapper.insertSelective(billItemDO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
    public void softDeleteBillItem(Integer id) {
        BillItemDO billItemDO = billItemDOMapper.selectByPrimaryKey(id);
        billItemDO.setDeleteStatus(DeleteStatusConstant.createDeleteStatus());
        billItemDOMapper.updateByPrimaryKeySelective(billItemDO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
    public void deleteBillItemBySchoolId(Integer schoolId) {
        billItemDOMapper.deleteBySchoolId(schoolId);
    }

    @Override
    @Transactional(propagation = Propagation.SUPPORTS)
    public List<BillItemDO> listBillItemBySchool(Integer schoolId, String deleteStatus) {
        return billItemDOMapper.listItemBySchool(schoolId, deleteStatus);
    }

    @Override
    @Transactional(propagation = Propagation.SUPPORTS)
    public BillItemDO getBillItem(Integer id) {
        return billItemDOMapper.selectByPrimaryKey(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
    public boolean batchImport(String fileName, MultipartFile file, Integer schoolId,String billName,String endDate) throws Exception {
        boolean notNull = false;
        List<BillDO> billDOList = new ArrayList<>();
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
        BillDO billDO;
        assert sheet != null;
        for (int r = 1; r <= sheet.getLastRowNum(); r++) {
            Row row = sheet.getRow(r);
            if (row == null) {
                continue;
            }
            billDO = new BillDO();
            String studentNo = null;
            if (row.getCell(0) != null) {
                row.getCell(0).setCellType(Cell.CELL_TYPE_STRING);
                studentNo = row.getCell(0).getStringCellValue();
            }
            // 判断该学号的学生是否存在
            SchoolModel schoolById = schoolService.getSchoolById(schoolId);
            StudentModel studentByStudentNo =
                    studentService.getStudentByStudentNo(schoolId, studentNo, DeleteStatusConstant.DELETE_STATUS_STAY);
            if (studentByStudentNo == null) {
                throw new BusinessException(
                        EmBusinessError.STUDENT_NOT_FIND.setErrMsg("学号为" + studentNo + "的学生不存在," + "请确认所有信息无误,再上传文件"));
            }
            if (studentNo == null || studentNo.isEmpty()) {
                throw new BusinessException(
                        EmBusinessError.EXCEL_ROW_TYPE_ERROR.setErrMsg("导入失败(第" + (r + 1) + "行,学号未填写)"));
            }
//            String billName = row.getCell(1).getStringCellValue();
//            if (billName == null || billName.isEmpty()) {
//                throw new BusinessException(
//                        EmBusinessError.EXCEL_ROW_TYPE_ERROR.setErrMsg("导入失败(第" + (r + 1) + "行,账单名称未填写)"));
//            }
            Integer billItemId = Integer.valueOf(NumberToTextConverter.toText(row.getCell(1).getNumericCellValue()));
            // 判断缴费类型是否存在
            List<BillItemDO> billItemDOList =
                    billItemDOMapper.listItemBySchool(schoolId, DeleteStatusConstant.DELETE_STATUS_STAY);
            List<Integer> billItemIdList = billItemDOList.stream().map(BillItemDO::getId).collect(Collectors.toList());
            if (billItemId == 0) {
                throw new BusinessException(
                        EmBusinessError.EXCEL_ROW_TYPE_ERROR.setErrMsg("导入失败(第" + (r + 1) + "行," + "缴费类型编号未填写,且不能为0)"));
            } else if (CollectionUtils.isEmpty(billItemIdList)) {
                throw new BusinessException(EmBusinessError.BILL_ITEM_NULL);
            } else if (!billItemIdList.contains(billItemId)) {
                throw new BusinessException(
                        EmBusinessError.BILL_ITEM_NULL.setErrMsg(billItemId + "该缴费类型不存在,请添加后再上传文件"));
            }
            double billAmount = row.getCell(2).getNumericCellValue();
            if (billAmount == 0) {
                throw new BusinessException(
                        EmBusinessError.EXCEL_ROW_TYPE_ERROR.setErrMsg("导入失败(第" + (r + 1) + "行," + "账单金额未填写,且不能为0)"));
            }
            String comment = row.getCell(3).getStringCellValue();
            Integer classId = studentByStudentNo.getClassId();
            ClassModel classById = classService.getClassById(classId);
            billDO.setStudentName(studentByStudentNo.getName());
            billDO.setSchoolId(schoolId);
            billDO.setIsvId(schoolById.getIsvId());
            billDO.setSchoolName(schoolById.getSchoolName());
            billDO.setClassId(classId);
            billDO.setGradeNum(classById.getGradeNum());
            billDO.setClassNum(classById.getClassNum());
            billDO.setStudentId(studentByStudentNo.getId());
            billDO.setStudentNo(studentNo);
            billDO.setBillName(billName);
            billDO.setBillItemId(billItemId);
            billDO.setBillAmount(billAmount);
            billDO.setBillStatus(BillConstant.BILL_STATUS_UNPAID);
            DateTime now = new DateTime();
            billDO.setCreateDate(now.toDate());
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date end = sdf.parse(endDate);
            billDO.setEndDate(end);
            billDO.setComment(comment);
            billDOList.add(billDO);
        }
        for (BillDO billRecord : billDOList) {
            // 学号 账单名称 缴费类型 未支付判断为同一个账单
            String studentNo = billRecord.getStudentNo();
//            String billName = billRecord.getBillName();
            Integer billItemId = billRecord.getBillItemId();
            // 校验账单记录是否存在
            int selectResult = billDOMapper.selectAmountByStudentAndBillAndItem(studentNo, billName, billItemId,
                    DeleteStatusConstant.DELETE_STATUS_STAY);
            if (selectResult == 0) {
                // 不存在则进行增加
                billDOMapper.insertSelective(billRecord);
            } else {
                // 已存在则进行更新
                BillDO billDoReturn = billDOMapper.getByStudentAndBillAndItem(studentNo, billName, billItemId,
                        DeleteStatusConstant.DELETE_STATUS_STAY);
                billRecord.setId(billDoReturn.getId());
                billDOMapper.updateByPrimaryKeySelective(billRecord);
            }
        }
        return notNull;
    }


    private BillModel convertModelFromDataObject(BillDO billDO) {
        BillModel billModel = new BillModel();
        BeanUtils.copyProperties(billDO, billModel);
        billModel.setBillAmount(new BigDecimal(billDO.getBillAmount()));
        billModel.setCreateDate(new DateTime(billDO.getCreateDate().getTime()));
        Date payDate = billDO.getPayDate();
        if (payDate != null) {
            billModel.setPayDate(new DateTime(payDate.getTime()));
        }
        Date updateDate = billDO.getUpdateDate();
        if (updateDate != null) {
            billModel.setUpdateDate(new DateTime(updateDate.getTime()));
        }
        Date endDate = billDO.getEndDate();
        if (endDate != null) {
            billModel.setEndDate(new DateTime(endDate.getTime()));
        }
        return billModel;
    }

    private BillDO convertDOFromModel(BillModel billModel) {
        if (billModel == null) {
            return null;
        }
        BillDO billDO = new BillDO();
        BeanUtils.copyProperties(billModel, billDO);
        BigDecimal billAmount = billModel.getBillAmount();
        if (billAmount != null) {
            billDO.setBillAmount(billAmount.doubleValue());
        }
        DateTime createDate = billModel.getCreateDate();
        if (createDate != null) {
            billDO.setCreateDate(createDate.toDate());
        }
        DateTime payDate = billModel.getPayDate();
        if (payDate != null) {
            billDO.setPayDate(payDate.toDate());
        }
        DateTime updateDate = billModel.getUpdateDate();
        if (updateDate != null) {
            billDO.setUpdateDate(updateDate.toDate());
        }
        DateTime endDate = billModel.getEndDate();
        if (endDate != null) {
            billDO.setEndDate(endDate.toDate());
        }
        return billDO;
    }

}
