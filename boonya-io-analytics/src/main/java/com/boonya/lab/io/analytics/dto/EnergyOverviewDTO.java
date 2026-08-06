package com.boonya.lab.io.analytics.dto;

/** 能碳总览 DTO */
public record EnergyOverviewDTO(
        String siteName,
        String period,
        double electricityKwh,
        double waterM3,
        double solarKwh,
        double storageDischargeKwh,
        double energyCostCny,
        double carbonTons,
        double carbonReductionTons,
        int activeAlarms,
        int onlineDevices,
        int totalDevices
) {}
