package com.ai.knowledgehub.user;

import com.ai.knowledgehub.common.config.JwtProperties;
import com.ai.knowledgehub.common.config.JwtUtil;
import com.ai.knowledgehub.common.exception.BusinessException;
import com.ai.knowledgehub.common.result.ResultCode;
import com.ai.knowledgehub.user.dto.LoginDTO;
import com.ai.knowledgehub.user.dto.RegisterDTO;
import com.ai.knowledgehub.user.entity.User;
import com.ai.knowledgehub.user.mapper.UserMapper;
import com.ai.knowledgehub.user.service.UserService;
import com.ai.knowledgehub.user.vo.LoginVO;
import com.ai.knowledgehub.user.vo.UserVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * UserService 单元测试
 * <p>
 * 使用真实 H2 数据库（dev profile），不 Mock MyBatis-Plus，
 * 保证 SQL 正确性和事务回滚行为。
 * </p>
 *
 * @author AI KnowledgeHub Team
 */
@SpringBootTest
@ActiveProfiles("dev")
@Transactional
@DisplayName("UserService 单元测试")
class UserServiceTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private JwtProperties jwtProperties;

    @BeforeEach
    void setUp() {
        // 注入静态 JwtUtil（与生产相同路径）
        JwtUtil.setDefaultProperties(jwtProperties);
        // 清理（@Transactional 会自动回滚，但显式清理更清晰）
    }

    // ============================================================
    // 注册测试
    // ============================================================

    @Test
    @DisplayName("注册新用户 - 成功")
    void register_newUser_success() {
        RegisterDTO dto = new RegisterDTO();
        dto.setUsername("alice");
        dto.setPassword("123456");

        Long userId = userService.register(dto);
        assertNotNull(userId, "新用户 ID 不应为空");

        User saved = userMapper.selectById(userId);
        assertNotNull(saved);
        assertEquals("alice", saved.getUsername());
        assertEquals("USER", saved.getRole());
        assertEquals("ENABLED", saved.getStatus());
        assertNotNull(saved.getPasswordHash());
        assertTrue(saved.getPasswordHash().startsWith("$2a$"), "密码必须 BCrypt 加密");
        assertNotEquals("123456", saved.getPasswordHash(), "不能明文存储");
    }

    @Test
    @DisplayName("注册 - 用户名已存在抛 USERNAME_EXISTS")
    void register_duplicateUsername_throws() {
        // 先注册一次
        RegisterDTO dto = new RegisterDTO();
        dto.setUsername("bob");
        dto.setPassword("123456");
        userService.register(dto);

        // 重复注册
        BusinessException ex = assertThrows(BusinessException.class,
                () -> userService.register(dto));
        assertEquals(ResultCode.USERNAME_EXISTS.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("注册 - 中文用户名合法")
    void register_chineseUsername_success() {
        RegisterDTO dto = new RegisterDTO();
        dto.setUsername("张三");
        dto.setPassword("123456");
        Long userId = userService.register(dto);
        assertNotNull(userId);
        assertEquals("张三", userMapper.selectById(userId).getUsername());
    }

    // ============================================================
    // 登录测试
    // ============================================================

    @Test
    @DisplayName("登录 - 正确密码返回 token + user")
    void login_correctPassword_returnsToken() {
        // 准备
        RegisterDTO reg = new RegisterDTO();
        reg.setUsername("charlie");
        reg.setPassword("123456");
        userService.register(reg);

        // 登录
        LoginDTO login = new LoginDTO();
        login.setUsername("charlie");
        login.setPassword("123456");
        LoginVO vo = userService.login(login);

        assertNotNull(vo.getToken());
        assertFalse(vo.getToken().isBlank());
        assertNotNull(vo.getExpiration());
        assertNotNull(vo.getUser());
        assertEquals("charlie", vo.getUser().getUsername());
        assertEquals("USER", vo.getUser().getRole());
    }

    @Test
    @DisplayName("登录 - 错误密码抛 LOGIN_ERROR")
    void login_wrongPassword_throws() {
        RegisterDTO reg = new RegisterDTO();
        reg.setUsername("dave");
        reg.setPassword("123456");
        userService.register(reg);

        LoginDTO login = new LoginDTO();
        login.setUsername("dave");
        login.setPassword("wrong");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> userService.login(login));
        assertEquals(ResultCode.LOGIN_ERROR.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("登录 - 不存在用户抛 LOGIN_ERROR（防枚举）")
    void login_userNotExists_throws() {
        LoginDTO login = new LoginDTO();
        login.setUsername("nobody");
        login.setPassword("123456");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> userService.login(login));
        assertEquals(ResultCode.LOGIN_ERROR.getCode(), ex.getCode(),
                "用户不存在应统一返回 LOGIN_ERROR，防止用户名枚举");
    }

    @Test
    @DisplayName("登录 - 登录生成的 token 可被 JwtUtil 解析")
    void login_tokenIsValid() {
        RegisterDTO reg = new RegisterDTO();
        reg.setUsername("eve");
        reg.setPassword("123456");
        userService.register(reg);

        LoginDTO login = new LoginDTO();
        login.setUsername("eve");
        login.setPassword("123456");
        String token = userService.login(login).getToken();

        // 解析 token，userId 应等于注册的 ID
        Long userId = userMapper.selectByUsername("eve").getId();
        var claims = JwtUtil.parseToken(token, jwtProperties);
        assertEquals(userId, JwtUtil.getUserId(claims));
        assertEquals("eve", JwtUtil.getUsername(claims));
        assertEquals("USER", JwtUtil.getRole(claims));
    }

    // ============================================================
    // 个人信息测试
    // ============================================================

    @Test
    @DisplayName("获取个人信息 - 成功")
    void getProfile_success() {
        RegisterDTO reg = new RegisterDTO();
        reg.setUsername("frank");
        reg.setPassword("123456");
        Long userId = userService.register(reg);

        UserVO vo = userService.getProfile(userId);
        assertNotNull(vo);
        assertEquals("frank", vo.getUsername());
        assertEquals("ENABLED", vo.getStatus());
    }

    @Test
    @DisplayName("获取个人信息 - userId 为 null 抛 UNAUTHORIZED")
    void getProfile_nullUserId_throws() {
        assertThrows(com.ai.knowledgehub.common.exception.AuthException.class,
                () -> userService.getProfile(null));
    }

    @Test
    @DisplayName("获取个人信息 - 用户不存在抛 USER_NOT_FOUND")
    void getProfile_userNotFound_throws() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> userService.getProfile(99999L));
        assertEquals(ResultCode.USER_NOT_FOUND.getCode(), ex.getCode());
    }

    // ============================================================
    // 注销测试
    // ============================================================

    @Test
    @DisplayName("注销 - 正常调用不抛异常")
    void logout_validToken_noException() {
        RegisterDTO reg = new RegisterDTO();
        reg.setUsername("grace");
        reg.setPassword("123456");
        userService.register(reg);

        LoginDTO login = new LoginDTO();
        login.setUsername("grace");
        login.setPassword("123456");
        String token = userService.login(login).getToken();

        assertDoesNotThrow(() -> userService.logout(token));
    }

    @Test
    @DisplayName("注销 - null token 不抛异常")
    void logout_nullToken_noException() {
        assertDoesNotThrow(() -> userService.logout(null));
        assertDoesNotThrow(() -> userService.logout(""));
    }
}
