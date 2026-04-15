package com.marrylink.websocket;

import com.marrylink.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

/**
 * WebSocket 握手拦截器
 * 在 WebSocket 连接建立前验证 JWT Token
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatWebSocketInterceptor implements HandshakeInterceptor {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        if (request instanceof ServletServerHttpRequest) {
            ServletServerHttpRequest servletRequest = (ServletServerHttpRequest) request;
            String token = servletRequest.getServletRequest().getParameter("token");

            if (token == null || token.isEmpty()) {
                log.warn("[WebSocket] 连接被拒绝：缺少 token 参数");
                return false;
            }

            // 验证 Token（JWT + Redis）
            if (!jwtTokenProvider.validateTokenWithRedis(token)) {
                log.warn("[WebSocket] 连接被拒绝：token 无效或已过期");
                return false;
            }

            // 提取用户信息到 attributes，供 Handler 使用
            Long accountId = jwtTokenProvider.getAccountIdFromToken(token);
            Long refId = jwtTokenProvider.getRefIdFromToken(token);
            String userType = jwtTokenProvider.getUserTypeFromToken(token);

            attributes.put("accountId", accountId);
            attributes.put("refId", refId);
            attributes.put("userType", userType);
            attributes.put("token", token);

            log.info("[WebSocket] 握手成功：accountId={}, userType={}", accountId, userType);
            return true;
        }

        return false;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // 握手后无需额外处理
    }
}
