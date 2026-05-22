package com.boonya.lab.io.iot.ruleengine;

import lombok.Data;
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
    
    // 执行动作
    private String actionType; // 例如: "ALERT", "FORWARD", "STORE"
    private Map<String, Object> actionConfig;
    
    // 规则状态
    private boolean enabled;
    private int priority;
}
