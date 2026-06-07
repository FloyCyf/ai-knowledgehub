package com.ai.knowledgehub.common.config;

import com.ai.knowledgehub.common.exception.AuthException;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JwtUtil 单元测试
 *
 * @author AI KnowledgeHub Team
 */
@DisplayName("JwtUtil 单元测试")
class JwtUtilTest {

    private JwtProperties properties;

    @BeforeEach
    void setUp() {
        properties = new JwtProperties();
        properties.setSecret("test-secret-key-must-be-at-least-32-bytes-long-for-jsws");
        properties.setExpirationMillis(3600_000L);
        properties.setIssuer("test-issuer");
        JwtUtil.setDefaultProperties(properties);
    }

    @Test
    @DisplayName("生成 Token 后可解析出原始数据")
    void generateAndParse() {
        String token = JwtUtil.generateToken(123L, "alice", "USER");
        assertNotNull(token);
        assertFalse(token.isBlank());

        Claims claims = JwtUtil.parseToken(token);
        assertEquals(123L, JwtUtil.getUserId(claims));
        assertEquals("alice", JwtUtil.getUsername(claims));
        assertEquals("USER", JwtUtil.getRole(claims));
        assertEquals("test-issuer", claims.getIssuer());
        assertEquals("alice", claims.getSubject());
    }

    @Test
    @DisplayName("无效签名抛 AuthException(TOKEN_INVALID)")
    void invalidSignature_throws() {
        String token = JwtUtil.generateToken(1L, "alice", "USER");
        // 篡改 token 最后一位
        String tampered = token.substring(0, token.length() - 1) + "x";

        AuthException ex = assertThrows(AuthException.class,
                () -> JwtUtil.parseToken(tampered));
        assertTrue(ex.getMessage().contains("签名") || ex.getMessage().contains("无效")
                || ex.getCode() == 2007);
    }

    @Test
    @DisplayName("错误格式抛 AuthException(TOKEN_INVALID)")
    void malformedToken_throws() {
        AuthException ex = assertThrows(AuthException.class,
                () -> JwtUtil.parseToken("not-a-jwt"));
        assertEquals(2007, ex.getCode());
    }

    @Test
    @DisplayName("过期 Token 抛 AuthException(TOKEN_EXPIRED)")
    void expiredToken_throws() {
        JwtProperties shortExp = new JwtProperties();
        shortExp.setSecret(properties.getSecret());
        shortExp.setExpirationMillis(0L);  // 立即过期
        shortExp.setIssuer(properties.getIssuer());

        String token = JwtUtil.generateToken(1L, "alice", "USER", shortExp);

        // 等待 10ms 确保过期
        try { Thread.sleep(10); } catch (InterruptedException e) {}

        AuthException ex = assertThrows(AuthException.class,
                () -> JwtUtil.parseToken(token));
        assertEquals(2008, ex.getCode());
    }

    @Test
    @DisplayName("validateToken 对有效 Token 返回 true")
    void validateToken_valid() {
        String token = JwtUtil.generateToken(1L, "alice", "USER");
        assertTrue(JwtUtil.validateToken(token));
    }

    @Test
    @DisplayName("validateToken 对无效 Token 返回 false（不抛异常）")
    void validateToken_invalid_returnsFalse() {
        assertFalse(JwtUtil.validateToken("not-a-jwt"));
    }

    @Test
    @DisplayName("extractToken 正确去除 Bearer 前缀")
    void extractToken() {
        assertEquals("abc.def.ghi", JwtUtil.extractToken("Bearer abc.def.ghi"));
        assertNull(JwtUtil.extractToken("Basic xxx"));
        assertNull(JwtUtil.extractToken(null));
    }

    @Test
    @DisplayName("refreshToken 生成新 Token 且数据一致")
    void refreshToken() {
        String oldToken = JwtUtil.generateToken(99L, "bob", "ADMIN");
        Claims oldClaims = JwtUtil.parseToken(oldToken);
        String newToken = JwtUtil.refreshToken(oldToken);
        Claims newClaims = JwtUtil.parseToken(newToken);

        assertEquals(JwtUtil.getUserId(oldClaims), JwtUtil.getUserId(newClaims));
        assertEquals(JwtUtil.getUsername(oldClaims), JwtUtil.getUsername(newClaims));
        assertEquals(JwtUtil.getRole(oldClaims), JwtUtil.getRole(newClaims));
        assertNotEquals(oldToken, newToken, "新 Token 必须不同（iat 时间不同）");
    }

    @Test
    @DisplayName("密钥不足 32 字节抛 IllegalStateException")
    void shortSecret_throws() {
        JwtProperties bad = new JwtProperties();
        bad.setSecret("short");
        bad.setExpirationMillis(3600_000L);

        assertThrows(IllegalStateException.class,
                () -> JwtUtil.generateToken(1L, "alice", "USER", bad));
    }
}
