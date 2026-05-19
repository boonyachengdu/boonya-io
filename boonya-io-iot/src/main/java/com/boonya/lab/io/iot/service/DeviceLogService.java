package com.boonya.lab.io.iot.service;

import com.boonya.lab.io.iot.model.DeviceLog;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@Service
public class DeviceLogService {

    // 模拟内存存储，实际项目应使用数据库
    private final ConcurrentHashMap<String, CopyOnWriteArrayList<DeviceLog>> deviceLogs = new ConcurrentHashMap<>();

    public void save(String deviceId, String fileUrl, String fileName) {
        DeviceLog deviceLog = new DeviceLog();
        deviceLog.setDeviceId(deviceId);
        deviceLog.setFileUrl(fileUrl);
        deviceLog.setFileName(fileName);
        deviceLog.setUploadTime(LocalDateTime.now());

        deviceLogs.computeIfAbsent(deviceId, k -> new CopyOnWriteArrayList<>()).add(deviceLog);
        log.info("Log saved for device {}: {}", deviceId, fileName);
    }

    public List<DeviceLog> getLogsByDevice(String deviceId) {
        return deviceLogs.getOrDefault(deviceId, new CopyOnWriteArrayList<>());
    }
}
