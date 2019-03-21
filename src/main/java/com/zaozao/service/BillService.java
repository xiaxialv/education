package com.zaozao.service;

import com.zaozao.dataobject.BillItemDO;
import com.zaozao.error.BusinessException;
import com.zaozao.service.model.BillModel;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * @author Sidney 2019-01-10.
 */
public interface BillService {
    // 账单的新增
    void createBill(BillModel billModel) throws BusinessException;

    // 根据学校班级学号查询学生的缴费账单
    List<BillModel> listBillBySchoolAndClassAndStudentNo(Integer schoolId, Integer classId, String studentNo,
            Byte billStatus, String deleteStatus);

    // 查询所有学生的缴费账单
    List<BillModel> listBill(String deleteStatus);

    // 模糊查询所有学生的缴费账单
    List<BillModel> listBillLike(String deleteStatus);

    // 查询isv所属学校所有学生的缴费账单
    List<BillModel> listBillBySchoolIdList(List<Integer> schoolIdList, String deleteStatus);

    // 模糊查询isv所属学校所有学生的缴费账单
    List<BillModel> listBillBySchoolIdListLike(List<Integer> schoolIdList, String deleteStatus);

    // 查询指定学校所有学生的缴费账单
    List<BillModel> listBillBySchoolId(Integer schoolId, String deleteStatus);

    // 模糊查询指定学校所有学生的缴费账单
    List<BillModel> listBillBySchoolIdLike(Integer schoolId, String deleteStatus);

    // 根据id获得账单
    BillModel getBillById(Integer id);

    // 账单修改
    void updateBill(BillModel billModel);

    // 账单软删除
    void softDeleteBill(Integer id);

    // 账单批量软删除
    void softDeleteBillBatch(List<Integer> idList);

    // 根据班级账单软删除
    void softDeleteBillByClassIdList(List<Integer> classIdList);

    // 根据学生编号对账单软删除
    void softDeleteBillByStudentId(Integer studentId);

    // 根据学校编号对账单硬删除
    void deleteBillBySchoolId(Integer schoolId);

    // 账单类型的增加
    void createBillItem(BillItemDO billItemDO);

    // 账单类型的软删除
    void softDeleteBillItem(Integer id);

    // 根据学校id对账单类型的硬删除
    void deleteBillItemBySchoolId(Integer schoolId);

    // 获取所有账单类型
    List<BillItemDO> listBillItemBySchool(Integer schoolId, String deleteStatus);

    // 获取指定id的账单类型
    BillItemDO getBillItem(Integer id);

    // 学生账单导入Excel
    boolean batchImport(String fileName, MultipartFile file, Integer schoolId,String billName,String endDate) throws Exception;

    // 动态查询学生账单
    List<BillModel> listBillByDynamicQuery(Integer schoolId, Integer gradeNum, Integer classNum, String studentNo,
            String studentName, String createDate, String payDate, String billNum, Byte payType, Byte billStatus,Byte synStatus,
            String billName, Integer billItemId, String deleteStatusStay);
}
