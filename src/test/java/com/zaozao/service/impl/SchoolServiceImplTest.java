package com.zaozao.service.impl;

import com.zaozao.App;
import com.zaozao.Constant.SchoolConstant;
import com.zaozao.service.SchoolService;
import com.zaozao.service.model.SchoolModel;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * @author Sidney 2019-03-21.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = App.class)
public class SchoolServiceImplTest {

    @Autowired
    private SchoolService schoolService;

    @Test
    public void createSchool() {
        SchoolModel schoolModel = new SchoolModel();
        schoolModel.setIsvId(1);
        schoolModel.setSchoolName("test");
        schoolModel.setSchoolAddress("test");
        schoolModel.setSchoolContact("test");
        schoolModel.setSchoolTel("test");
        schoolModel.setAlipayAccount("test");
        schoolModel.setSchoolSystem(1);
        schoolModel.setSchoolType(1);
        schoolModel.setProvinceCode("test");
        schoolModel.setProvinceName("test");
        schoolModel.setCityCode("test");
        schoolModel.setCityName("test");
        schoolModel.setDistrictCode("test");
        schoolModel.setDistrictName("test");
        schoolModel.setSchoolStatus(SchoolConstant.SCHOOL_SEND_UNSEND);
        SchoolModel school = schoolService.createSchool(schoolModel);
        System.out.println(school.getId());
    }

    @Test
    public void listSchool() {
    }

    @Test
    public void listSchoolByIsvId() {
    }

    @Test
    public void getSchoolById() {
    }

    @Test
    public void updateSchool() {
    }

    @Test
    public void deleteSchool() {
    }

    @Test
    public void listSchoolByGivenIdList() {
    }
}
