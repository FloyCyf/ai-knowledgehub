package com.ai.knowledgehub.gateway.ratelimit;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;

/**
 * Gateway 固定窗口限流默认配置。
 */
@ConfigurationProperties(prefix = "rate-limit")
@RefreshScope
public class RateLimitProperties {

    private boolean enabled = true;

    private ArticleDetail articleDetail = new ArticleDetail();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public ArticleDetail getArticleDetail() {
        return articleDetail;
    }

    public void setArticleDetail(ArticleDetail articleDetail) {
        this.articleDetail = articleDetail;
    }

    public static class ArticleDetail {
        private int windowSeconds = 10;
        private int maxRequests = 20;

        public int getWindowSeconds() {
            return windowSeconds;
        }

        public void setWindowSeconds(int windowSeconds) {
            this.windowSeconds = windowSeconds;
        }

        public int getMaxRequests() {
            return maxRequests;
        }

        public void setMaxRequests(int maxRequests) {
            this.maxRequests = maxRequests;
        }
    }
}
