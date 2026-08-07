package com.boonya.lab.io.minio.controller;

import com.boonya.lab.io.common.response.Result;
import com.boonya.lab.io.minio.service.MinioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
@Tag(name = "文件管理", description = "文件上传、删除、访问等接口")
public class FileController {

    private final MinioService minioService;

    @PostMapping("/upload")
    @Operation(summary = "上传文件", description = "上传文件到MinIO，返回预签名访问URL（7天有效期）")
    public Result<String> uploadFile(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return Result.error("请选择要上传的文件");
        }
        String fileUrl = minioService.uploadFile(file);
        return Result.success(fileUrl);
    }

    @DeleteMapping
    @Operation(summary = "删除文件", description = "根据对象名删除MinIO中的文件，对象名通过参数传递以支持含/的多级路径")
    public Result<Void> deleteFile(@RequestParam String objectName) {
        minioService.deleteFile(objectName);
        return Result.success();
    }

    @GetMapping("/check")
    @Operation(summary = "检查文件是否存在", description = "检查指定对象名的文件是否存在于MinIO中")
    public Result<Boolean> checkFileExists(@RequestParam String objectName) {
        return Result.success(minioService.fileExists(objectName));
    }

    @GetMapping("/url")
    @Operation(summary = "获取临时访问URL", description = "生成文件的临时预签名访问URL（默认1小时有效期）")
    public Result<String> getTemporaryAccessUrl(@RequestParam String objectName) {
        int expirySeconds = 3600; // 1小时有效期
        String url = minioService.getTemporaryAccessUrl(objectName, expirySeconds);
        return Result.success(url);
    }
}
