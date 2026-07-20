//package com.opensabre.admin.common.util;
//
//import java.util.HashMap;
//import java.util.Map;
//import java.util.Optional;
//
///**
// * 用户上下文 - 基于ThreadLocal的线程级用户信息存储
// */
//public class UserContextHolder {
//
//    public static final String KEY_USERNAME = "user_name";
//
//    private final ThreadLocal<Map<String, String>> threadLocal;
//
//    private UserContextHolder() {
//        this.threadLocal = new ThreadLocal<>();
//    }
//
//    public static UserContextHolder getInstance() {
//        return SingletonHolder.INSTANCE;
//    }
//
//    private static class SingletonHolder {
//        private static final UserContextHolder INSTANCE = new UserContextHolder();
//    }
//
//    /**
//     * 设置上下文信息
//     */
//    public void setContext(Map<String, String> map) {
//        threadLocal.set(map);
//    }
//
//    /**
//     * 获取上下文信息
//     */
//    public Map<String, String> getContext() {
//        return Optional.ofNullable(threadLocal.get()).orElse(new HashMap<>());
//    }
//
//    /**
//     * 获取当前用户名
//     */
//    public String getUsername() {
//        return getContext().get(KEY_USERNAME);
//    }
//
//    /**
//     * 清空上下文
//     */
//    public void clear() {
//        threadLocal.remove();
//    }
//}
