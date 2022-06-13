package com.william.takeout.common;

// 基于ThreadLocal 封装工具类，用户保存和获取当前登录的用户id
// ThreadLocal以线程为 作用域，保存每个线程中的数据副本
public class BaseContexts {

    private static ThreadLocal<Long> threadLocal = new ThreadLocal<>();//用其存登陆id

    //  设置当前用户id, 工具方法，要设置成static
    public static void setCurrentId(Long id){
        threadLocal.set(id);
    }

    public static Long getCurrentId(){
        return threadLocal.get();
    }
}
