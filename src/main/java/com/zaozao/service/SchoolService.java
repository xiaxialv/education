package com.zaozao.service;

import com.zaozao.service.model.SchoolModel;

import java.util.List;

/**
 * @author Sidney 2019-01-10.
 */
public interface SchoolService {
    // 创建学校
    SchoolModel createSchool(SchoolModel schoolModel);

    // 学校列表
    List<SchoolModel> listSchool();

    // 指定服务商的学校列表
    List<SchoolModel> listSchoolByIsvId(Integer isvId);

    // 学校详情浏览
    SchoolModel getSchoolById(Integer id);

    // 修改学校信息
    void updateSchool(SchoolModel schoolModel);

    // 删除学校信息
    void deleteSchool(Integer id);

    // 根据指定的idList获得学校
    List<SchoolModel> listSchoolByGivenIdList(List<Integer> schoolIdList);
}
