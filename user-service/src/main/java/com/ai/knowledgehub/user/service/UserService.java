package com.ai.knowledgehub.user.service;

import com.ai.knowledgehub.common.config.JwtProperties;
import com.ai.knowledgehub.common.config.JwtUtil;
import com.ai.knowledgehub.common.constant.HeaderConstants;
import com.ai.knowledgehub.common.exception.AuthException;
import com.ai.knowledgehub.common.exception.BusinessException;
import com.ai.knowledgehub.common.result.ResultCode;
import com.ai.knowledgehub.user.dto.LoginDTO;
import com.ai.knowledgehub.user.dto.RegisterDTO;
import com.ai.knowledgehub.user.entity.User;
import com.ai.knowledgehub.user.mapper.UserMapper;
import com.ai.knowledgehub.user.vo.LoginVO;
import com.ai.knowledgehub.user.vo.UserVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 用户业务逻辑
 *
 * @author AI KnowledgeHub Team
 */
@Slf4j
@Service
public class UserService {

    private final UserMapper userMapper;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtProperties jwtProperties;
    private final TokenBlacklistService tokenBlacklistService;

    @Autowired
    public UserService(UserMapper userMapper,
                       JwtProperties jwtProperties,
                       TokenBlacklistService tokenBlacklistService) {
        this.userMapper = userMapper;
        this.jwtProperties = jwtProperties;
        this.tokenBlacklistService = tokenBlacklistService;
        this.passwordEncoder = new BCryptPasswordEncoder(10);
    }

    // ============================================================
    // 注册
    // ============================================================

    /**
     * 用户注册
     *
     * @param dto 注册请求
     * @return 新用户 ID
     * @throws BusinessException 2003 USERNAME_EXISTS 用户名已存在
     */
    @Transactional(rollbackFor = Exception.class)
    public Long register(RegisterDTO dto) {
        String username = dto.getUsername().trim();

        // 唯一性校验（大小写不敏感）
        Long count = userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username));
        if (count != null && count > 0) {
            throw new BusinessException(ResultCode.USERNAME_EXISTS);
        }

        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
        user.setRole(HeaderConstants.ROLE_USER);
        user.setStatus("ENABLED");

        userMapper.insert(user);
        log.info("用户注册成功: id={}, username={}", user.getId(), username);
        return user.getId();
    }

    // ============================================================
    // 登录
    // ============================================================

    /**
     * 用户登录
     *
     * @param dto 登录请求
     * @return 登录响应（包含 token + user）
     * @throws BusinessException 2001 LOGIN_ERROR 用户名或密码错误 / 2002 USER_DISABLED
     */
    public LoginVO login(LoginDTO dto) {
        String username = dto.getUsername().trim();

        User user = userMapper.selectByUsername(username);
        if (user == null) {
            // 防止用户名枚举：统一返回"用户名或密码错误"
            throw new BusinessException(ResultCode.LOGIN_ERROR);
        }

        if (!user.isEnabled()) {
            throw new BusinessException(ResultCode.USER_DISABLED);
        }

        if (!passwordEncoder.matches(dto.getPassword(), user.getPasswordHash())) {
            log.warn("用户 {} 登录失败：密码错误", username);
            throw new BusinessException(ResultCode.LOGIN_ERROR);
        }

        // 签发 JWT
        String token = JwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole(), jwtProperties);
        long expiration = System.currentTimeMillis() + jwtProperties.getExpirationMillis();

        log.info("用户登录成功: id={}, username={}, role={}", user.getId(), username, user.getRole());
        return LoginVO.builder()
                .token(token)
                .expiration(expiration)
                .user(toVO(user))
                .build();
    }

    // ============================================================
    // 注销
    // ============================================================

    /**
     * 用户注销
     * <p>
     * 简单版：直接返回成功（前端丢弃 token）。
     * 加分版：将 token 的 jti 写入 Redis 黑名单，TTL = token 剩余有效期。
     * </p>
     *
     * @param token 当前 Token（从请求头解析）
     */
    public void logout(String token) {
        if (token == null || token.isBlank()) {
            return;
        }
        try {
            Claims claims = JwtUtil.parseToken(token, jwtProperties);
            tokenBlacklistService.blacklist(claims);
        } catch (AuthException e) {
            // Token 已过期 / 无效，无需加入黑名单
            log.info("注销时 Token 已无效，跳过黑名单: {}", e.getMessage());
        }
    }

    /**
     * 校验 Token 是否在黑名单中（供网关调用）
     */
    public boolean isTokenBlacklisted(Claims claims) {
        if (claims == null) {
            return false;
        }
        String jti = claims.getId();
        if (jti == null || jti.isBlank()) {
            jti = claims.getSubject();
        }
        return tokenBlacklistService.isBlacklisted(jti);
    }

    // ============================================================
    // 个人信息
    // ============================================================

    /**
     * 获取当前用户个人信息
     *
     * @param userId 网关透传的用户 ID
     * @return 用户 VO
     * @throws BusinessException 2000 USER_NOT_FOUND
     */
    public UserVO getProfile(Long userId) {
        if (userId == null) {
            throw AuthException.unauthorized();
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        return toVO(user);
    }

    /**
     * 根据用户名查询（管理后台用，加分项）
     */
    public User getByUsername(String username) {
        return userMapper.selectByUsername(username);
    }

    // ============================================================
    // 内部工具
    // ============================================================

    /**
     * User → UserVO（剥离 passwordHash）
     */
    private UserVO toVO(User user) {
        if (user == null) {
            return null;
        }
        return UserVO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .role(user.getRole())
                .status(user.getStatus())
                .createdAt(user.getCreateTime())
                .build();
    }
}
