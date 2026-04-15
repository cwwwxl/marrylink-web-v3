package com.marrylink.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.marrylink.common.PageResult;
import com.marrylink.entity.*;
import com.marrylink.mapper.ChatConversationMapper;
import com.marrylink.mapper.ChatMessageMapper;
import com.marrylink.service.IChatService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 聊天服务实现
 */
@Slf4j
@Service
public class ChatServiceImpl extends ServiceImpl<ChatMessageMapper, ChatMessage> implements IChatService {

    @Resource
    private ChatConversationMapper conversationMapper;

    @Resource
    private ChatMessageMapper chatMessageMapper;

    @Resource
    private com.marrylink.mapper.AccountMappingMapper accountMappingMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChatConversation getOrCreateConversation(Long currentAccountId, String targetUserId) {
        // 查询当前用户信息
        AccountMapping currentUser = accountMappingMapper.selectById(currentAccountId);
        if (currentUser == null) {
            throw new RuntimeException("当前用户不存在");
        }

        // 根据目标用户的业务ID（refId）查找其账号
        Long targetRefId = Long.valueOf(targetUserId);
        String targetUserType;
        if ("CUSTOMER".equals(currentUser.getUserType())) {
            targetUserType = "HOST";
        } else {
            targetUserType = "CUSTOMER";
        }

        LambdaQueryWrapper<AccountMapping> targetWrapper = new LambdaQueryWrapper<>();
        targetWrapper.eq(AccountMapping::getRefId, targetRefId)
                     .eq(AccountMapping::getUserType, targetUserType);
        AccountMapping targetUser = accountMappingMapper.selectOne(targetWrapper);
        if (targetUser == null) {
            throw new RuntimeException("目标用户不存在");
        }

        // 确定 customerId 和 hostId
        Long customerId, hostId, customerAccountId, hostAccountId;
        if ("CUSTOMER".equals(currentUser.getUserType())) {
            customerId = currentUser.getRefId();
            customerAccountId = currentUser.getId();
            hostId = targetRefId;
            hostAccountId = targetUser.getId();
        } else {
            customerId = targetRefId;
            customerAccountId = targetUser.getId();
            hostId = currentUser.getRefId();
            hostAccountId = currentUser.getId();
        }

        // 查找已有会话
        LambdaQueryWrapper<ChatConversation> convWrapper = new LambdaQueryWrapper<>();
        convWrapper.eq(ChatConversation::getCustomerId, customerId)
                   .eq(ChatConversation::getHostId, hostId);
        ChatConversation conversation = conversationMapper.selectOne(convWrapper);

        if (conversation != null) {
            return conversation;
        }

        // 创建新会话
        conversation = new ChatConversation();
        conversation.setCustomerId(customerId);
        conversation.setHostId(hostId);
        conversation.setCustomerAccountId(customerAccountId);
        conversation.setHostAccountId(hostAccountId);
        conversation.setCustomerUnread(0);
        conversation.setHostUnread(0);
        conversationMapper.insert(conversation);

        return conversation;
    }

    @Override
    public PageResult<Map<String, Object>> getConversationList(Long accountId, long current, long size) {
        // 查询当前用户信息
        AccountMapping currentUser = accountMappingMapper.selectById(accountId);
        if (currentUser == null) {
            return emptyPageResult(current, size);
        }

        boolean isCustomer = "CUSTOMER".equals(currentUser.getUserType());

        // 查询与当前用户相关的会话
        LambdaQueryWrapper<ChatConversation> wrapper = new LambdaQueryWrapper<>();
        if (isCustomer) {
            wrapper.eq(ChatConversation::getCustomerAccountId, accountId);
        } else {
            wrapper.eq(ChatConversation::getHostAccountId, accountId);
        }
        wrapper.orderByDesc(ChatConversation::getLastMsgTime);

        Page<ChatConversation> page = new Page<>(current, size);
        conversationMapper.selectPage(page, wrapper);

        // 构建返回数据，附加对方用户信息
        List<Map<String, Object>> records = new ArrayList<>();
        for (ChatConversation conv : page.getRecords()) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", conv.getId());
            map.put("lastMsgContent", conv.getLastMsgContent());
            map.put("lastMsgTime", conv.getLastMsgTime());

            if (isCustomer) {
                map.put("unreadCount", conv.getCustomerUnread());
                map.put("targetUserId", conv.getHostId());
                map.put("targetUserType", "HOST");
                // 查询主持人信息
                fillTargetUserInfo(map, conv.getHostAccountId());
            } else {
                map.put("unreadCount", conv.getHostUnread());
                map.put("targetUserId", conv.getCustomerId());
                map.put("targetUserType", "CUSTOMER");
                // 查询新人信息
                fillTargetUserInfo(map, conv.getCustomerAccountId());
            }

            records.add(map);
        }

