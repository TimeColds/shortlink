package com.timecold.gateway.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.timecold.gateway.config.FilterConfig;
import com.timecold.gateway.dto.GatewayErrorResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Objects;

@Component
@Slf4j
public class TokenValidateGatewayFilterFactory extends AbstractGatewayFilterFactory<FilterConfig> {

    private final StringRedisTemplate stringRedisTemplate;

    private final ObjectMapper objectMapper;

    public TokenValidateGatewayFilterFactory(StringRedisTemplate stringRedisTemplate, ObjectMapper objectMapper) {
        super(FilterConfig.class);
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public GatewayFilter apply(FilterConfig config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            String requestPath = request.getPath().toString();
            String requestMethod = request.getMethod().name();
            if (!isPathInWhiteList(requestPath, requestMethod, config.getWhitePathList())) {
                String username = request.getHeaders().getFirst("username");
                String token = request.getHeaders().getFirst("token");
                Object userInfo;
                if (StringUtils.isNoneBlank(username, token) && (userInfo = stringRedisTemplate.opsForHash().get("user:login:" + username, token)) != null) {
                    try {
                        JsonNode userInfoJsonObject = objectMapper.readTree(userInfo.toString());
                        ServerHttpRequest mutatedRequest = request.mutate()
                                .header("uid", userInfoJsonObject.get("uid").asText())
                                .header("username",userInfoJsonObject.get("username").asText())
                                .build();
                        return chain.filter(exchange.mutate().request(mutatedRequest).build());
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                }
                ServerHttpResponse response = exchange.getResponse();
                response.setStatusCode(HttpStatus.UNAUTHORIZED);
                return response.writeWith(Mono.fromSupplier(() -> {
                    DataBufferFactory bufferFactory = response.bufferFactory();
                    GatewayErrorResult resultMessage = GatewayErrorResult.builder()
                            .status(HttpStatus.UNAUTHORIZED.value())
                            .message("Token validation error")
                            .build();
                    try {
                        return bufferFactory.wrap(objectMapper.writeValueAsString(resultMessage).getBytes());
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                }));
            }
            return chain.filter(exchange);
        };
    }
    private boolean isPathInWhiteList(String requestPath, String requestMethod, List<String> whitePathList) {
        return (!CollectionUtils.isEmpty(whitePathList) && whitePathList.stream().anyMatch(requestPath::startsWith)) || (Objects.equals(requestPath, "/api/v1/short_link/admin/user") && Objects.equals(requestMethod, "POST"));
    }
}
