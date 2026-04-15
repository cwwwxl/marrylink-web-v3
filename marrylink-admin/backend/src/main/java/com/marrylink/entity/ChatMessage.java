package com.marrylink.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 聊天消息实体
 */
@Data
@TableName("chat_message")
public class ChatMessage {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 所属会话ID
     */
    private Long conversationId;

    /**
     * 发送者账号ID（account_mapping表ID）
     */
    private Long senderId;

    /**
     * 接收者账号ID（account_mapping表ID）
     */
    private Long receiverId;

    /**
     * 消息内容（文字内容 或 图片URL）
     */
    private String content;

    /**
     * 消息类型: text-文字, image-图片
     */
    private String msgType;

    /**
     * 消息状态: 1-未读, 2-已读
     */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer isDeleted;
}
