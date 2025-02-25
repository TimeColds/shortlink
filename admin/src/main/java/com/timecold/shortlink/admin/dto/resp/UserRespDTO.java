package com.timecold.shortlink.admin.dto.resp;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.timecold.shortlink.admin.serialize.PhoneDesensitizationSerializer;
import lombok.Data;

/**
 * 用户返回参数响应
 */
@Data
public class UserRespDTO {

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
    @JsonSerialize(using = PhoneDesensitizationSerializer.class)
    private String phone;

    /**
     * 邮箱
     */
    private String mail;
}
