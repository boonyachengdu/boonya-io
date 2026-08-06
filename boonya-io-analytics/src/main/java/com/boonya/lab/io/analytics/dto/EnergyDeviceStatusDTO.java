package com.boonya.lab.io.analytics.dto;

/** 能碳设备状态 DTO */
public record EnergyDeviceStatusDTO(
        String deviceId,
        String deviceName,
        String deviceType,
        String status,
        double value,
        String unit,
        String location
) {}
