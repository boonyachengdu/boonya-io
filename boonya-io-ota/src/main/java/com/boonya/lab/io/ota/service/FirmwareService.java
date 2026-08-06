package com.boonya.lab.io.ota.service;

import com.boonya.lab.io.common.exception.BusinessException;
import com.boonya.lab.io.ota.config.MinioConfig;
import com.boonya.lab.io.ota.dto.FirmwareUploadRequest;
import com.boonya.lab.io.ota.entity.Firmware;
import com.boonya.lab.io.ota.entity.OtaTask;
import com.boonya.lab.io.ota.repository.FirmwareRepository;
import com.boonya.lab.io.ota.repository.OtaTaskRepository;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FirmwareService {

    private final FirmwareRepository firmwareRepository;
    private final OtaTaskRepository otaTaskRepository;
    private final MinioClient minioClient;
    private final MinioConfig minioConfig;

    /**
     * 上传固件
     */
    @Transactional
    public Firmware uploadFirmware(FirmwareUploadRequest request) {
        // 检查版本是否已存在
        if (firmwareRepository.findByDeviceModelAndVersion(
                request.getDeviceModel(), request.getVersion()).isPresent()) {
            throw new BusinessException(
                    "FIRMWARE_EXISTS",
                    "设备型号 " + request.getDeviceModel() + " 的版本 " + request.getVersion() + " 已存在",
                    HttpStatus.CONFLICT
            );
        }

        try {
            MultipartFile file = request.getFile();
            String fileName = generateFileName(request.getDeviceModel(), request.getVersion(), file.getOriginalFilename());

            // 计算 MD5
            String md5 = calculateMd5(file.getInputStream());

            // 上传到 MinIO
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .object(fileName)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType("application/octet-stream")
                            .build()
            );

            // 保存固件信息
            Firmware firmware = new Firmware();
            firmware.setDeviceModel(request.getDeviceModel());
            firmware.setVersion(request.getVersion());
            firmware.setDescription(request.getDescription());
            firmware.setFilePath(fileName);
            firmware.setFileName(file.getOriginalFilename());
            firmware.setFileSize(file.getSize());
            firmware.setMd5Checksum(md5);
            firmware.setForceUpdate(request.getForceUpdate() != null ? request.getForceUpdate() : false);
            firmware.setStatus("draft");

            return firmwareRepository.save(firmware);

        } catch (Exception e) {
            log.error("上传固件失败", e);
            throw new BusinessException(
                    "UPLOAD_FAILED",
                    "上传固件失败: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    /**
     * 发布固件
     */
    @Transactional
    public Firmware publishFirmware(Long id) {
        Firmware firmware = firmwareRepository.findById(id)
                .orElseThrow(() -> new BusinessException(
                        "FIRMWARE_NOT_FOUND",
                        "固件不存在",
                        HttpStatus.NOT_FOUND
                ));

        if ("published".equals(firmware.getStatus())) {
            throw new BusinessException(
                    "ALREADY_PUBLISHED",
                    "固件已发布",
                    HttpStatus.BAD_REQUEST
            );
        }

        firmware.setStatus("published");
        firmware.setPublishTime(LocalDateTime.now());
        return firmwareRepository.save(firmware);
    }

    /**
     * 归档固件
     */
    @Transactional
    public Firmware archiveFirmware(Long id) {
        Firmware firmware = firmwareRepository.findById(id)
                .orElseThrow(() -> new BusinessException(
                        "FIRMWARE_NOT_FOUND",
                        "固件不存在",
                        HttpStatus.NOT_FOUND
                ));

        firmware.setStatus("archived");
        return firmwareRepository.save(firmware);
    }

    /**
     * 获取固件列表（分页）
     */
    public Page<Firmware> listFirmwares(int pageNum, int pageSize, String deviceModel, String status) {
        if (pageSize > 100) {
            pageSize = 100;
        }
        if (pageNum < 1) {
            pageNum = 1;
        }
        Pageable pageable = PageRequest.of(pageNum - 1, pageSize, Sort.by(Sort.Direction.DESC, "createTime"));

        Specification<Firmware> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (StringUtils.hasText(deviceModel)) {
                predicates.add(cb.like(root.get("deviceModel"), "%" + deviceModel + "%"));
            }
            if (StringUtils.hasText(status)) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return firmwareRepository.findAll(spec, pageable);
    }

    /**
     * 获取固件列表（不分页，保留兼容）
     */
    public List<Firmware> listFirmwares(String deviceModel, String status) {
        if (deviceModel != null && status != null) {
            return firmwareRepository.findByDeviceModelAndStatusOrderByCreateTimeDesc(deviceModel, status);
        } else if (deviceModel != null) {
            return firmwareRepository.findByDeviceModelAndStatusOrderByCreateTimeDesc(deviceModel, "published");
        } else {
            return firmwareRepository.findAllByOrderByCreateTimeDesc();
        }
    }

    /**
     * 获取固件详情
     */
    public Firmware getFirmware(Long id) {
        return firmwareRepository.findById(id)
                .orElseThrow(() -> new BusinessException(
                        "FIRMWARE_NOT_FOUND",
                        "固件不存在",
                        HttpStatus.NOT_FOUND
                ));
    }

    /**
     * 删除固件
     */
    @Transactional
    public void deleteFirmware(Long id) {
        Firmware firmware = firmwareRepository.findById(id)
                .orElseThrow(() -> new BusinessException(
                        "FIRMWARE_NOT_FOUND",
                        "固件不存在",
                        HttpStatus.NOT_FOUND
                ));

        if ("published".equals(firmware.getStatus())) {
            throw new BusinessException(
                    "CANNOT_DELETE_PUBLISHED",
                    "不能删除已发布的固件",
                    HttpStatus.BAD_REQUEST
            );
        }

        // 从 MinIO 删除文件
        try {
            minioClient.removeObject(
                    io.minio.RemoveObjectArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .object(firmware.getFilePath())
                            .build()
            );
        } catch (Exception e) {
            log.warn("删除 MinIO 文件失败", e);
        }

        firmwareRepository.delete(firmware);
    }

    /**
     * 生成文件名
     */
    private String generateFileName(String deviceModel, String version, String originalFilename) {
        String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        return String.format("%s/%s/%s%s", deviceModel, version, System.currentTimeMillis(), extension);
    }

    /**
     * 计算 MD5
     */
    private String calculateMd5(InputStream inputStream) throws Exception {
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] buffer = new byte[8192];
        int bytesRead;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            md.update(buffer, 0, bytesRead);
        }
        byte[] digest = md.digest();
        StringBuilder sb = new StringBuilder();
        for (byte b : digest) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
