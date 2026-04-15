package com.marrylink.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.marrylink.common.PageResult;
import com.marrylink.common.Result;
import com.marrylink.dto.UserInfoResponse;
import com.marrylink.entity.User;
import com.marrylink.security.CustomUserDetails;
import com.marrylink.service.IUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 用户控制器
 */
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final IUserService userService;

    /**
     * 获取当前登录用户信息（Web端通用）
     */
    @GetMapping("/me")
    public Result<Map<String, Object>> getMe() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Object principal = authentication.getPrincipal();

        if (principal instanceof CustomUserDetails) {
            CustomUserDetails userDetails = (CustomUserDetails) principal;

            Map<String, Object> map = new HashMap<>();
            map.put("userId", userDetails.getAccountId());
            map.put("accountId", userDetails.getAccountId());
            map.put("refId", userDetails.getRefId());
            map.put("userType", userDetails.getUserType());
            map.put("username", userDetails.getUsername());
            map.put("realName", userDetails.getRealName());
            map.put("nickname", userDetails.getRealName());
            map.put("phone", userDetails.getPhone());
            map.put("email", userDetails.getEmail());
            map.put("roles", userDetails.getRoles());
            map.put("perms", userDetails.getPermissions());
            map.put("avatar", "https://wpimg.wallstcn.com/f778738c-e4f8-4870-b634-56703b4acafe.gif"); // 默认头像

            return Result.ok(map);
        }

        return Result.error("获取用户信息失败");
    }

    /**
     * 获取当前用户信息（小程序端使用）
     */
    @GetMapping("/info")
    @PreAuthorize("hasRole('CUSTOMER')")
    public Result<UserInfoResponse> getCurrentUserInfo() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Long refId = userDetails.getRefId();

        User user = userService.getById(refId);
        if (user == null) {
            return Result.error("用户不存在");
        }

        UserInfoResponse response = new UserInfoResponse();
        BeanUtils.copyProperties(user, response);

        return Result.ok(response);
    }

    /**
     * 更新用户信息（小程序端使用）
     */
    @PostMapping("/update")
    @PreAuthorize("hasRole('CUSTOMER')")
    public Result<Void> updateUserInfo(@RequestBody User user) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Long refId = userDetails.getRefId();

        // 只允许更新当前用户的信息
        user.setId(refId);
        userService.updateById(user);

        return Result.ok();
    }

    /**
     * 上传用户头像（小程序端使用）
     */
    @PostMapping("/uploadAvatar")
    @PreAuthorize("hasRole('CUSTOMER')")
    public Result<String> uploadAvatar(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return Result.error("文件不能为空");
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Long refId = userDetails.getRefId();

        try {
            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            String filename = UUID.randomUUID().toString() + extension;

            String uploadDir = System.getProperty("user.dir") + File.separator + "marrylink-admin" + File.separator + "uploads" + File.separator + "avatars";
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            Path filePath = uploadPath.resolve(filename);
            file.transferTo(filePath.toFile());

            String avatarUrl = "/uploads/avatars/" + filename;

            // 更新数据库中的头像字段
            User user = new User();
            user.setId(refId);
            user.setAvatar(avatarUrl);
            userService.updateById(user);

            return Result.ok(avatarUrl);
        } catch (IOException e) {
            return Result.error("文件上传失败");
        }
    }

    /**
     * 分页查询用户列表（管理端使用）
     */
    @GetMapping("/page")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<PageResult<User>> page(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String keyword) {

        Page<User> page = new Page<>(current, size);
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();

        if (status != null) {
            wrapper.eq(User::getStatus, status);
        }
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(User::getBrideName, keyword)
                    .or().like(User::getGroomName, keyword)
                    .or().like(User::getPhone, keyword));
        }

        wrapper.orderByDesc(User::getCreateTime);
        userService.page(page, wrapper);

        return Result.ok(PageResult.of(page));
    }

    /**
     * 根据ID查询用户（管理端使用）
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<User> getById(@PathVariable Long id) {
        return Result.ok(userService.getById(id));
    }

    /**
     * 更新用户状态（管理端使用）
     */
    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        User user = new User();
        user.setId(id);
        user.setStatus(status);
        userService.updateById(user);
        return Result.ok();
    }
}
