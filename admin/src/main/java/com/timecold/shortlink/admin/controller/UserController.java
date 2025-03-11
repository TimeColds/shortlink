package com.timecold.shortlink.admin.controller;

import cn.hutool.core.bean.BeanUtil;
import com.timecold.shortlink.admin.common.convention.result.Result;
import com.timecold.shortlink.admin.common.convention.result.Results;
import com.timecold.shortlink.admin.dto.req.UserLoginReqDTO;
import com.timecold.shortlink.admin.dto.req.UserRegisterReqDTO;
import com.timecold.shortlink.admin.dto.req.UserUpdateReqDTO;
import com.timecold.shortlink.admin.dto.resp.UserActualRespDTO;
import com.timecold.shortlink.admin.dto.resp.UserLoginRespDTO;
import com.timecold.shortlink.admin.dto.resp.UserRespDTO;
import com.timecold.shortlink.admin.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 用户管理控制层
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/short_link/admin")
public class UserController {

    private final UserService userService;

    /**
     * 根据用户名查询用户信息
     */
    @GetMapping("/user")
    public Result<UserRespDTO> getUserByUsername() {
            return Results.success(userService.getUserByUsername());
    }

    /**
     * 根据用户名查询用户未脱敏信息
     */
    @GetMapping("user/actual")
    public Result<UserActualRespDTO> getActualUserByUsername() {
        return Results.success(BeanUtil.toBean(userService.getUserByUsername(), UserActualRespDTO.class));
    }

    /**
     * 查询用户名是否存在
     */
    @GetMapping("/user/has-username")
    public Result<Boolean> hasUserName(@RequestParam("username") String username) {
        return Results.success(userService.isUsernameAvailable(username));
    }

    /**
     * 注册用户
     */
    @PostMapping("/user")
    public Result<Void> register(@RequestBody UserRegisterReqDTO requestParam) {
        userService.register(requestParam);
        return Results.success();
    }

    /**
     * 修改用户信息
     */
    @PutMapping("/user")
    public Result<Void> update(@RequestBody UserUpdateReqDTO requestParam) {
        userService.update(requestParam);
        return Results.success();
    }
    /**
     * 用户登录
     */
    @PostMapping("/user/login")
    public Result<UserLoginRespDTO> login(@RequestBody UserLoginReqDTO requestParam) {
        return Results.success(userService.login(requestParam));
    }

    /**
     * 校验用户是否登录
     */
    @GetMapping("/user/check-login")
    public Result<Boolean> checkLogin(HttpServletRequest request) {
        String username = request.getHeader("username");
        String token = request.getHeader("token");
        return Results.success(userService.checkLogin(username, token));
    }

    @DeleteMapping("/user/logout")
    public Result<Void> logout(HttpServletRequest request) {
        String username = request.getHeader("username");
        String token = request.getHeader("token");
        userService.logout(username, token);
        return Results.success();
    }
}
