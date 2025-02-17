package com.timecold.shortlink.project.common.constant;

/**
 * Redis Key 常量类
 */
public class RedisKeyConstant {

    /**
     * 短链接跳转前缀 Key
     */
    public static final String SHORT_LINK_GOTO_KEY = "link:goto:";

    /**
     * 短链接跳转锁前缀 Key
     */
    public static final String SHORT_LINK_GOTO_LOCK_KEY = "link:goto:lock:";

    /**
     * 短链接空值跳转前缀 Key
     */
    public static final String SHORT_LINK_GOTO_NULL_KEY = "link:goto:null:";

}
