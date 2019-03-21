package com.zaozao.service;

import com.zaozao.dao.BillDOMapper;
import com.zaozao.dao.StudentDOMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.Future;

/**
 * @author Sidney 2019-02-19.
 * 异步执行类
 */
@Component
public class AsyncTask {

    @Autowired
    private BillDOMapper billDOMapper;

    @Autowired
    private StudentDOMapper studentDOMapper;

    // 示例,以备后用
    @Async
    public Future<Double> countBillAmount1(Map<String, Object> map) throws InterruptedException {
        Double amount = billDOMapper.countBillAmountDynamic(map);
        return new AsyncResult<>(amount);
    }
}
