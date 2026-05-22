package com.boonya.lab.io.iot.ruleengine;

import com.boonya.lab.io.iot.event.OverTempEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 规则引擎 - 处理设备数据并触发规则
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RuleEngine {

    private final ApplicationEventPublisher eventPublisher;
    private final Map<String, Rule> rules = new ConcurrentHashMap<>();

    /**
     * 注册规则
     */
    public void registerRule(Rule rule) {
        rules.put(rule.getRuleId(), rule);
        log.info("Rule registered: {} - {}", rule.getRuleId(), rule.getRuleName());
    }

    /**
     * 评估设备数据
     */
    public void evaluate(String deviceId, Map<String, Object> data) {
        rules.values().stream()
                .filter(Rule::isEnabled)
                .sorted((r1, r2) -> Integer.compare(r2.getPriority(), r1.getPriority()))
                .forEach(rule -> {
                    try {
                        if (matchesCondition(rule, data)) {
                            executeAction(rule, deviceId, data);
                        }
                    } catch (Exception e) {
                        log.error("Error evaluating rule: {}", rule.getRuleId(), e);
                    }
                });
    }

    /**
     * 检查是否满足条件
     */
    private boolean matchesCondition(Rule rule, Map<String, Object> data) {
        Object value = data.get(rule.getMetric());
        if (value == null) {
            return false;
        }

        double numericValue = ((Number) value).doubleValue();
        double threshold = rule.getThreshold();

        return switch (rule.getOperator()) {
            case ">" -> numericValue > threshold;
            case "<" -> numericValue < threshold;
            case ">=" -> numericValue >= threshold;
            case "<=" -> numericValue <= threshold;
            case "==" -> Math.abs(numericValue - threshold) < 0.001;
            default -> false;
        };
    }

    /**
     * 执行动作
     */
    private void executeAction(Rule rule, String deviceId, Map<String, Object> data) {
        log.info("Rule triggered: {} for device {}", rule.getRuleId(), deviceId);

        switch (rule.getActionType()) {
            case "ALERT" -> handleAlert(rule, deviceId, data);
            case "FORWARD" -> handleForward(rule, deviceId, data);
            case "STORE" -> handleStore(rule, deviceId, data);
            default -> log.warn("Unknown action type: {}", rule.getActionType());
        }
    }

    /**
     * 处理告警动作
     */
    private void handleAlert(Rule rule, String deviceId, Map<String, Object> data) {
        double temp = ((Number) data.get("temp")).doubleValue();
        long ts = data.containsKey("ts") ? ((Number) data.get("ts")).longValue() : System.currentTimeMillis();

        // 发布温度过高事件
        OverTempEvent event = new OverTempEvent(deviceId, temp, ts);
        eventPublisher.publishEvent(event);

        log.warn("Alert triggered: Device {} temperature {:.2f}°C exceeds threshold {:.2f}°C",
                deviceId, temp, rule.getThreshold());
    }

    /**
     * 处理转发动作
     */
    private void handleForward(Rule rule, String deviceId, Map<String, Object> data) {
        // TODO: 实现数据转发到其他系统
        log.info("Forwarding data for device {}: {}", deviceId, data);
    }

    /**
     * 处理存储动作
     */
    private void handleStore(Rule rule, String deviceId, Map<String, Object> data) {
        // TODO: 实现特殊存储逻辑
        log.info("Storing data for device {}: {}", deviceId, data);
    }

    /**
     * 初始化默认规则
     */
    public void initDefaultRules() {
        // 温度过高告警规则
        Rule tempAlertRule = new Rule();
        tempAlertRule.setRuleId("temp_high_alert");
        tempAlertRule.setRuleName("温度过高告警");
        tempAlertRule.setDescription("当温度超过30度时触发告警");
        tempAlertRule.setMetric("temp");
        tempAlertRule.setOperator(">");
        tempAlertRule.setThreshold(30.0);
        tempAlertRule.setActionType("ALERT");
        tempAlertRule.setEnabled(true);
        tempAlertRule.setPriority(1);

        registerRule(tempAlertRule);

        // 温度过低告警规则
        Rule tempLowRule = new Rule();
        tempLowRule.setRuleId("temp_low_alert");
        tempLowRule.setRuleName("温度过低告警");
        tempLowRule.setDescription("当温度低于5度时触发告警");
        tempLowRule.setMetric("temp");
        tempLowRule.setOperator("<");
        tempLowRule.setThreshold(5.0);
        tempLowRule.setActionType("ALERT");
        tempLowRule.setEnabled(true);
        tempLowRule.setPriority(1);

        registerRule(tempLowRule);

        log.info("Default rules initialized");
    }
}
