package com.boonya.lab.io.device.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.boonya.lab.io.common.constant.CommonConstants;
import com.boonya.lab.io.common.exception.ResourceNotFoundException;
import com.boonya.lab.io.common.tenant.TenantContext;
import com.boonya.lab.io.common.util.TokenUtils;
import com.boonya.lab.io.device.dto.DeviceQueryRequest;
import com.boonya.lab.io.device.dto.DeviceRegisterRequest;
import com.boonya.lab.io.device.dto.DeviceResponse;
import com.boonya.lab.io.device.mapper.DeviceMapper;
import com.boonya.lab.io.device.entity.Device;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceService {

    private final DeviceMapper deviceMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final long HEARTBEAT_TIMEOUT = 300;

    @Transactional
    public DeviceResponse registerDevice(DeviceRegisterRequest request) {
        LambdaQueryWrapper<Device> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Device::getDeviceId, request.getDeviceId());
        Device existingDevice = deviceMapper.selectOne(wrapper);

        if (existingDevice != null) {
            throw new RuntimeException("设备ID已存在: " + request.getDeviceId());
        }

        Device device = new Device();
        device.setDeviceId(request.getDeviceId());
        device.setDeviceName(request.getDeviceName());
        device.setDeviceType(request.getDeviceType());
        device.setModel(request.getModel());
        device.setGroupId(request.getGroupId());
        device.setLocation(request.getLocation());
        device.setDescription(request.getDescription());
        device.setStatus(CommonConstants.DeviceStatus.INACTIVE);
        device.setAuthToken(TokenUtils.generateDeviceToken());

        deviceMapper.insert(device);

        log.info("设备注册成功: {}", request.getDeviceId());

        return convertToResponse(device);
    }

    @Transactional
    public void activateDevice(String deviceId) {
        Device device = getDeviceByDeviceId(deviceId);

        if (device == null) {
            throw new ResourceNotFoundException("设备不存在: " + deviceId);
        }

        device.setStatus(CommonConstants.DeviceStatus.ONLINE);
        device.setLastHeartbeat(LocalDateTime.now());
        deviceMapper.updateById(device);

        updateDeviceStatusCache(deviceId, CommonConstants.DeviceStatus.ONLINE);

        log.info("设备激活: {}", deviceId);
    }

    @Transactional
    public void updateHeartbeat(String deviceId) {
        Device device = getDeviceByDeviceId(deviceId);

        if (device == null) {
            throw new ResourceNotFoundException("设备不存在: " + deviceId);
        }

        device.setLastHeartbeat(LocalDateTime.now());

        if (CommonConstants.DeviceStatus.OFFLINE.equals(device.getStatus())) {
            device.setStatus(CommonConstants.DeviceStatus.ONLINE);
        }

        deviceMapper.updateById(device);

        updateDeviceStatusCache(deviceId, CommonConstants.DeviceStatus.ONLINE);

        log.debug("设备心跳更新: {}", deviceId);
    }

    public DeviceResponse getDevice(Long id) {
        Device device = deviceMapper.selectById(id);
        if (device == null) {
            throw new ResourceNotFoundException("Device", String.valueOf(id));
        }
        return convertToResponse(device);
    }

    public DeviceResponse getDeviceResponseByDeviceId(String deviceId) {
        LambdaQueryWrapper<Device> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Device::getDeviceId, deviceId);
        Device device = deviceMapper.selectOne(wrapper);
        if (device == null) {
            throw new ResourceNotFoundException("Device", deviceId);
        }
        return convertToResponse(device);
    }

    public Page<DeviceResponse> queryDevices(DeviceQueryRequest request) {
        LambdaQueryWrapper<Device> wrapper = new LambdaQueryWrapper<>();
        // 多租户过滤：自动按当前租户ID过滤数据
        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null && tenantId > 0) {
            wrapper.eq(Device::getTenantId, tenantId);
        }

        if (request.getDeviceId() != null && !request.getDeviceId().isEmpty()) {
            wrapper.like(Device::getDeviceId, request.getDeviceId());
        }

        if (request.getDeviceName() != null && !request.getDeviceName().isEmpty()) {
            wrapper.like(Device::getDeviceName, request.getDeviceName());
        }

        if (request.getDeviceType() != null && !request.getDeviceType().isEmpty()) {
            wrapper.eq(Device::getDeviceType, request.getDeviceType());
        }

        if (request.getStatus() != null && !request.getStatus().isEmpty()) {
            wrapper.eq(Device::getStatus, request.getStatus());
        }

        if (request.getGroupId() != null) {
            wrapper.eq(Device::getGroupId, request.getGroupId());
        }

        wrapper.orderByDesc(Device::getCreateTime);

        Page<Device> page = new Page<>(request.getPageNum(), request.getPageSize());
        Page<Device> devicePage = deviceMapper.selectPage(page, wrapper);

        Page<DeviceResponse> responsePage = new Page<>();
        responsePage.setCurrent(devicePage.getCurrent());
        responsePage.setSize(devicePage.getSize());
        responsePage.setTotal(devicePage.getTotal());
        responsePage.setRecords(
                devicePage.getRecords().stream()
                        .map(this::convertToResponse)
                        .collect(Collectors.toList())
        );

        return responsePage;
    }

    @Transactional
    public void deleteDevice(Long id) {
        Device device = deviceMapper.selectById(id);
        if (device == null) {
            throw new ResourceNotFoundException("Device", String.valueOf(id));
        }

        deviceMapper.deleteById(id);

        redisTemplate.delete(CommonConstants.RedisKeys.DEVICE_STATUS + device.getDeviceId());

        log.info("设备删除: {}", device.getDeviceId());
    }

    @Transactional
    public void updateDeviceStatus(String deviceId, String status) {
        Device device = getDeviceByDeviceId(deviceId);
        device.setStatus(status);

        if (CommonConstants.DeviceStatus.ONLINE.equals(status)) {
            device.setLastHeartbeat(LocalDateTime.now());
        }

        deviceMapper.updateById(device);
        updateDeviceStatusCache(deviceId, status);

        log.info("设备状态更新: {} -> {}", deviceId, status);
    }

    public String getDeviceStatus(String deviceId) {
        String cachedStatus = (String) redisTemplate.opsForValue().get(CommonConstants.RedisKeys.DEVICE_STATUS + deviceId);
        if (cachedStatus != null) {
            return cachedStatus;
        }

        Device device = getDeviceByDeviceId(deviceId);
        return device.getStatus();
    }

    public List<DeviceResponse> getOnlineDevices() {
        LambdaQueryWrapper<Device> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Device::getStatus, CommonConstants.DeviceStatus.ONLINE);
        List<Device> devices = deviceMapper.selectList(wrapper);

        return devices.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public Device getDeviceByDeviceId(String deviceId) {
        LambdaQueryWrapper<Device> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Device::getDeviceId, deviceId);
        return deviceMapper.selectOne(wrapper);
    }

    private void updateDeviceStatusCache(String deviceId, String status) {
        redisTemplate.opsForValue().set(CommonConstants.RedisKeys.DEVICE_STATUS + deviceId, status, HEARTBEAT_TIMEOUT, TimeUnit.SECONDS);
    }

    public DeviceResponse convertToResponse(Device device) {
        return DeviceResponse.builder()
                .id(device.getId())
                .deviceId(device.getDeviceId())
                .deviceName(device.getDeviceName())
                .deviceType(device.getDeviceType())
                .model(device.getModel())
                .firmwareVersion(device.getFirmwareVersion())
                .status(device.getStatus())
                .lastHeartbeat(device.getLastHeartbeat() != null ? device.getLastHeartbeat().toString() : null)
                .groupId(device.getGroupId())
                .location(device.getLocation())
                .description(device.getDescription())
                .createTime(device.getCreateTime() != null ? device.getCreateTime().toString() : null)
                .build();
    }
}
