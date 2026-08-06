package com.boonya.lab.io.analytics.dto;

/** 能源趋势单项 DTO */
public record EnergyTrendItemDTO(
        String time,
        double electricityKwh,
        double waterM3,
        double solarKwh,
        double carbonTons
) {}
