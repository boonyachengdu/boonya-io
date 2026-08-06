package com.boonya.lab.io.ota.service;

import com.boonya.lab.io.common.exception.BusinessException;
import com.boonya.lab.io.ota.entity.Firmware;
import com.boonya.lab.io.ota.entity.OtaTask;
import com.boonya.lab.io.ota.repository.FirmwareRepository;
import com.boonya.lab.io.ota.repository.OtaTaskRepository;
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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OtaTaskService {

    private final OtaTaskRepository otaTaskRepository;
    private final FirmwareRepository firmwareRepository;

    /**
     * 创建 OTA 任务
     */
    @Transactional
    public OtaTask createOtaTask(String deviceId, Long firmwareId) {
        // 检查固件是否存在
        Firmware firmware = firmwareRepository.findById(firmwareId)
                .orElseThrow(() -> new BusinessException(
                        "FIRMWARE_NOT_FOUND",
                        "固件不存在",
                        HttpStatus.NOT_FOUND
                ));

        if (!"published".equals(firmware.getStatus())) {
            throw new BusinessException(
                    "FIRMWARE_NOT_PUBLISHED",
                    "固件未发布，不能用于升级",
                    HttpStatus.BAD_REQUEST
            );
        }

        // 检查设备是否有进行中的任务
        otaTaskRepository.findFirstByDeviceIdAndStatusIn(
                deviceId, Arrays.asList("pending", "downloading", "installing")
        ).ifPresent(task -> {
            throw new BusinessException(
                    "TASK_IN_PROGRESS",
                    "设备已有进行中的升级任务",
                    HttpStatus.CONFLICT
            );
        });

        // 创建新任务
        OtaTask task = new OtaTask();
        task.setDeviceId(deviceId);
        task.setFirmwareId(firmwareId);
        task.setStatus("pending");
        task.setProgress(0);

        return otaTaskRepository.save(task);
    }

    /**
     * 更新任务状态
     */
    @Transactional
    public OtaTask updateTaskStatus(Long taskId, String status, Integer progress, String errorMessage) {
        OtaTask task = otaTaskRepository.findById(taskId)
                .orElseThrow(() -> new BusinessException(
                        "TASK_NOT_FOUND",
                        "任务不存在",
                        HttpStatus.NOT_FOUND
                ));

        task.setStatus(status);
        if (progress != null) {
            task.setProgress(progress);
        }
        if (errorMessage != null) {
            task.setErrorMessage(errorMessage);
        }

        if ("downloading".equals(status) && task.getStartTime() == null) {
            task.setStartTime(LocalDateTime.now());
        }

        if ("success".equals(status) || "failed".equals(status) || "cancelled".equals(status)) {
            task.setCompleteTime(LocalDateTime.now());
        }

        return otaTaskRepository.save(task);
    }

    /**
     * 取消任务
     */
    @Transactional
    public OtaTask cancelTask(Long taskId) {
        return updateTaskStatus(taskId, "cancelled", null, null);
    }

    /**
     * 获取设备的任务列表
     */
    public List<OtaTask> getDeviceTasks(String deviceId) {
        return otaTaskRepository.findByDeviceIdOrderByCreateTimeDesc(deviceId);
    }

    /**
     * 获取任务详情
     */
    public OtaTask getTask(Long taskId) {
        return otaTaskRepository.findById(taskId)
                .orElseThrow(() -> new BusinessException(
                        "TASK_NOT_FOUND",
                        "任务不存在",
                        HttpStatus.NOT_FOUND
                ));
    }

    /**
     * 获取固件相关的任务列表
     */
    public List<OtaTask> getFirmwareTasks(Long firmwareId) {
        return otaTaskRepository.findByFirmwareId(firmwareId);
    }

    /**
     * 全局分页查询OTA任务
     * @param pageNum 页码（从1开始）
     * @param pageSize 每页数量
     * @param deviceId 设备ID（可选筛选）
     * @param status 任务状态（可选筛选）
     */
    public Page<OtaTask> queryTasks(int pageNum, int pageSize, String deviceId, String status) {
        // 限制每页最大100条，防止一次拉爆
        if (pageSize > 100) {
            pageSize = 100;
        }
        if (pageNum < 1) {
            pageNum = 1;
        }
        Pageable pageable = PageRequest.of(pageNum - 1, pageSize, Sort.by(Sort.Direction.DESC, "createTime"));

        Specification<OtaTask> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (StringUtils.hasText(deviceId)) {
                predicates.add(cb.like(root.get("deviceId"), "%" + deviceId + "%"));
            }
            if (StringUtils.hasText(status)) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return otaTaskRepository.findAll(spec, pageable);
    }
}
