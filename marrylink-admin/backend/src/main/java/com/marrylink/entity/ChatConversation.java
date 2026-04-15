package com.marrylink.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 聊天会话实体
 * 记录新人与主持人之间的会话关系
 */
@Data
@TableName("chat_conversation")
public class ChatConversation {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 新人用户ID（user表）
     */
    private Long customerId;

    /**
     * 主持人ID（host表）
     */
    private Long hostId;

    /**
     * 新人账号ID（account_mapping表）
     */
    private Long customerAccountId;

    /**
     * 主持人账号ID（account_mapping表）
     */
    private Long hostAccountId;

    /**
     * 最后一条消息内容
     */
    private String lastMsgContent;

    /**
     * 最后一条消息时间
     */
    private LocalDateTime lastMsgTime;

    /**
     * 新人未读消息数
     */
    private Integer customerUnread;

    /**
     * 主持人未读消息数
     */
    private Integer hostUnread;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer isDeleted;
}
