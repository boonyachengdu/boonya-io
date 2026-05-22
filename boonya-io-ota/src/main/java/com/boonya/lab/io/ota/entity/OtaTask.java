package com.boonya.lab.io.ota.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "ota_task")
@Schema(description = "OTA升级任务")
public class OtaTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "任务ID")
    private Long id;

    @Column(nullable = false, length = 64)
    @Schema(description = "设备ID", example = "device_001")
    private String deviceId;

    @Column(nullable = false)
    @Schema(description = "固件ID")
    private Long firmwareId;

    @Column(length = 32)
    @Schema(description = "任务状态：pending-等待中, downloading-下载中, installing-安装中, success-成功, failed-失败, cancelled-已取消")
    private String status = "pending";

    @Column(columnDefinition = "TEXT")
    @Schema(description = "失败原因")
    private String errorMessage;

    @Column
    @Schema(description = "下载进度（0-100）")
    private Integer progress = 0;

    @Column
    @Schema(description = "开始时间")
    private LocalDateTime startTime;

    @Column
    @Schema(description = "完成时间")
    private LocalDateTime completeTime;

    @CreationTimestamp
    @Column(updatable = false)
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @UpdateTimestamp
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
