-- ============================================================
-- MarryLink 实时沟通功能 - 数据库建表脚本
-- 新增 chat_conversation（聊天会话表）和 chat_message（聊天消息表）
-- ============================================================

-- 聊天会话表
CREATE TABLE IF NOT EXISTS `chat_conversation` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '会话ID',
  `customer_id` BIGINT NOT NULL COMMENT '新人用户ID（user表ID）',
  `host_id` BIGINT NOT NULL COMMENT '主持人ID（host表ID）',
  `customer_account_id` BIGINT NOT NULL COMMENT '新人账号ID（account_mapping表ID）',
  `host_account_id` BIGINT NOT NULL COMMENT '主持人账号ID（account_mapping表ID）',
  `last_msg_content` VARCHAR(255) DEFAULT NULL COMMENT '最后一条消息内容',
  `last_msg_time` DATETIME DEFAULT NULL COMMENT '最后一条消息时间',
  `customer_unread` INT NOT NULL DEFAULT 0 COMMENT '新人未读消息数',
  `host_unread` INT NOT NULL DEFAULT 0 COMMENT '主持人未读消息数',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-未删除, 1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_customer_host` (`customer_id`, `host_id`),
  KEY `idx_customer_account` (`customer_account_id`),
  KEY `idx_host_account` (`host_account_id`),
  KEY `idx_last_msg_time` (`last_msg_time` DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='聊天会话表';

-- 聊天消息表
CREATE TABLE IF NOT EXISTS `chat_message` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '消息ID',
  `conversation_id` BIGINT NOT NULL COMMENT '所属会话ID',
  `sender_id` BIGINT NOT NULL COMMENT '发送者账号ID（account_mapping表ID）',
  `receiver_id` BIGINT NOT NULL COMMENT '接收者账号ID（account_mapping表ID）',
  `content` TEXT NOT NULL COMMENT '消息内容（文字 或 图片URL）',
  `msg_type` VARCHAR(20) NOT NULL DEFAULT 'text' COMMENT '消息类型: text-文字, image-图片',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '消息状态: 1-未读, 2-已读',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-未删除, 1-已删除',
  PRIMARY KEY (`id`),
  KEY `idx_conversation_id` (`conversation_id`),
  KEY `idx_sender_id` (`sender_id`),
  KEY `idx_receiver_id` (`receiver_id`),
  KEY `idx_conversation_create` (`conversation_id`, `create_time` DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='聊天消息表';
