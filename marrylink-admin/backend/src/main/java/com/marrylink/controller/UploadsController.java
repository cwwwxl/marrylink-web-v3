package com.marrylink.controller;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 文件访问控制器
 * 统一处理上传文件的访问（头像、聊天图片等）
 */
@RestController
@RequestMapping("/uploads")
public class UploadsController {

    @GetMapping("/avatars/{filename}")
    public ResponseEntity<Resource> getAvatar(@PathVariable String filename) {
        // 兼容两个上传目录：
        // 1. {user.dir}/marrylink-admin/uploads/avatars/ (HostController上传路径)
        // 2. {user.dir}/uploads/avatars/ (UserController上传路径)
        String baseDir = System.getProperty("user.dir");
        String[] searchDirs = {
            baseDir + File.separator + "marrylink-admin" + File.separator + "uploads" + File.separator + "avatars",
            baseDir + File.separator + "uploads" + File.separator + "avatars"
        };

        for (String dir : searchDirs) {
            try {
                Path filePath = Paths.get(dir).resolve(filename).normalize();
                Resource resource = new UrlResource(filePath.toUri());
                if (resource.exists() && resource.isReadable()) {
                    return ResponseEntity.ok()
                            .contentType(MediaType.parseMediaType(getContentType(filename)))
                            .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                            .body(resource);
                }
            } catch (Exception ignored) {
            }
        }

        return ResponseEntity.notFound().build();
    }

    @GetMapping("/chat/{datePath1}/{datePath2}/{datePath3}/{filename}")
    public ResponseEntity<Resource> getChatImage(
            @PathVariable String datePath1,
            @PathVariable String datePath2,
            @PathVariable String datePath3,
            @PathVariable String filename) {
        String baseDir = System.getProperty("user.dir");
        String relativePath = "chat" + File.separator + datePath1 + File.separator + datePath2 + File.separator + datePath3;
        String[] searchDirs = {
            baseDir + File.separator + "marrylink-admin" + File.separator + "uploads" + File.separator + relativePath,
            baseDir + File.separator + "uploads" + File.separator + relativePath
        };

        for (String dir : searchDirs) {
            try {
                Path filePath = Paths.get(dir).resolve(filename).normalize();
                Resource resource = new UrlResource(filePath.toUri());
                if (resource.exists() && resource.isReadable()) {
                    return ResponseEntity.ok()
                            .contentType(MediaType.parseMediaType(getContentType(filename)))
                            .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                            .body(resource);
                }
            } catch (Exception ignored) {
            }
        }

        return ResponseEntity.notFound().build();
    }

    private String getContentType(String filename) {
        if (filename == null) return "application/octet-stream";
        String lower = filename.toLowerCase();
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".gif")) return "image/gif";
        if (lower.endsWith(".webp")) return "image/webp";
        return "application/octet-stream";
    }
}
