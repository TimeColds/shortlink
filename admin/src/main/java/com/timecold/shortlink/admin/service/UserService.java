package com.timecold.shortlink.admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.timecold.shortlink.admin.dao.entity.UserDO;
import com.timecold.shortlink.admin.dto.req.UserRegisterReqDTO;
import com.timecold.shortlink.admin.dto.resp.UserRespDTO;

/**
 * 用户接口层
 */
public interface UserService extends IService<UserDO> {

    /**
     * 根据用户名查询用户信息
     * @param username 用户名
     * @return 用户返回实体
     */
    UserRespDTO getUserByUsername(String username);

    /**
     * 查询用户名是否存在
     * @param username 用户名
     * @return 用户存在返回 True, 不存在返回 False
     */
    Boolean hasUserName(String username);

    /**
     * 注册用户
     *
     * @param requestParam 注册用户请求参数
     */
    void register(UserRegisterReqDTO requestParam);
}
