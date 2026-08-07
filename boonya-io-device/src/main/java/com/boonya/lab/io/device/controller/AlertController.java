package com.boonya.lab.io.device.controller;

import com.boonya.lab.io.common.response.PageResult;
import com.boonya.lab.io.common.response.Result;
import com.boonya.lab.io.device.entity.Alert;
import com.boonya.lab.io.device.service.AlertManageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
@Tag(name = "告警管理", description = "告警查询、状态流转、统计等接口")
public class AlertController {

    private final AlertManageService alertManageService;

    @GetMapping
    @Operation(summary = "分页查询告警", description = "支持按设备/级别/状态/时间范围筛选")
    public Result<PageResult<Alert>> queryAlerts(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String deviceId,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {
        return Result.success(alertManageService.queryAlerts(pageNum, pageSize, deviceId, severity, status, startTime, endTime));
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取告警详情")
    public Result<Alert> getAlertById(@PathVariable Long id) {
        return Result.success(alertManageService.getAlertById(id));
    }

    @PutMapping("/{id}/acknowledge")
    @Operation(summary = "确认告警", description = "将 PENDING 状态告警转为 ACKNOWLEDGED")
    public Result<Alert> acknowledgeAlert(@PathVariable Long id,
                                           @RequestParam(required = false) String operator) {
        return Result.success(alertManageService.acknowledgeAlert(id, operator));
    }

    @PutMapping("/{id}/resolve")
    @Operation(summary = "解决告警", description = "将告警转为 RESOLVED 状态")
    public Result<Alert> resolveAlert(@PathVariable Long id,
                                       @RequestParam(required = false) String operator) {
        return Result.success(alertManageService.resolveAlert(id, operator));
    }

    @PutMapping("/{id}/close")
    @Operation(summary = "关闭告警", description = "将告警转为 CLOSED 状态")
    public Result<Alert> closeAlert(@PathVariable Long id,
                                     @RequestParam(required = false) String operator) {
        return Result.success(alertManageService.closeAlert(id, operator));
    }

    @GetMapping("/statistics")
    @Operation(summary = "告警统计", description = "按状态/级别聚合统计今日告警")
    public Result<Map<String, Object>> getStatistics() {
        return Result.success(alertManageService.getStatistics());
    }
}
