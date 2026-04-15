package com.marrylink.websocket;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.marrylink.entity.ChatMessage;
import com.marrylink.service.IChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 聊天 WebSocket 处理器
 * 管理在线用户连接，处理实时消息收发
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private final IChatService chatService;

    /**
     * 在线用户会话映射: accountId -> WebSocketSession
     */
    private static final ConcurrentHashMap<Long, WebSocketSession> ONLINE_SESSIONS = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        Long accountId = getAccountId(session);
        if (accountId != null) {
            // 如果用户已有旧连接，关闭旧连接
            WebSocketSession oldSession = ONLINE_SESSIONS.put(accountId, session);
            if (oldSession != null && oldSession.isOpen()) {
                try {
                    oldSession.close(CloseStatus.NORMAL);
                } catch (IOException e) {
                    log.warn("关闭旧会话失败: {}", e.getMessage());
                }
            }
            log.info("[WebSocket] 用户上线: accountId={}, 当前在线人数: {}", accountId, ONLINE_SESSIONS.size());
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        Long senderAccountId = getAccountId(session);
        if (senderAccountId == null) return;

        try {
            JSONObject json = JSONUtil.parseObj(message.getPayload());
            String type = json.getStr("type");

            if (type == null) return;

            switch (type) {
                case "CHAT":
                    handleChatMessage(senderAccountId, json);
                    break;
                case "READ":
                    handleReadReceipt(senderAccountId, json);
                    break;
                case "TYPING":
                    handleTyping(senderAccountId, json);
                    break;
                case "PING":
                    // 心跳回复
                    sendToUser(senderAccountId, createPongMessage());
                    break;
                default:
                    log.warn("[WebSocket] 未知消息类型: {}", type);
            }
        } catch (Exception e) {
            log.error("[WebSocket] 处理消息异常: {}", e.getMessage(), e);
        }
    }

    /**
     * 处理聊天消息
     */
    private void handleChatMessage(Long senderAccountId, JSONObject json) {
        Long conversationId = json.getLong("conversationId");
        Long receiverId = json.getLong("receiverId");
        String content = json.getStr("content");
        String msgType = json.getStr("msgType", "text");

        if (conversationId == null || receiverId == null || content == null || content.isEmpty()) {
            log.warn("[WebSocket] 消息参数不完整");
            return;
        }

        // 保存消息到数据库
        ChatMessage chatMessage = chatService.sendMessage(conversationId, senderAccountId, receiverId, content, msgType);

        // 构建推送消息
        JSONObject pushMsg = new JSONObject();
        pushMsg.set("type", "CHAT");
        pushMsg.set("id", chatMessage.getId());
        pushMsg.set("conversationId", conversationId);
        pushMsg.set("senderId", senderAccountId);
        pushMsg.set("receiverId", receiverId);
        pushMsg.set("content", content);
        pushMsg.set("msgType", msgType);
        pushMsg.set("createTime", chatMessage.getCreateTime() != null ?
                chatMessage.getCreateTime().toString() : LocalDateTime.now().toString());

        // 推送给接收者
        sendToUser(receiverId, pushMsg.toString());

        log.info("[WebSocket] 消息已发送: {} -> {}, conversationId={}", senderAccountId, receiverId, conversationId);
    }

    /**
     * 处理已读回执
     */
    private void handleReadReceipt(Long senderAccountId, JSONObject json) {
        Long conversationId = json.getLong("conversationId");
        if (conversationId == null) return;

        chatService.markConversationRead(conversationId, senderAccountId);

        // 可选：通知对方消息已读
        // 暂不实现，以减少网络开销
    }

    /**
     * 处理正在输入状态
     */
    private void handleTyping(Long senderAccountId, JSONObject json) {
        Long conversationId = json.getLong("conversationId");
        if (conversationId == null) return;

        // 通过会话查找接收者并转发
        // 简化处理：前端已经知道 conversationId 对应的对方用户
        // 这里将 TYPING 消息广播给该会话的另一方
        JSONObject pushMsg = new JSONObject();
        pushMsg.set("type", "TYPING");
        pushMsg.set("conversationId", conversationId);
        pushMsg.set("senderId", senderAccountId);

        // 遍历在线用户推送（简化处理，生产环境可以通过会话查找精确用户）
        // 这里信赖前端传来的 conversationId，不额外查库
        for (Map.Entry<Long, WebSocketSession> entry : ONLINE_SESSIONS.entrySet()) {
            if (!entry.getKey().equals(senderAccountId) && entry.getValue().isOpen()) {
                // 发送给所有非自己的在线用户，由前端根据 conversationId 过滤
                sendToUser(entry.getKey(), pushMsg.toString());
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long accountId = getAccountId(session);
        if (accountId != null) {
            ONLINE_SESSIONS.remove(accountId, session);
            log.info("[WebSocket] 用户下线: accountId={}, 当前在线人数: {}", accountId, ONLINE_SESSIONS.size());
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        Long accountId = getAccountId(session);
        log.error("[WebSocket] 传输错误: accountId={}, error={}", accountId, exception.getMessage());
        if (session.isOpen()) {
            try {
                session.close(CloseStatus.SERVER_ERROR);
            } catch (IOException e) {
                log.error("关闭会话失败: {}", e.getMessage());
            }
        }
        if (accountId != null) {
            ONLINE_SESSIONS.remove(accountId, session);
        }
    }

    /**
     * 发送消息给指定用户
     */
    private void sendToUser(Long accountId, String message) {
        WebSocketSession session = ONLINE_SESSIONS.get(accountId);
        if (session != null && session.isOpen()) {
            try {
                session.sendMessage(new TextMessage(message));
            } catch (IOException e) {
                log.error("[WebSocket] 发送消息失败: accountId={}, error={}", accountId, e.getMessage());
            }
        }
    }

    /**
     * 获取会话中的 accountId
     */
    private Long getAccountId(WebSocketSession session) {
        Object accountId = session.getAttributes().get("accountId");
        return accountId instanceof Long ? (Long) accountId : null;
    }

    /**
     * 创建心跳响应
     */
    private String createPongMessage() {
        JSONObject pong = new JSONObject();
        pong.set("type", "PONG");
        return pong.toString();
    }

    /**
     * 检查用户是否在线
     */
    public boolean isUserOnline(Long accountId) {
        WebSocketSession session = ONLINE_SESSIONS.get(accountId);
        return session != null && session.isOpen();
    }

    /**
     * 获取在线用户数量
     */
    public int getOnlineCount() {
        return ONLINE_SESSIONS.size();
    }
}
