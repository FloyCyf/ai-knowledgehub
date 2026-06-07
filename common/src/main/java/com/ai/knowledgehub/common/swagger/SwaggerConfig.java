package com.ai.knowledgehub.common.swagger;

import com.ai.knowledgehub.common.constant.HeaderConstants;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger / Knife4j 基础配置
 * <p>
 * 各微服务引入本配置类后，自动获得：
 * </p>
 * <ul>
 *     <li>OpenAPI 3 文档：{@code /v3/api-docs}</li>
 *     <li>Swagger UI：{@code /swagger-ui/index.html}</li>
 *     <li>Knife4j 增强 UI：{@code /doc.html}</li>
 *     <li>JWT Bearer 鉴权按钮（点击后 Swagger UI 自动带 token 请求）</li>
 * </ul>
 *
 * <p>通过配置 {@code swagger.enabled=false} 关闭文档（生产环境推荐关闭）。</p>
 *
 * @author AI KnowledgeHub Team
 */
@Configuration
@ConditionalOnClass(name = "io.swagger.v3.oas.models.OpenAPI")
@ConditionalOnProperty(prefix = "swagger", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SwaggerConfig {

    /**
     * 鉴权头名称（与 JwtUtil 中保持一致）
     */
    private static final String SECURITY_SCHEME_NAME = "Bearer Authentication";

    /**
     * 构建 OpenAPI 元数据
     */
    @Bean
    public OpenAPI aiKnowledgeHubOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("AI-KnowledgeHub API")
                        .description("基于 AI 的智能知识库与内容发布平台 - 统一 API 文档")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("AI KnowledgeHub Team")
                                .email("akh-team@example.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .in(SecurityScheme.In.HEADER)
                                .name(HeaderConstants.AUTHORIZATION)
                                .description("登录后从 /api/user/login 拿到的 token，填入此处即可鉴权")))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME));
    }
}
