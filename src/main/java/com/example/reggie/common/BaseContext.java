package com.example.reggie.common;

/**
 * 用于在当前线程设置employee id的工具类
 */
public class BaseContext {
    private static ThreadLocal<Long> threadLocal = new ThreadLocal<>();

    /**
     * 设置employee
     * @param id
     */
    public static void setCurrentId(Long id){
        threadLocal.set(id);
    }

    /**
     * 获取employee
     * @return
     */
    public static Long getCurrentId(){
        return threadLocal.get();
    }
}
