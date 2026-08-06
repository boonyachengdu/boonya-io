package com.boonya.lab.io.ota.controller;

import com.boonya.lab.io.common.response.PageResult;
import com.boonya.lab.io.common.response.Result;
import com.boonya.lab.io.ota.entity.OtaTask;
import com.boonya.lab.io.ota.service.OtaTaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ota/tasks")
@RequiredArgsConstructor
@Tag(name = "OTA任务管理", description = "升级任务创建、状态更新、查询接口")
public class OtaTaskController {

    private final OtaTaskService otaTaskService;

    @PostMapping
    @Operation(summary = "创建OTA任务", description = "为指定设备创建固件升级任务")
    public Result<OtaTask> createTask(
            @RequestParam String deviceId,
            @RequestParam Long firmwareId) {
        OtaTask task = otaTaskService.createOtaTask(deviceId, firmwareId);
        return Result.success("OTA任务创建成功", task);
    }

    @GetMapping
    @Operation(summary = "全局分页查询OTA任务", description = "管理员查询全部OTA任务，支持按设备ID和状态筛选，支持分页")
    public Result<PageResult<OtaTask>> queryTasks(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String deviceId,
            @RequestParam(required = false) String status) {
        Page<OtaTask> page = otaTaskService.queryTasks(pageNum, pageSize, deviceId, status);
        PageResult<OtaTask> pageResult = PageResult.of(
                page.getNumber() + 1L,
                page.getSize(),
                page.getTotalElements(),
                page.getContent()
        );
        return Result.success(pageResult);
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取任务详情", description = "根据ID获取任务详细信息")
    public Result<OtaTask> getTask(@PathVariable Long id) {
        OtaTask task = otaTaskService.getTask(id);
        return Result.success(task);
    }

    @GetMapping("/device/{deviceId}")
    @Operation(summary = "获取设备任务列表", description = "查询指定设备的所有升级任务")
    public Result<List<OtaTask>> getDeviceTasks(@PathVariable String deviceId) {
        List<OtaTask> tasks = otaTaskService.getDeviceTasks(deviceId);
        return Result.success(tasks);
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "更新任务状态", description = "设备端上报任务进度和状态")
    public Result<OtaTask> updateTaskStatus(
            @PathVariable Long id,
            @RequestBody Map<String, Object> update) {
        String status = (String) update.get("status");
        Integer progress = update.get("progress") != null ? 
                ((Number) update.get("progress")).intValue() : null;
        String errorMessage = (String) update.get("errorMessage");
        
        OtaTask task = otaTaskService.updateTaskStatus(id, status, progress, errorMessage);
        return Result.success("任务状态更新成功", task);
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "取消任务", description = "取消进行中的升级任务")
    public Result<OtaTask> cancelTask(@PathVariable Long id) {
        OtaTask task = otaTaskService.cancelTask(id);
        return Result.success("任务已取消", task);
    }
}
