package com.ai.knowledgehub.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * JWT 配置项
 * <p>
 * 通过 {@code @EnableConfigurationProperties(JwtProperties.class)} 激活，
 * 各服务在自己的启动类上启用即可。默认值与 {@code JwtUtil.DEFAULT_*} 保持一致，
 * 保证单元测试 / 未配置环境仍能运行。
 * </p>
 *
 * <p>配置示例（application.yml）：</p>
 * <pre>
 * jwt:
 *   secret: ${JWT_SECRET:your-secret-key-must-be-at-least-32-bytes-long}
 *   expiration-millis: 86400000  # 24h
 *   issuer: ai-knowledgehub
 * </pre>
 *
 * @author AI KnowledgeHub Team
 */
@Data
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    /**
     * 默认密钥（64 字符，仅用于本地开发 / 单元测试）
     */
    public static final String DEFAULT_SECRET =
            "ai-knowledgehub-default-secret-key-for-jwt-token-generation-2024-do-not-use-in-prod";

    /**
     * 默认过期时间：24h
     */
    public static final long DEFAULT_EXPIRATION_MILLIS = 24 * 60 * 60 * 1000L;

    /**
     * JWT 签名密钥（HS256 要求至少 32 字节）
     */
    private String secret = DEFAULT_SECRET;

    /**
     * Token 过期时间（毫秒）
     */
    private long expirationMillis = DEFAULT_EXPIRATION_MILLIS;

    /**
     * 签发者（写入 JWT iss 声明）
     */
    private String issuer = "ai-knowledgehub";

    /**
     * Token 前缀
     */
    private String tokenPrefix = "Bearer ";

    /**
     * 校验密钥长度，HS256 至少需要 32 字节
     */
    public void validate() {
        if (secret == null || secret.getBytes().length < 32) {
            throw new IllegalStateException(
                    "JWT 密钥长度不足 32 字节，请配置 jwt.secret（当前长度："
                            + (secret == null ? 0 : secret.getBytes().length) + "）");
        }
    }
}
