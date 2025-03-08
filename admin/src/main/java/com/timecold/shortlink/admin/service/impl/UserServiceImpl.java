package com.timecold.shortlink.admin.service.impl;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.timecold.shortlink.admin.biz.user.UserContext;
import com.timecold.shortlink.admin.common.constant.RedisKeyConstant;
import com.timecold.shortlink.admin.common.convention.exception.ClientException;
import com.timecold.shortlink.admin.common.convention.exception.ServiceException;
import com.timecold.shortlink.admin.common.enums.UserErrorCodeEnum;
import com.timecold.shortlink.admin.dao.entity.UserDO;
import com.timecold.shortlink.admin.dao.mapper.UserMapper;
import com.timecold.shortlink.admin.dto.req.UserLoginReqDTO;
import com.timecold.shortlink.admin.dto.req.UserRegisterReqDTO;
import com.timecold.shortlink.admin.dto.req.UserUpdateReqDTO;
import com.timecold.shortlink.admin.dto.resp.UserLoginRespDTO;
import com.timecold.shortlink.admin.dto.resp.UserRespDTO;
import com.timecold.shortlink.admin.service.GroupService;
import com.timecold.shortlink.admin.service.UserService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, UserDO> implements UserService {

    private final RBloomFilter<String> userRegisterCachePenetrationBloomFilter;

    private final RedissonClient redissonClient;

    private final StringRedisTemplate stringRedisTemplate;

    private final GroupService groupService;

    private final ObjectMapper objectMapper;

    @Override
    public UserRespDTO getUserByUsername() {
        LambdaQueryWrapper<UserDO> queryWrapper = Wrappers.lambdaQuery(UserDO.class)
                .eq(UserDO::getUsername, UserContext.getUsername());
        UserDO userDO = baseMapper.selectOne(queryWrapper);
        if (userDO == null) {
            throw new ServiceException(UserErrorCodeEnum.USER_NULL);
        }
        UserRespDTO result = new UserRespDTO();
        BeanUtils.copyProperties(userDO, result);
        return result;
    }

    @Override
    public Boolean hasUserName(String username) {
        return !userRegisterCachePenetrationBloomFilter.contains(username);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void register(UserRegisterReqDTO requestParam) {
        // 初步布隆过滤器检查
        if (!hasUserName(requestParam.getUsername())) {
            throw new ClientException(UserErrorCodeEnum.USER_EXIST);
        }

        RLock lock = redissonClient.getLock(RedisKeyConstant.USER_REGISTER_LOCK_KEY + requestParam.getUsername());
        try {
            // 尝试获取锁，设置等待时间和锁持有时间以避免死锁
            boolean isLockAcquired = lock.tryLock(100, 10000, TimeUnit.MILLISECONDS);
            if (!isLockAcquired) {
                // 锁获取失败后二次检查数据库
                UserDO existingUser = baseMapper.selectOne(new LambdaQueryWrapper<UserDO>()
                        .eq(UserDO::getUsername, requestParam.getUsername()));
                if (existingUser != null) {
                    userRegisterCachePenetrationBloomFilter.add(requestParam.getUsername());
                    throw new ClientException(UserErrorCodeEnum.USER_EXIST);
                } else {
                    throw new ClientException(UserErrorCodeEnum.USER_REGISTER_CONFLICT);
                }
            }

            try {
                // 双重检查：获取锁后再次验证用户名
                if (!hasUserName(requestParam.getUsername())) {
                    throw new ClientException(UserErrorCodeEnum.USER_EXIST);
                }

                // 创建用户逻辑
                long uid = IdUtil.getSnowflakeNextId();
                UserDO userDO = UserDO.builder()
                        .uid(uid)
                        .build();
                BeanUtils.copyProperties(requestParam,userDO);
                if (baseMapper.insert(userDO) < 1) {
                    throw new ClientException(UserErrorCodeEnum.USER_REGISTER_ERROR);
                }

                // 更新布隆过滤器并创建默认分组
                userRegisterCachePenetrationBloomFilter.add(requestParam.getUsername());
                groupService.saveGroup(uid, "默认分组");
            } catch (DuplicateKeyException e) {
                // 处理唯一键冲突并更新布隆过滤器
                userRegisterCachePenetrationBloomFilter.add(requestParam.getUsername());
                throw new ClientException(UserErrorCodeEnum.USER_EXIST);
            } finally {
                lock.unlock();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ClientException(UserErrorCodeEnum.USER_REGISTER_ERROR);
        }
    }

    @Override
    public void update(UserUpdateReqDTO requestParam) {
        LambdaUpdateWrapper<UserDO> updateWrapper = Wrappers.lambdaUpdate(UserDO.class)
                .eq(UserDO::getUsername, UserContext.getUsername());
        UserDO userDO = new UserDO();
        BeanUtils.copyProperties(requestParam, userDO);
        baseMapper.update(userDO,updateWrapper);
    }

    @Override
    public UserLoginRespDTO login(UserLoginReqDTO requestParam) {
        if (requestParam == null || StringUtils.isBlank(requestParam.getUsername())
                || StringUtils.isBlank(requestParam.getPassword())) {
            throw new ClientException("登录参数不能为空");
        }
        String loginKey = RedisKeyConstant.USER_LOGIN_KEY + requestParam.getUsername();
        Boolean hasLogin = stringRedisTemplate.hasKey(loginKey);
        Map<Object, Object> hasLoginMap = stringRedisTemplate.opsForHash().entries(loginKey);
        if (MapUtils.isNotEmpty(hasLoginMap)) {
            stringRedisTemplate.expire(loginKey, 30L, TimeUnit.DAYS);
            String token = hasLoginMap.keySet().stream()
                    .findFirst()
                    .map(Object::toString)
                    .orElseThrow(() -> new ClientException("用户登录错误"));
            return new UserLoginRespDTO(token);
        }
        if (Boolean.TRUE.equals(hasLogin)) {
            throw new ClientException("用户已登录");
        }
        LambdaQueryWrapper<UserDO> queryWrapper = Wrappers.lambdaQuery(UserDO.class)
                .eq(UserDO::getUsername, requestParam.getUsername())
                .eq(UserDO::getPassword, requestParam.getPassword())
                .eq(UserDO::getDelFlag, 0);
        UserDO userDO = baseMapper.selectOne(queryWrapper);
        if (userDO == null) {
            throw new ClientException("用户名或密码错误");
        }
        String uuid = UUID.randomUUID().toString();
        try {
            Map<String, Object> loginInfo = new HashMap<>();
            loginInfo.put("uid", userDO.getUid());
            loginInfo.put("username", userDO.getUsername());
            loginInfo.put("realName", userDO.getRealName());
            stringRedisTemplate.opsForHash().put(loginKey, uuid, objectMapper.writeValueAsString(loginInfo));
            stringRedisTemplate.expire(loginKey, 30L, TimeUnit.DAYS);
        } catch (Exception e) {
            throw new ClientException("登录失败，请稍后重试");
        }
        return new UserLoginRespDTO(uuid);
    }

    @Override
    public Boolean checkLogin(String username, String token) {
        return stringRedisTemplate.opsForHash().get(RedisKeyConstant.USER_LOGIN_KEY + username, token) != null;
    }

    @Override
    public void logout(String username, String token) {
        if (checkLogin(username, token)) {
            stringRedisTemplate.delete(RedisKeyConstant.USER_LOGIN_KEY + username);
        } else {
            throw new ClientException("用户未登录");
        }
    }
}
