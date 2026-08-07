package com.boonya.lab.io.device.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.boonya.lab.io.common.exception.BusinessException;
import com.boonya.lab.io.common.response.PageResult;
import com.boonya.lab.io.device.entity.Alert;
import com.boonya.lab.io.device.mapper.AlertMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlertManageService {

    private final AlertMapper alertMapper;

    public PageResult<Alert> queryAlerts(int pageNum, int pageSize, String deviceId,
                                          String severity, String status,
                                          String startTime, String endTime) {
        LambdaQueryWrapper<Alert> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(deviceId)) {
            wrapper.eq(Alert::getDeviceId, deviceId);
        }
        if (StringUtils.hasText(severity)) {
            wrapper.eq(Alert::getSeverity, severity);
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(Alert::getStatus, status);
        }
        if (StringUtils.hasText(startTime)) {
            wrapper.ge(Alert::getTriggerTime, LocalDateTime.parse(startTime));
        }
        if (StringUtils.hasText(endTime)) {
            wrapper.le(Alert::getTriggerTime, LocalDateTime.parse(endTime));
        }
        wrapper.orderByDesc(Alert::getTriggerTime);

        Page<Alert> page = alertMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        return PageResult.of(page.getCurrent(), page.getSize(), page.getTotal(), page.getRecords());
    }

    public Alert getAlertById(Long id) {
        Alert alert = alertMapper.selectById(id);
        if (alert == null) {
            throw new BusinessException("告警不存在: " + id);
        }
        return alert;
    }

    @Transactional
    public Alert acknowledgeAlert(Long id, String operator) {
        Alert alert = getAlertById(id);
        if (!"PENDING".equals(alert.getStatus())) {
            throw new BusinessException("仅待处理告警可确认，当前状态: " + alert.getStatus());
        }
        alert.setStatus("ACKNOWLEDGED");
        alert.setAckTime(LocalDateTime.now());
        if (StringUtils.hasText(operator)) {
            alert.setOperator(operator);
        }
        alertMapper.updateById(alert);
        log.info("告警已确认: id={}, operator={}", id, operator);
        return alert;
    }

    @Transactional
    public Alert resolveAlert(Long id, String operator) {
        Alert alert = getAlertById(id);
        if ("CLOSED".equals(alert.getStatus())) {
            throw new BusinessException("已关闭的告警不可解决");
        }
        alert.setStatus("RESOLVED");
        alert.setResolveTime(LocalDateTime.now());
        if (StringUtils.hasText(operator)) {
            alert.setOperator(operator);
        }
        alertMapper.updateById(alert);
        log.info("告警已解决: id={}, operator={}", id, operator);
        return alert;
    }

    @Transactional
    public Alert closeAlert(Long id, String operator) {
        Alert alert = getAlertById(id);
        alert.setStatus("CLOSED");
        if (StringUtils.hasText(operator)) {
            alert.setOperator(operator);
        }
        alertMapper.updateById(alert);
        log.info("告警已关闭: id={}, operator={}", id, operator);
        return alert;
    }

    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();

        // 按状态统计
        for (String status : new String[]{"PENDING", "ACKNOWLEDGED", "RESOLVED", "CLOSED"}) {
            LambdaQueryWrapper<Alert> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Alert::getStatus, status);
            stats.put(status.toLowerCase(), alertMapper.selectCount(wrapper));
        }

        // 按严重级别统计今日告警
        LocalDateTime todayStart = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
        for (String severity : new String[]{"INFO", "WARNING", "CRITICAL"}) {
            LambdaQueryWrapper<Alert> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Alert::getSeverity, severity)
                   .ge(Alert::getTriggerTime, todayStart);
            stats.put(severity.toLowerCase() + "_today", alertMapper.selectCount(wrapper));
        }

        // 今日总数
        LambdaQueryWrapper<Alert> todayWrapper = new LambdaQueryWrapper<>();
        todayWrapper.ge(Alert::getTriggerTime, todayStart);
        stats.put("total_today", alertMapper.selectCount(todayWrapper));

        return stats;
    }

    /**
     * 内部接口：创建告警记录（供 iot 模块通过 JdbcTemplate 调用时参考）
     */
    @Transactional
    public Alert createAlert(String deviceId, String alertType, String severity,
                              String title, String message,
                              Double metricValue, Double threshold) {
        Alert alert = new Alert();
        alert.setDeviceId(deviceId);
        alert.setAlertType(alertType);
        alert.setSeverity(severity);
        alert.setTitle(title);
        alert.setMessage(message);
        alert.setMetricValue(metricValue);
        alert.setThreshold(threshold);
        alert.setStatus("PENDING");
        alert.setTriggerTime(LocalDateTime.now());
        alertMapper.insert(alert);
        log.info("告警已创建: device={}, type={}, severity={}", deviceId, alertType, severity);
        return alert;
    }
}
