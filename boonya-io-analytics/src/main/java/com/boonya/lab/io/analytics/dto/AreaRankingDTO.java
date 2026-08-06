package com.boonya.lab.io.analytics.dto;

/** 区域能耗排行 DTO */
public record AreaRankingDTO(
        String name,
        double electricityKwh,
        double waterM3,
        double carbonTons,
        String trend
) {}
