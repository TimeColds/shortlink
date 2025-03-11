package com.timecold.shortlink.admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.timecold.shortlink.admin.dao.entity.UserDO;
import com.timecold.shortlink.admin.dto.req.UserLoginReqDTO;
import com.timecold.shortlink.admin.dto.req.UserRegisterReqDTO;
import com.timecold.shortlink.admin.dto.req.UserUpdateReqDTO;
import com.timecold.shortlink.admin.dto.resp.UserLoginRespDTO;
import com.timecold.shortlink.admin.dto.resp.UserRespDTO;

/**
 * 用户接口层
 */
public interface UserService extends IService<UserDO> {

    /**
     * 根据用户名查询用户信息
     * @return 用户返回实体
     */
    UserRespDTO getUserByUsername();

    /**
     * 查询用户名是否存在
     * @param username 用户名
     * @return 用户存在返回 False, 不存在返回 True
     */
    Boolean isUsernameAvailable(String username);

    /**
     * 注册用户
     *
     * @param requestParam 注册用户请求参数
     */
    void register(UserRegisterReqDTO requestParam);

    /**
     * 根据用户名修改用户
     *
     * @param requestParam 修改用户请求参数
     */
    void update(UserUpdateReqDTO requestParam);

    /**
     * 用户登录
     * @param requestParam 用户登录请求参数
     * @return 用户登录返回参数 Token
     */
    UserLoginRespDTO login(UserLoginReqDTO requestParam);

    /**
     * 检查用户是否登录
     * @param username 用户名
     * @param token 用户登录 token
     * @return 已登录返回 True 未登录返回 False
     */
    Boolean checkLogin(String username, String token);

    /**
     * 用户退出登录
     * @param username 用户名
     * @param token token
     */
    void logout(String username, String token);
}
