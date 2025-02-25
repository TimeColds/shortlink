package com.timecold.shortlink.admin.dto.resp;

import lombok.Data;

/**
 * 用户返回真实参数响应
 */
@Data
public class UserActualRespDTO {

    /**
     * 用户ID
     */
    private Long uid;

    /**
     * 用户名
     */
    private String username;

    /**
     * 真实姓名
     */
    private String realName;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 邮箱
     */
    private String mail;
}
