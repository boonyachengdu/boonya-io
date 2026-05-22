package com.boonya.lab.io.device.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.boonya.lab.io.device.dto.DeviceQueryRequest;
import com.boonya.lab.io.device.dto.DeviceRegisterRequest;
import com.boonya.lab.io.device.dto.DeviceResponse;
import com.boonya.lab.io.device.service.DeviceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/devices")
@RequiredArgsConstructor
@Tag(name = "设备管理", description = "设备注册、查询、状态管理等接口")
public class DeviceController {

    private final DeviceService deviceService;

    @PostMapping("/register")
    @Operation(summary = "注册设备", description = "注册一个新的IoT设备")
    public ResponseEntity<DeviceResponse> registerDevice(@Valid @RequestBody DeviceRegisterRequest request) {
        DeviceResponse response = deviceService.registerDevice(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{deviceId}/activate")
    @Operation(summary = "激活设备", description = "激活已注册的设备")
    public ResponseEntity<Void> activateDevice(@PathVariable String deviceId) {
        deviceService.activateDevice(deviceId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{deviceId}/heartbeat")
    @Operation(summary = "设备心跳", description = "上报设备心跳，保持在线状态")
    public ResponseEntity<Void> updateHeartbeat(@PathVariable String deviceId) {
        deviceService.updateHeartbeat(deviceId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取设备信息", description = "根据ID获取设备详细信息")
    public ResponseEntity<DeviceResponse> getDevice(@PathVariable Long id) {
        DeviceResponse response = deviceService.getDevice(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/by-id/{deviceId}")
    @Operation(summary = "根据设备ID获取信息", description = "根据设备唯一标识获取设备信息")
    public ResponseEntity<DeviceResponse> getDeviceByDeviceId(@PathVariable String deviceId) {
        DeviceResponse response = deviceService.getDeviceResponseByDeviceId(deviceId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/query")
    @Operation(summary = "查询设备列表", description = "分页查询设备列表，支持多条件筛选")
    public ResponseEntity<Page<DeviceResponse>> queryDevices(DeviceQueryRequest request) {
        Page<DeviceResponse> page = deviceService.queryDevices(request);
        return ResponseEntity.ok(page);
    }

    @GetMapping("/online")
    @Operation(summary = "获取在线设备", description = "获取所有在线设备列表")
    public ResponseEntity<List<DeviceResponse>> getOnlineDevices() {
        List<DeviceResponse> devices = deviceService.getOnlineDevices();
        return ResponseEntity.ok(devices);
    }

    @GetMapping("/{deviceId}/status")
    @Operation(summary = "获取设备状态", description = "获取指定设备的当前状态")
    public ResponseEntity<Map<String, String>> getDeviceStatus(@PathVariable String deviceId) {
        String status = deviceService.getDeviceStatus(deviceId);
        Map<String, String> result = new HashMap<>();
        result.put("deviceId", deviceId);
        result.put("status", status);
        return ResponseEntity.ok(result);
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新设备状态", description = "更新设备状态（online/offline/disabled）")
    public ResponseEntity<Void> updateDeviceStatus(@PathVariable Long id, @RequestParam String status) {
        DeviceResponse device = deviceService.getDevice(id);
        deviceService.updateDeviceStatus(device.getDeviceId(), status);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除设备", description = "逻辑删除设备")
    public ResponseEntity<Void> deleteDevice(@PathVariable Long id) {
        deviceService.deleteDevice(id);
        return ResponseEntity.ok().build();
    }
}
