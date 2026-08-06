package com.boonya.lab.io.analytics.dto;

/** 能耗告警 DTO */
public record EnergyAlarmDTO(
        String level,
        String title,
        String deviceId,
        String description,
        String status,
        String time
) {}
