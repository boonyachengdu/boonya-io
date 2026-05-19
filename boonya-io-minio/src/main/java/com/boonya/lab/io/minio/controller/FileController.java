package com.boonya.lab.io.minio.controller;

import com.boonya.lab.io.minio.service.MinioService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final MinioService minioService;

    @PostMapping("/upload")
    public ResponseEntity<String> uploadFile(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("请选择要上传的文件");
        }
        String fileUrl = minioService.uploadFile(file);
        return ResponseEntity.ok(fileUrl);
    }

    @DeleteMapping("/{objectName}")
    public ResponseEntity<String> deleteFile(@PathVariable String objectName) {
        minioService.deleteFile(objectName);
        return ResponseEntity.ok("文件删除成功");
    }

    @GetMapping("/check/{objectName}")
    public ResponseEntity<Boolean> checkFileExists(@PathVariable String objectName) {
        return ResponseEntity.ok(minioService.fileExists(objectName));
    }

    @GetMapping("/{objectName}")
    public ResponseEntity<String> getTemporaryAccessUrl(@PathVariable String objectName) {
        int expirySeconds = 3600; // 1小时有效期
        String url = minioService.getTemporaryAccessUrl(objectName, expirySeconds);
        return ResponseEntity.ok(url);
    }
}