        PageResult<Map<String, Object>> result = new PageResult<>();
        result.setRecords(records);
        result.setTotal(page.getTotal());
        result.setCurrent(page.getCurrent());
        result.setSize(page.getSize());
        return result;
    }

    /**
     * 填充对方用户信息
     */
    private void fillTargetUserInfo(Map<String, Object> map, Long targetAccountId) {
        AccountMapping targetAccount = accountMappingMapper.selectById(targetAccountId);
        if (targetAccount != null) {
            // 根据 userType 和 refId 查询具体的业务表获取姓名和头像
            if ("HOST".equals(targetAccount.getUserType())) {
                // 查询 host 表
                map.put("targetName", targetAccount.getAccountId()); // 先用账号，后续可替换
                map.put("targetAvatar", "");
                try {
                    com.marrylink.entity.Host host = getHostById(targetAccount.getRefId());
                    if (host != null) {
                        map.put("targetName", host.getName());
                        map.put("targetAvatar", host.getAvatar() != null ? host.getAvatar() : "");
                    }
                } catch (Exception e) {
                    log.warn("查询主持人信息失败: {}", e.getMessage());
                }
            } else {
                map.put("targetName", targetAccount.getAccountId());
                map.put("targetAvatar", "");
                try {
                    com.marrylink.entity.User user = getUserById(targetAccount.getRefId());
                    if (user != null) {
                        // User 表使用 brideName + groomName
                        String name = "";
                        if (user.getBrideName() != null && user.getGroomName() != null) {
                            name = user.getBrideName() + " & " + user.getGroomName();
                        } else if (user.getBrideName() != null) {
                            name = user.getBrideName();
                        } else if (user.getGroomName() != null) {
                            name = user.getGroomName();
                        }
                        map.put("targetName", name.isEmpty() ? targetAccount.getAccountId() : name);
                        map.put("targetAvatar", user.getAvatar() != null ? user.getAvatar() : "");
                    }
                } catch (Exception e) {
                    log.warn("查询用户信息失败: {}", e.getMessage());
                }
            }
        } else {
            map.put("targetName", "未知用户");
            map.put("targetAvatar", "");
        }
    }

    @Resource
    private com.marrylink.mapper.HostMapper hostMapper;
    @Resource
    private com.marrylink.mapper.UserMapper userMapper;

    private Host getHostById(Long id) {
        return hostMapper.selectById(id);
    }

    private User getUserById(Long id) {
        return userMapper.selectById(id);
    }

    @Override
    public PageResult<ChatMessage> getChatHistory(Long conversationId, Long accountId, long current, long size, Long lastMsgId) {
        // 验证用户有权访问该会话
        ChatConversation conv = conversationMapper.selectById(conversationId);
        if (conv == null) {
            return emptyMsgPageResult(current, size);
        }
        if (!accountId.equals(conv.getCustomerAccountId()) && !accountId.equals(conv.getHostAccountId())) {
            throw new RuntimeException("无权访问该会话");
        }

        LambdaQueryWrapper<ChatMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatMessage::getConversationId, conversationId);

        // 如果有 lastMsgId，则加载比该ID更早的消息
        if (lastMsgId != null) {
            wrapper.lt(ChatMessage::getId, lastMsgId);
        }

        // 按时间倒序查询（前端会反转）
        wrapper.orderByDesc(ChatMessage::getId);

        Page<ChatMessage> page = new Page<>(current, size);
        chatMessageMapper.selectPage(page, wrapper);

        return PageResult.of(page);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChatMessage sendMessage(Long conversationId, Long senderId, Long receiverId, String content, String msgType) {
        // 验证会话存在
        ChatConversation conv = conversationMapper.selectById(conversationId);
        if (conv == null) {
            throw new RuntimeException("会话不存在");
        }

        // 创建消息
        ChatMessage message = new ChatMessage();
        message.setConversationId(conversationId);
        message.setSenderId(senderId);
        message.setReceiverId(receiverId);
        message.setContent(content);
        message.setMsgType(msgType != null ? msgType : "text");
        message.setStatus(1); // 未读

        chatMessageMapper.insert(message);

        // 更新会话的最后消息和未读数
        String lastContent = "image".equals(msgType) ? "[图片]" : content;
        if (lastContent != null && lastContent.length() > 100) {
            lastContent = lastContent.substring(0, 100) + "...";
        }

        ChatConversation update = new ChatConversation();
        update.setId(conv.getId());
        update.setLastMsgContent(lastContent);
        update.setLastMsgTime(LocalDateTime.now());

        // 增加对方的未读数
        if (senderId.equals(conv.getCustomerAccountId())) {
            update.setHostUnread(conv.getHostUnread() != null ? conv.getHostUnread() + 1 : 1);
        } else {
            update.setCustomerUnread(conv.getCustomerUnread() != null ? conv.getCustomerUnread() + 1 : 1);
        }

        conversationMapper.updateById(update);

        return message;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markConversationRead(Long conversationId, Long accountId) {
        ChatConversation conv = conversationMapper.selectById(conversationId);
        if (conv == null) return;

        // 清零当前用户的未读数
        ChatConversation update = new ChatConversation();
        update.setId(conversationId);

        if (accountId.equals(conv.getCustomerAccountId())) {
            update.setCustomerUnread(0);
        } else if (accountId.equals(conv.getHostAccountId())) {
            update.setHostUnread(0);
        }

        conversationMapper.updateById(update);

        // 将该会话中发给当前用户的消息标记为已读
        LambdaUpdateWrapper<ChatMessage> msgUpdate = new LambdaUpdateWrapper<>();
        msgUpdate.eq(ChatMessage::getConversationId, conversationId)
                 .eq(ChatMessage::getReceiverId, accountId)
                 .eq(ChatMessage::getStatus, 1)
                 .set(ChatMessage::getStatus, 2);
        chatMessageMapper.update(null, msgUpdate);
    }

    @Override
    public long getUnreadCount(Long accountId) {
        AccountMapping currentUser = accountMappingMapper.selectById(accountId);
        if (currentUser == null) return 0;

        boolean isCustomer = "CUSTOMER".equals(currentUser.getUserType());

        LambdaQueryWrapper<ChatConversation> wrapper = new LambdaQueryWrapper<>();
        if (isCustomer) {
            wrapper.eq(ChatConversation::getCustomerAccountId, accountId)
                   .gt(ChatConversation::getCustomerUnread, 0);
        } else {
            wrapper.eq(ChatConversation::getHostAccountId, accountId)
                   .gt(ChatConversation::getHostUnread, 0);
        }

        List<ChatConversation> conversations = conversationMapper.selectList(wrapper);
        long total = 0;
        for (ChatConversation conv : conversations) {
            if (isCustomer) {
                total += conv.getCustomerUnread() != null ? conv.getCustomerUnread() : 0;
            } else {
                total += conv.getHostUnread() != null ? conv.getHostUnread() : 0;
            }
        }
        return total;
    }

    private PageResult<Map<String, Object>> emptyPageResult(long current, long size) {
        PageResult<Map<String, Object>> result = new PageResult<>();
        result.setRecords(new ArrayList<>());
        result.setTotal(0);
        result.setCurrent(current);
        result.setSize(size);
        return result;
    }

    private PageResult<ChatMessage> emptyMsgPageResult(long current, long size) {
        PageResult<ChatMessage> result = new PageResult<>();
        result.setRecords(new ArrayList<>());
        result.setTotal(0);
        result.setCurrent(current);
        result.setSize(size);
        return result;
    }
}
