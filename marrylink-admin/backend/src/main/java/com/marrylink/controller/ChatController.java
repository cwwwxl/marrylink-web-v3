package com.marrylink.controller;

import com.marrylink.common.PageResult;
import com.marrylink.common.Result;
import com.marrylink.entity.ChatConversation;
import com.marrylink.entity.ChatMessage;
import com.marrylink.service.IChatService;
import com.marrylink.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 聊天控制器
 * 处理新人端与主持人端的实时沟通相关接口
 */
@Slf4j
@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
public class ChatController {

    @Resource
    private IChatService chatService;

    /**
     * 获取会话列表
     */
    @GetMapping("/conversations")
    public Result<PageResult<Map<String, Object>>> getConversations(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "20") Long size) {
        Long accountId = SecurityUtils.getCurrentAccountId();
        PageResult<Map<String, Object>> result = chatService.getConversationList(accountId, current, size);
        return Result.ok(result);
    }

    /**
     * 获取或创建与目标用户的会话
     */
    @PostMapping("/conversation")
    public Result<ChatConversation> getOrCreateConversation(@RequestBody Map<String, String> body) {
        Long accountId = SecurityUtils.getCurrentAccountId();
        String targetUserId = body.get("targetUserId");
        if (targetUserId == null || targetUserId.isEmpty()) {
            return Result.error("目标用户ID不能为空");
        }
        ChatConversation conversation = chatService.getOrCreateConversation(accountId, targetUserId);
        return Result.ok(conversation);
    }

    /**
     * 获取聊天记录
     */
    @GetMapping("/history/{conversationId}")
    public Result<PageResult<ChatMessage>> getChatHistory(
            @PathVariable Long conversationId,
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "30") Long size,
            @RequestParam(required = false) Long lastMsgId) {
        Long accountId = SecurityUtils.getCurrentAccountId();
        PageResult<ChatMessage> result = chatService.getChatHistory(conversationId, accountId, current, size, lastMsgId);
        return Result.ok(result);
    }

    /**
     * 发送消息（HTTP备用通道）
     */
    @PostMapping("/send")
    public Result<ChatMessage> sendMessage(@RequestBody Map<String, Object> body) {
        Long accountId = SecurityUtils.getCurrentAccountId();

        Long conversationId = Long.valueOf(body.get("conversationId").toString());
        Long receiverId = Long.valueOf(body.get("receiverId").toString());
        String content = (String) body.get("content");
        String msgType = body.getOrDefault("msgType", "text").toString();

        ChatMessage message = chatService.sendMessage(conversationId, accountId, receiverId, content, msgType);
        return Result.ok(message);
    }

    /**
     * 标记会话消息已读
     */
    @PostMapping("/read/{conversationId}")
    public Result<Void> markRead(@PathVariable Long conversationId) {
        Long accountId = SecurityUtils.getCurrentAccountId();
        chatService.markConversationRead(conversationId, accountId);
        return Result.ok();
    }

    /**
     * 获取聊天未读消息总数
     */
    @GetMapping("/unread/count")
    public Result<Map<String, Object>> getUnreadCount() {
        Long accountId = SecurityUtils.getCurrentAccountId();
        long count = chatService.getUnreadCount(accountId);
        Map<String, Object> result = new HashMap<>();
        result.put("count", count);
        return Result.ok(result);
    }

    /**
     * 上传聊天图片
     */
    @PostMapping("/upload")
    public Result<Map<String, Object>> uploadImage(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return Result.error("文件不能为空");
        }

        // 限制文件大小（10MB）
        if (file.getSize() > 10 * 1024 * 1024) {
            return Result.error("文件大小不能超过10MB");
        }

        // 限制文件类型
        String originalFilename = file.getOriginalFilename();
        if (originalFilename != null) {
            String ext = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
            if (!ext.matches("\\.(jpg|jpeg|png|gif|webp)")) {
                return Result.error("仅支持 jpg、png、gif、webp 格式的图片");
            }
        }

        try {
            // 生成存储路径
            String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
            String fileName = UUID.randomUUID().toString().replace("-", "") +
                    (originalFilename != null ? originalFilename.substring(originalFilename.lastIndexOf(".")) : ".jpg");

            String uploadDir = System.getProperty("user.dir") + File.separator +
                    "marrylink-admin" + File.separator + "uploads" + File.separator +
                    "chat" + File.separator + datePath;

            File dir = new File(uploadDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            File dest = new File(dir, fileName);
            file.transferTo(dest);

            // 返回访问URL（相对路径）
            String url = "/uploads/chat/" + datePath + "/" + fileName;

            Map<String, Object> result = new HashMap<>();
            result.put("url", url);
            return Result.ok(result);

        } catch (IOException e) {
            log.error("文件上传失败:", e);
            return Result.error("文件上传失败");
        }
    }
}
