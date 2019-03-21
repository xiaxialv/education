package com.zaozao.config;

import com.zaozao.error.BusinessException;
import com.zaozao.error.EmBusinessError;
import com.zaozao.util.AlipayConfig;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.*;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;

/**
 * @author Sidney 2019-03-07.
 */
@Aspect
@Component
public class AspectConfig {

    private Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private HttpServletRequest httpServletRequest;

    /**
     * 在controller上加@AuthCheck注解即可进行登录拦截
     */
    @Pointcut("@annotation(com.zaozao.annotation.AuthCheck)")
    public void authCheck(){}

    @Before("authCheck()")
    public void authCheckBefore() throws BusinessException {
        Boolean isLogin = (Boolean) httpServletRequest.getSession().getAttribute("IS_LOGIN");
        if (isLogin == null || !isLogin) {
            throw new BusinessException(EmBusinessError.USER_NOT_LOGIN);
        }
    }

    /**
     * 配置所有controller方法进入的日志
     */
    @Pointcut("@annotation(org.springframework.web.bind.annotation.RequestMapping)")
    public void printLog(){}

    @Around("printLog()")
    public Object printLogAround(ProceedingJoinPoint joinPoint) throws Throwable {
        long l = System.currentTimeMillis();
        Object result = null;
        Object[] args = joinPoint.getArgs();
        StringBuilder param = new StringBuilder();
        Signature sig = joinPoint.getSignature();
        String methodName;
        String[] parameterNames = null;
        if (sig instanceof MethodSignature) {
            MethodSignature signature = (MethodSignature) sig;
            parameterNames = signature.getParameterNames();
            Object target = joinPoint.getTarget();
            Method currentMethod = target.getClass().getMethod(signature.getName(), signature.getParameterTypes());
            methodName = currentMethod.getName();
        } else {
            methodName = "";
        }

        for (int i = 0; i < args.length; i++) {
            Object arg = args[i];
            if (parameterNames == null) {
                param.append(i).append("[").append(arg).append("] ");
            } else {
                param.append(parameterNames[i]).append(":[").append(arg).append("] ");
            }
        }
        String className = joinPoint.getTarget().getClass().getName();
        logger.info(className + "." + methodName + ", param : " + param
                        .toString());
        try {
            result = joinPoint.proceed(args);
        } finally {
            logger.info(className + "." + methodName + " time cost : " + (System.currentTimeMillis() - l));
        }
        return result;
    }

    /**
     * 进入IsvController中修改支付宝配置isvAlipayConfigEdit方法后,刷新内存中的AlipayClient
     * @Refresh注解即可进行刷新
     * TODO 检查是否可用
     */
    @Pointcut("@annotation(com.zaozao.annotation.Refresh)")
    public void refresh(){}

    @After("refresh()")
    public void refreshAfter() {
        AlipayConfig.refresh();
        logger.info("=======刷新内存======");
    }
}
