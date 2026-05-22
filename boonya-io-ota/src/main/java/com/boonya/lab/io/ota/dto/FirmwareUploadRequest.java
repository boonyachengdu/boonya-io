package com.boonya.lab.io.ota.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
@Schema(description = "固件上传请求")
public class FirmwareUploadRequest {

    @NotBlank(message = "设备型号不能为空")
    @Schema(description = "设备型号", example = "sensor-v1", requiredMode = Schema.RequiredMode.REQUIRED)
    private String deviceModel;

    @NotBlank(message = "版本号不能为空")
    @Schema(description = "版本号", example = "v1.0.0", requiredMode = Schema.RequiredMode.REQUIRED)
    private String version;

    @Schema(description = "更新说明")
    private String description;

    @Schema(description = "是否强制升级")
    private Boolean forceUpdate = false;

    @Schema(description = "固件文件", requiredMode = Schema.RequiredMode.REQUIRED)
    private MultipartFile file;
}
