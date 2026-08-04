package com.boonya.lab.io.iot.ruleengine;

import com.boonya.lab.io.iot.event.OverTempEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
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

    // 修改内容：修改人：pengjunlin 时间：2026-08-04 18:10:00 -- start ----
    // 注入 WebSocket 用于 FORWARD 动作转发
    private final SimpMessagingTemplate websocket;

    /**
     * 注册规则
     */
    public void registerRule(Rule rule) {
        rules.put(rule.getRuleId(), rule);
        log.info("Rule registered: {} - {}", rule.getRuleId(), rule.getRuleName());
    }

    /**
     * 获取所有规则
     */
    public List<Rule> getAllRules() {
        return new ArrayList<>(rules.values());
    }

    /**
     * 启用规则
     */
    public boolean enableRule(String ruleId) {
        Rule rule = rules.get(ruleId);
        if (rule == null) {
            return false;
        }
        rule.setEnabled(true);
        log.info("Rule enabled: {}", ruleId);
        return true;
    }

    /**
     * 禁用规则
     */
    public boolean disableRule(String ruleId) {
        Rule rule = rules.get(ruleId);
        if (rule == null) {
            return false;
        }
        rule.setEnabled(false);
        log.info("Rule disabled: {}", ruleId);
        return true;
    }

    /**
     * 删除规则
     */
    public boolean deleteRule(String ruleId) {
        Rule removed = rules.remove(ruleId);
        if (removed != null) {
            log.info("Rule deleted: {}", ruleId);
            return true;
        }
        return false;
    }
    // 修改内容：修改人：pengjunlin 时间：2026-08-04 18:10:00 -- end ----

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
                            // 修改内容：修改人：pengjunlin 时间：2026-08-04 18:30:00 -- start ----
                            // 冷却时间检查：若在冷却期内则跳过本次触发
                            if (isInCooldown(rule)) {
                                log.debug("Rule {} skipped due to cooldown (lastTriggerTime={}, cooldownSeconds={})",
                                        rule.getRuleId(), rule.getLastTriggerTime(), rule.getCooldownSeconds());
                                return;
                            }
                            // 触发动作前更新上次触发时间
                            rule.setLastTriggerTime(System.currentTimeMillis());
                            // 修改内容：修改人：pengjunlin 时间：2026-08-04 18:30:00 -- end ----
                            executeAction(rule, deviceId, data);
                        }
                    } catch (Exception e) {
                        log.error("Error evaluating rule: {}", rule.getRuleId(), e);
                    }
                });
    }

    // 修改内容：修改人：pengjunlin 时间：2026-08-04 18:30:00 -- start ----
    /**
     * 判断规则是否处于冷却期内
     */
    private boolean isInCooldown(Rule rule) {
        if (rule.getCooldownSeconds() <= 0 || rule.getLastTriggerTime() <= 0) {
            return false;
        }
        long elapsedMillis = System.currentTimeMillis() - rule.getLastTriggerTime();
        return elapsedMillis < rule.getCooldownSeconds() * 1000L;
    }
    // 修改内容：修改人：pengjunlin 时间：2026-08-04 18:30:00 -- end ----

    /**
     * 检查是否满足条件
     */
    private boolean matchesCondition(Rule rule, Map<String, Object> data) {
        // 修改内容：修改人：pengjunlin 时间：2026-08-04 18:30:00 -- start ----
        // 多条件模式：当 metrics 不为空时使用多指标逻辑
        if (rule.getMetrics() != null && !rule.getMetrics().isEmpty()) {
            return matchesMultiCondition(rule, data);
        }
        // 修改内容：修改人：pengjunlin 时间：2026-08-04 18:30:00 -- end ----

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
            case "!=" -> Math.abs(numericValue - threshold) >= 0.001;
            default -> false;
        };
    }

    // 修改内容：修改人：pengjunlin 时间：2026-08-04 18:30:00 -- start ----
    /**
     * 多条件匹配：根据 logicOperator (AND/OR) 组合多指标判断
     * - 默认逻辑运算符为 AND
     * - 每个指标优先从 thresholds 取阈值，从 operators 取运算符；
     *   若 operators 未配置则回退到 rule.operator
     */
    private boolean matchesMultiCondition(Rule rule, Map<String, Object> data) {
        String logic = rule.getLogicOperator() == null ? "AND" : rule.getLogicOperator().toUpperCase();
        boolean useAnd = "AND".equals(logic);

        for (String metric : rule.getMetrics()) {
            Object value = data.get(metric);
            boolean matched = false;
            if (value != null && value instanceof Number) {
                double numericValue = ((Number) value).doubleValue();
                Double threshold = rule.getThresholds() != null ? rule.getThresholds().get(metric) : null;
                if (threshold != null) {
                    String op = (rule.getOperators() != null && rule.getOperators().get(metric) != null)
                            ? rule.getOperators().get(metric)
                            : rule.getOperator();
                    matched = compareValue(numericValue, threshold, op);
                }
            }

            if (useAnd && !matched) {
                return false; // AND 模式下任一条件不满足即整体不满足
            }
            if (!useAnd && matched) {
                return true;  // OR 模式下任一条件满足即整体满足
            }
        }
        // AND 模式全部满足返回 true；OR 模式全部不满足返回 false
        return useAnd;
    }

    /**
     * 根据运算符比较数值
     */
    private boolean compareValue(double numericValue, double threshold, String operator) {
        if (operator == null) {
            return false;
        }
        return switch (operator) {
            case ">" -> numericValue > threshold;
            case "<" -> numericValue < threshold;
            case ">=" -> numericValue >= threshold;
            case "<=" -> numericValue <= threshold;
            case "==" -> Math.abs(numericValue - threshold) < 0.001;
            case "!=" -> Math.abs(numericValue - threshold) >= 0.001;
            default -> false;
        };
    }
    // 修改内容：修改人：pengjunlin 时间：2026-08-04 18:30:00 -- end ----

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

    // 修改内容：修改人：pengjunlin 时间：2026-08-04 18:10:00 -- start ----
    /**
     * 处理转发动作 - 通过 WebSocket 推送到指定 topic
     */
    private void handleForward(Rule rule, String deviceId, Map<String, Object> data) {
        String targetTopic = "/topic/device/" + deviceId;
        if (rule.getActionConfig() != null && rule.getActionConfig().containsKey("targetTopic")) {
            targetTopic = (String) rule.getActionConfig().get("targetTopic");
        }
        websocket.convertAndSend(targetTopic, Map.of(
                "deviceId", deviceId,
                "data", data,
                "ruleId", rule.getRuleId(),
                "timestamp", System.currentTimeMillis()
        ));
        log.info("Forwarded data for device {} to topic: {}", deviceId, targetTopic);
    }

    /**
     * 处理存储动作 - 将数据写入特殊存储（通过 WebSocket 推送到 /topic/stored）
     */
    private void handleStore(Rule rule, String deviceId, Map<String, Object> data) {
        String storageKey = "rule_store_" + rule.getRuleId();
        if (rule.getActionConfig() != null && rule.getActionConfig().containsKey("storageKey")) {
            storageKey = (String) rule.getActionConfig().get("storageKey");
        }
        log.info("Storing data for device {} with storage key: {}", deviceId, storageKey);
        // 推送存储通知到 WebSocket
        websocket.convertAndSend("/topic/stored", Map.of(
                "storageKey", storageKey,
                "deviceId", deviceId,
                "ruleId", rule.getRuleId(),
                "data", data,
                "timestamp", System.currentTimeMillis()
        ));
    }
    // 修改内容：修改人：pengjunlin 时间：2026-08-04 18:10:00 -- end ----

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
