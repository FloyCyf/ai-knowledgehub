package com.ai.knowledgehub.gateway.ratelimit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * Publishes the article detail rate-limit configuration to Nacos Config.
 */
@Component
public class NacosRateLimitConfigPublisher {

    private static final String DATA_ID = "gateway-service.yml";
    private static final String DEFAULT_GROUP = "DEFAULT_GROUP";

    private final WebClient webClient;
    private final String group;
    private final String namespace;

    public NacosRateLimitConfigPublisher(
            WebClient.Builder webClientBuilder,
            @Value("${spring.cloud.nacos.config.server-addr:localhost:8848}") String serverAddr,
            @Value("${spring.cloud.nacos.config.group:DEFAULT_GROUP}") String group,
            @Value("${spring.cloud.nacos.config.namespace:}") String namespace) {
        this.webClient = webClientBuilder
                .baseUrl(normalizeBaseUrl(serverAddr))
                .build();
        this.group = (group == null || group.isBlank()) ? DEFAULT_GROUP : group;
        this.namespace = namespace == null ? "" : namespace;
    }

    public Mono<RateLimitConfig> publishArticleDetailConfig(RateLimitConfig config) {
        BodyInserters.FormInserter<String> form = BodyInserters
                .fromFormData("dataId", DATA_ID)
                .with("group", group)
                .with("type", "yaml")
                .with("content", toYaml(config));
        if (!namespace.isBlank()) {
            form.with("tenant", namespace);
        }

        return webClient.post()
                .uri("/nacos/v1/cs/configs")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .bodyToMono(String.class)
                .flatMap(body -> "true".equalsIgnoreCase(body.trim())
                        ? Mono.just(config)
                        : Mono.error(new IllegalStateException("Nacos publish returned: " + body)));
    }

    private String toYaml(RateLimitConfig config) {
        return """
                rate-limit:
                  enabled: %s
                  article-detail:
                    window-seconds: %d
                    max-requests: %d
                """.formatted(config.enabled(), config.windowSeconds(), config.maxRequests());
    }

    private String normalizeBaseUrl(String serverAddr) {
        String value = serverAddr == null || serverAddr.isBlank() ? "localhost:8848" : serverAddr.trim();
        if (!value.startsWith("http://") && !value.startsWith("https://")) {
            value = "http://" + value;
        }
        return value.replaceAll("/+$", "");
    }
}
