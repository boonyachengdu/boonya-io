package com.boonya.lab.io.iot.ruleengine;

import lombok.Data;
import java.util.List;
import java.util.Map;

/**
 * 规则定义
 */
@Data
public class Rule {

    private String ruleId;
    private String ruleName;
    private String description;

    // 触发条件
    private String condition;  // 例如: "temp > 30"
    private String metric;     // 例如: "temp"
    private String operator;   // 例如: ">", "<", "==", ">="
    private Double threshold;  // 例如: 30.0

    // 修改内容：修改人：pengjunlin 时间：2026-08-04 18:30:00 -- start ----
    // 多指标支持
    private List<String> metrics;          // 多指标列表，例如: ["temp", "humidity"]
    private String logicOperator;          // 多条件逻辑运算符: "AND" / "OR"
    private Map<String, Double> thresholds; // 多指标阈值，例如: {"temp": 30.0, "humidity": 80.0}
    private Map<String, String> operators;  // 多指标运算符，例如: {"temp": ">", "humidity": ">"}

    // 冷却时间（秒）：同一规则在冷却期内不会重复触发
    private int cooldownSeconds;

    // 上次触发时间（毫秒时间戳）：用于冷却判断
    private long lastTriggerTime;
    // 修改内容：修改人：pengjunlin 时间：2026-08-04 18:30:00 -- end ----

    // 执行动作
    private String actionType; // 例如: "ALERT", "FORWARD", "STORE"
    private Map<String, Object> actionConfig;

    // 规则状态
    private boolean enabled;
    private int priority;
}
