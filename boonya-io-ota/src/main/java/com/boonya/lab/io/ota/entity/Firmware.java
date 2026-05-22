package com.boonya.lab.io.ota.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "firmware")
@Schema(description = "固件版本")
public class Firmware {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "固件ID")
    private Long id;

    @Column(nullable = false, length = 64)
    @Schema(description = "设备型号", example = "sensor-v1")
    private String deviceModel;

    @Column(nullable = false, length = 32)
    @Schema(description = "版本号", example = "v1.0.0")
    private String version;

    @Column(columnDefinition = "TEXT")
    @Schema(description = "更新说明")
    private String description;

    @Column(length = 512)
    @Schema(description = "文件路径（MinIO）")
    private String filePath;

    @Column(length = 256)
    @Schema(description = "文件名")
    private String fileName;

    @Column
    @Schema(description = "文件大小（字节）")
    private Long fileSize;

    @Column(length = 64)
    @Schema(description = "文件MD5校验值")
    private String md5Checksum;

    @Column(nullable = false)
    @Schema(description = "发布状态：draft-草稿, published-已发布, archived-已归档")
    private String status = "draft";

    @Column
    @Schema(description = "是否强制升级")
    private Boolean forceUpdate = false;

    @CreationTimestamp
    @Column(updatable = false)
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @UpdateTimestamp
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    @Column
    @Schema(description = "发布时间")
    private LocalDateTime publishTime;
}
