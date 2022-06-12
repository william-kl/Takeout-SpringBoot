package com.william.takeout.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.sql.SQLIntegrityConstraintViolationException;

// 全局异常处理
// 如果类上加有 @RestController、@Controller注解(annotations的属性值)的类中有方法抛出异常，由GlobalExceptionHander来处理异常
@ControllerAdvice(annotations = {RestController.class, Controller.class})
@ResponseBody  // 将结果封装成JSON数据并返回
@Slf4j
public class GlobalExceptionHandler {

    // 解决 字段username被唯一索引约束的情况下，添加相同的username，抛出SQLIntegrityConstraintViolationException 的全局异常
    @ExceptionHandler(SQLIntegrityConstraintViolationException.class)
    public Result<String> exceptionHandler(SQLIntegrityConstraintViolationException e){
        //在写具体类的时候，可以先在log.error加一个断点；先保证这个类能起作用（debug模式，输入duplicate
        // username,提交后直接就跳过来了），然后再具体写处理逻辑
        //这里要写log.error
        log.error(e.getMessage());

        //如果异常信息里含有这个关键字
        if (e.getMessage().contains("Duplicate entry")){
            //异常信息用空格分隔，返回string数组
            String[] split = e.getMessage().split(" ");
            //zhangsan已存在
            String msg = split[2] + "已存在";//Duplicate entry 'zhangsan' for key 'idx_username'
            //返回给前端‘zhangsan’ 已存在        split[2] = zhangsan就拿到了
            return Result.error(msg);
        }
        return Result.error("未知错误！");
    }
/*
    @ExceptionHandler(MyCustomException.class)
    public Result<String> exceptionHandler(MyCustomException e){
        log.info(e.getMessage());

        return Result.error(e.getMessage());

    }

 */
}

