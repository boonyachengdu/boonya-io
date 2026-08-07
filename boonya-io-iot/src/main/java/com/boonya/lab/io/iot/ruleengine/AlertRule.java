package com.boonya.lab.io.iot.ruleengine;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 告警规则实体（对应 PostgreSQL 中的 alert_rule 表）。
 *
 * <p>iot 模块未引入 MyBatis-Plus，因此这里是一个纯 POJO，
 * 持久化由 {@link AlertRuleMapper} 通过 JdbcTemplate 完成。</p>
 *
 * <p>该实体仅承载可持久化的核心字段；内存中的 {@link Rule} 还包含多指标列表
 * (metrics / thresholds / operators) 与 actionConfig 等运行期扩展字段，
 * 这些复杂结构暂不入库，加载规则时保留内存默认值。</p>
 */
@Data
public class AlertRule {

    /** 自增主键 */
    private Long id;

    /** 业务规则标识（如 temp_high_alert 或 UUID） */
    private String ruleId;

    /** 规则名称 */
    private String ruleName;

    /** 设备 ID（可为空表示全局规则） */
    private String deviceId;

    /** 指标名：temp / humidity 等 */
    private String metric;

    /** 运算符: > / < / >= / <= / == / != */
    private String operator;

    /** 阈值 */
    private Double threshold;

    /** 多条件逻辑 AND/OR（单条件为空） */
    private String logic;

    /** 动作：ALERT / FORWARD / STORE */
    private String action;

    /** 严重级别：INFO / WARNING / CRITICAL */
    private String severity;

    /** 冷却时间（毫秒） */
    private Long cooldownMs;

    /** 是否启用 */
    private Boolean enabled;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
