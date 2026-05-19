package com.boonya.lab.io.minio.service;

import io.minio.*;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.boonya.lab.io.minio.config.MinioProperties;
import java.io.InputStream;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class MinioService {

    private final MinioClient minioClient;
    private final MinioProperties minioProperties;

    /**
     * 上传文件并返回预签名访问URL（7天有效期）
     */
    public String uploadFile(MultipartFile file) {
        return uploadFile(file, 7, TimeUnit.DAYS);
    }

    /**
     * 上传文件并指定有效期
     * @param file 上传的文件
     * @param duration 有效期数值
     * @param unit 时间单位
     */
    public String uploadFile(MultipartFile file, int duration, TimeUnit unit) {
        String bucketName = minioProperties.getBucketName();
        String objectName = generateObjectName(file.getOriginalFilename());

        try (InputStream inputStream = file.getInputStream()) {
            // 1. 确保桶存在
            ensureBucketExists(bucketName);

            // 2. 上传文件
            uploadToMinio(bucketName, objectName, inputStream, file);

            // 3. 生成预签名访问URL
            int expirySeconds = (int) unit.toSeconds(duration);
            return generatePresignedUrl(bucketName, objectName, expirySeconds);

        } catch (Exception e) {
            log.error("文件上传失败: {}", e.getMessage(), e);
            throw new RuntimeException("文件上传失败", e);
        }
    }

    /**
     * 检查并创建桶
     */
    private void ensureBucketExists(String bucketName) throws Exception {
        boolean found = minioClient.bucketExists(BucketExistsArgs.builder()
                .bucket(bucketName)
                .build());
        if (!found) {
            minioClient.makeBucket(MakeBucketArgs.builder()
                    .bucket(bucketName)
                    .build());
            log.info("创建桶: {}", bucketName);
        }
    }

    /**
     * 上传文件到MinIO
     */
    private void uploadToMinio(String bucketName, String objectName,
                               InputStream inputStream, MultipartFile file) throws Exception {
        minioClient.putObject(PutObjectArgs.builder()
                .bucket(bucketName)
                .object(objectName)
                .stream(inputStream, file.getSize(), -1)
                .contentType(file.getContentType())
                .build());
        log.info("文件上传成功: {}", objectName);
    }

    /**
     * 生成预签名访问URL
     * @param expirySeconds 过期时间（秒），最大604800秒（7天）
     */
    private String generatePresignedUrl(String bucketName, String objectName, int expirySeconds)
            throws Exception {
        // 确保不超过7天上限
        int maxExpiry = Math.min(expirySeconds, 604800);

        return minioClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                        .method(Method.GET)
                        .bucket(bucketName)
                        .object(objectName)
                        .expiry(maxExpiry)
                        .build()
        );
    }

    /**
     * 生成唯一对象名（UUID + 原始文件名）
     */
    private String generateObjectName(String originalFilename) {
        String uuid = UUID.randomUUID().toString();
        return uuid + "-" + originalFilename;
    }

    /**
     * 删除文件
     */
    public void deleteFile(String objectName) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(minioProperties.getBucketName())
                    .object(objectName)
                    .build());
            log.info("文件删除成功: {}", objectName);
        } catch (Exception e) {
            log.error("文件删除失败: {}", e.getMessage(), e);
            throw new RuntimeException("文件删除失败", e);
        }
    }

    /**
     * 检查文件是否存在
     */
    public boolean fileExists(String objectName) {
        try {
            minioClient.statObject(StatObjectArgs.builder()
                    .bucket(minioProperties.getBucketName())
                    .object(objectName)
                    .build());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 按需生成临时访问URL（推荐生产环境使用）
     */
    public String getTemporaryAccessUrl(String objectName, int expirySeconds) {
        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(minioProperties.getBucketName())
                            .object(objectName)
                            .expiry(expirySeconds)  // 建议300-3600秒
                            .build()
            );
        } catch (Exception e) {
            throw new RuntimeException("生成访问URL失败", e);
        }
    }
}
