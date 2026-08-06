package com.boonya.lab.io.ota.controller;

import com.boonya.lab.io.common.response.PageResult;
import com.boonya.lab.io.common.response.Result;
import com.boonya.lab.io.ota.dto.FirmwareUploadRequest;
import com.boonya.lab.io.ota.entity.Firmware;
import com.boonya.lab.io.ota.service.FirmwareService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/firmware")
@RequiredArgsConstructor
@Tag(name = "固件管理", description = "固件上传、发布、查询接口")
public class FirmwareController {

    private final FirmwareService firmwareService;

    @PostMapping
    @Operation(summary = "上传固件", description = "上传新的固件版本到 MinIO")
    public Result<Firmware> uploadFirmware(@Valid FirmwareUploadRequest request) {
        Firmware firmware = firmwareService.uploadFirmware(request);
        return Result.success("固件上传成功", firmware);
    }

    @GetMapping
    @Operation(summary = "获取固件列表（分页）", description = "分页查询固件列表，可按设备型号和状态筛选")
    public Result<PageResult<Firmware>> listFirmwares(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String deviceModel,
            @RequestParam(required = false) String status) {
        Page<Firmware> page = firmwareService.listFirmwares(pageNum, pageSize, deviceModel, status);
        PageResult<Firmware> pageResult = PageResult.of(
                page.getNumber() + 1L,
                page.getSize(),
                page.getTotalElements(),
                page.getContent()
        );
        return Result.success(pageResult);
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取固件详情", description = "根据ID获取固件详细信息")
    public Result<Firmware> getFirmware(@PathVariable Long id) {
        Firmware firmware = firmwareService.getFirmware(id);
        return Result.success(firmware);
    }

    @PostMapping("/{id}/publish")
    @Operation(summary = "发布固件", description = "将固件状态改为已发布")
    public Result<Firmware> publishFirmware(@PathVariable Long id) {
        Firmware firmware = firmwareService.publishFirmware(id);
        return Result.success("固件发布成功", firmware);
    }

    @PostMapping("/{id}/archive")
    @Operation(summary = "归档固件", description = "将固件状态改为已归档")
    public Result<Firmware> archiveFirmware(@PathVariable Long id) {
        Firmware firmware = firmwareService.archiveFirmware(id);
        return Result.success("固件归档成功", firmware);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除固件", description = "删除草稿状态的固件")
    public Result<Void> deleteFirmware(@PathVariable Long id) {
        firmwareService.deleteFirmware(id);
        return Result.success("固件删除成功", null);
    }
}
