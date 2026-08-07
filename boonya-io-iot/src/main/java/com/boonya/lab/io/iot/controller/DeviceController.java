package com.boonya.lab.io.iot.controller;

import com.boonya.lab.io.iot.model.DeviceData;
import com.boonya.lab.io.iot.model.DeviceLog;
import com.boonya.lab.io.iot.service.DeviceLogService;
import com.boonya.lab.io.iot.service.TimeSeriesService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/iot/devices")
@RequiredArgsConstructor
public class DeviceController {

    private final TimeSeriesService timeSeriesService;
    private final DeviceLogService deviceLogService;

    @GetMapping("/{deviceId}/history")
    public ResponseEntity<List<DeviceData>> getHistory(
            @PathVariable String deviceId,
            @RequestParam long startTs,
            @RequestParam long endTs) {
        return ResponseEntity.ok(timeSeriesService.queryHistory(deviceId, startTs, endTs));
    }

    @GetMapping("/{deviceId}/logs")
    public ResponseEntity<List<DeviceLog>> getLogs(@PathVariable String deviceId) {
        return ResponseEntity.ok(deviceLogService.getLogsByDevice(deviceId));
    }
}
