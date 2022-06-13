package com.william.takeout.common;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

// 自定义元数据对象处理器，自动填充
// 发送一次Http请求，服务端就会新创建一个线程来处理来处理该请求，即处理请求用到的所有方法使用同一个线程
// LoginCheckFilter的doFilter方法
// EmployeeController的update方法
// MyMetaObjectHandler的updateFill方法
// 由于属于同一个线程且TreadLocal, 可以在LoginCheckFilter的doFilter方法中获取当前登陆用户的id（Filter有HttpServletRequest）
// 并调用ThreadLocal的set方法来设置当前线程的线程局部变量的值（用户id）,然后再MyMetaObjectHandler(没有HttpServletRequest)
// 的updateFill方法中调用ThreadLocal的get方法来获得它

// 具体实现
// 编写BaseContext工具类，基于ThreadLocal封装的工具类
// 在LoginCheckFilter的doFilter方法中调用BaseContext来设置当前登陆用户的ID
// 在MyMetaObjectHandler的方法中调用BaseContext获取登陆用户的ID


@Slf4j
@Component
public class MyMetaObjectHandler implements MetaObjectHandler {
    @Override
    public void insertFill(MetaObject metaObject) {//添加操作，就跳到这个方法了；metaObject包含添加的对象
        log.info("公共字段自动填充[insert]....");
        metaObject.setValue("createTime", LocalDateTime.now());
        metaObject.setValue("updateTime", LocalDateTime.now());

        metaObject.setValue("createUser",BaseContext.getCurrentId());//动态获取登陆用户的ID
        metaObject.setValue("updateUser",BaseContext.getCurrentId());


    }

    @Override
    public void updateFill(MetaObject metaObject) {//页面更新操作，跳到这个方法
        log.info("公共字段自动填充[update]....");

        metaObject.setValue("updateTime", LocalDateTime.now());
        metaObject.setValue("updateUser",BaseContext.getCurrentId());

    }
}
