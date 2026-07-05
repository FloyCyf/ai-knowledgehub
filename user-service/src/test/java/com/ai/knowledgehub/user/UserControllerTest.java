package com.ai.knowledgehub.user;

import com.ai.knowledgehub.common.config.JwtProperties;
import com.ai.knowledgehub.common.config.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * UserController 端到端 MockMvc 测试
 * <p>
 * 覆盖阶段 2 审查方案 § 2.3 的 9 个核心接口测试用例。
 * </p>
 *
 * @author AI KnowledgeHub Team
 */
@SpringBootTest
@ActiveProfiles("dev")
@Transactional
@DisplayName("UserController MockMvc 测试")
class UserControllerTest {

    @Autowired
    private WebApplicationContext wac;

    @Autowired
    private JwtProperties jwtProperties;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
        JwtUtil.setDefaultProperties(jwtProperties);
    }

    // ============================================================
    // 注册
    // ============================================================

    @Test
    @DisplayName("POST /api/user/register 成功")
    void register_success() throws Exception {
        mockMvc.perform(post("/api/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("username", "alice", "password", "123456"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.userId").exists());
    }

    @Test
    @DisplayName("POST /api/user/register 重复用户名返回 2003")
    void register_duplicateUsername_returns2003() throws Exception {
        String body = objectMapper.writeValueAsString(
                Map.of("username", "bobby", "password", "123456"));

        // 第一次成功
        mockMvc.perform(post("/api/user/register")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(jsonPath("$.code").value(200));

        // 第二次失败
        mockMvc.perform(post("/api/user/register")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(jsonPath("$.code").value(2003));
    }

    @Test
    @DisplayName("POST /api/user/register 密码为空返回 422")
    void register_emptyPassword_returns422() throws Exception {
        mockMvc.perform(post("/api/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"charlie\",\"password\":\"\"}"))
                .andExpect(jsonPath("$.code").value(422));
    }

    // ============================================================
    // 登录
    // ============================================================

    @Test
    @DisplayName("POST /api/user/login 成功返回 token")
    void login_success() throws Exception {
        // 先注册
        mockMvc.perform(post("/api/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("username", "dave", "password", "123456"))))
                .andExpect(jsonPath("$.code").value(200));

        // 再登录
        mockMvc.perform(post("/api/user/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("username", "dave", "password", "123456"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.token").exists())
                .andExpect(jsonPath("$.data.user.username").value("dave"))
                .andExpect(jsonPath("$.data.user.role").value("USER"));
    }

    @Test
    @DisplayName("POST /api/user/login 错误密码返回 2001")
    void login_wrongPassword_returns2001() throws Exception {
        // 注册
        mockMvc.perform(post("/api/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        Map.of("username", "eve", "password", "123456"))));

        // 错误密码
        mockMvc.perform(post("/api/user/login")
                        .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        Map.of("username", "eve", "password", "wrong"))))
                .andExpect(jsonPath("$.code").value(2001));
    }

    // ============================================================
    // 个人信息
    // ============================================================

    @Test
    @DisplayName("GET /api/user/profile 不带 token 返回 401")
    void profile_noToken_returns401() throws Exception {
        mockMvc.perform(get("/api/user/profile"))
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    @DisplayName("GET /api/user/profile 未透传用户 ID 返回 401")
    void profile_missingGatewayHeader_returns401() throws Exception {
        mockMvc.perform(get("/api/user/profile")
                        .header("Authorization", "Bearer invalid.token.here"))
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    @DisplayName("GET /api/user/profile 带网关透传用户 ID 返回 200")
    void profile_validToken_returns200() throws Exception {
        // 注册并取 userId，模拟 gateway 校验 token 后向下游透传 X-User-Id
        String registerResponse = mockMvc.perform(post("/api/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        Map.of("username", "frank", "password", "123456"))))
                .andReturn().getResponse().getContentAsString();
        String userId = objectMapper.readTree(registerResponse).get("data").get("userId").asText();

        // 访问 profile
        mockMvc.perform(get("/api/user/profile")
                        .header("X-User-Id", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.username").value("frank"));
    }
}
