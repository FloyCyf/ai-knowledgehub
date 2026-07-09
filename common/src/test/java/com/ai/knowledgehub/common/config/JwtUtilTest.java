package com.ai.knowledgehub.common.config;

import com.ai.knowledgehub.common.exception.AuthException;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("JwtUtil unit tests")
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
    @DisplayName("generated token can be parsed")
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
    @DisplayName("tampered token throws TOKEN_INVALID")
    void invalidSignature_throws() {
        String token = JwtUtil.generateToken(1L, "alice", "USER");
        String[] parts = token.split("\\.");
        char replacement = parts[1].charAt(0) == 'a' ? 'b' : 'a';
        parts[1] = replacement + parts[1].substring(1);
        String tampered = String.join(".", parts);

        AuthException ex = assertThrows(AuthException.class,
                () -> JwtUtil.parseToken(tampered));
        assertEquals(2007, ex.getCode());
    }

    @Test
    @DisplayName("malformed token throws TOKEN_INVALID")
    void malformedToken_throws() {
        AuthException ex = assertThrows(AuthException.class,
                () -> JwtUtil.parseToken("not-a-jwt"));
        assertEquals(2007, ex.getCode());
    }

    @Test
    @DisplayName("expired token throws TOKEN_EXPIRED")
    void expiredToken_throws() {
        JwtProperties shortExp = new JwtProperties();
        shortExp.setSecret(properties.getSecret());
        shortExp.setExpirationMillis(0L);
        shortExp.setIssuer(properties.getIssuer());

        String token = JwtUtil.generateToken(1L, "alice", "USER", shortExp);

        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        AuthException ex = assertThrows(AuthException.class,
                () -> JwtUtil.parseToken(token));
        assertEquals(2008, ex.getCode());
    }

    @Test
    @DisplayName("validateToken returns true for valid token")
    void validateToken_valid() {
        String token = JwtUtil.generateToken(1L, "alice", "USER");
        assertTrue(JwtUtil.validateToken(token));
    }

    @Test
    @DisplayName("validateToken returns false for invalid token")
    void validateToken_invalid_returnsFalse() {
        assertFalse(JwtUtil.validateToken("not-a-jwt"));
    }

    @Test
    @DisplayName("extractToken removes Bearer prefix")
    void extractToken() {
        assertEquals("abc.def.ghi", JwtUtil.extractToken("Bearer abc.def.ghi"));
        assertNull(JwtUtil.extractToken("Basic xxx"));
        assertNull(JwtUtil.extractToken(null));
    }

    @Test
    @DisplayName("refreshToken keeps user claims")
    void refreshToken() {
        String oldToken = JwtUtil.generateToken(99L, "bob", "ADMIN");
        Claims oldClaims = JwtUtil.parseToken(oldToken);
        String newToken = JwtUtil.refreshToken(oldToken);
        Claims newClaims = JwtUtil.parseToken(newToken);

        assertEquals(JwtUtil.getUserId(oldClaims), JwtUtil.getUserId(newClaims));
        assertEquals(JwtUtil.getUsername(oldClaims), JwtUtil.getUsername(newClaims));
        assertEquals(JwtUtil.getRole(oldClaims), JwtUtil.getRole(newClaims));
        assertNotEquals(oldToken, newToken);
    }

    @Test
    @DisplayName("short secret throws IllegalStateException")
    void shortSecret_throws() {
        JwtProperties bad = new JwtProperties();
        bad.setSecret("short");
        bad.setExpirationMillis(3600_000L);

        assertThrows(IllegalStateException.class,
                () -> JwtUtil.generateToken(1L, "alice", "USER", bad));
    }
}
