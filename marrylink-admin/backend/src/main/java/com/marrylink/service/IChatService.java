package com.marrylink.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.marrylink.common.PageResult;
import com.marrylink.entity.ChatConversation;
import com.marrylink.entity.ChatMessage;

import java.util.Map;

/**
 * 聊天服务接口
 */
public interface IChatService extends IService<ChatMessage> {

    /**
     * 获取或创建会话
     * @param currentAccountId 当前用户账号ID
     * @param targetUserId 目标用户业务ID（refId）
     * @return 会话信息
     */
    ChatConversation getOrCreateConversation(Long currentAccountId, String targetUserId);

    /**
     * 获取会话列表
     * @param accountId 当前用户账号ID
     * @param current 当前页
     * @param size 每页大小
     * @return 分页会话列表
     */
    PageResult<Map<String, Object>> getConversationList(Long accountId, long current, long size);

    /**
     * 获取聊天记录
     * @param conversationId 会话ID
     * @param accountId 当前用户账号ID
     * @param current 当前页
     * @param size 每页大小
     * @param lastMsgId 最后一条消息ID（用于加载更多历史）
     * @return 分页消息列表
     */
    PageResult<ChatMessage> getChatHistory(Long conversationId, Long accountId, long current, long size, Long lastMsgId);

    /**
     * 发送消息（HTTP通道）
     * @param conversationId 会话ID
     * @param senderId 发送者账号ID
     * @param receiverId 接收者账号ID
     * @param content 消息内容
     * @param msgType 消息类型
     * @return 消息实体
     */
    ChatMessage sendMessage(Long conversationId, Long senderId, Long receiverId, String content, String msgType);

    /**
     * 标记会话消息为已读
     * @param conversationId 会话ID
     * @param accountId 当前用户账号ID
     */
    void markConversationRead(Long conversationId, Long accountId);

    /**
     * 获取未读消息总数
     * @param accountId 当前用户账号ID
     * @return 未读数量
     */
    long getUnreadCount(Long accountId);
}
