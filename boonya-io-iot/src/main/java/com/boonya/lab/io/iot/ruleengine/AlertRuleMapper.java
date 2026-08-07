package com.boonya.lab.io.iot.ruleengine;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

/**
 * alert_rule 表的 JdbcTemplate 持久化访问层。
 *
 * <p>iot 模块未引入 MyBatis-Plus，故不使用 BaseMapper，而是直接基于 pgJdbcTemplate 实现 CRUD。
 * 当 pgJdbcTemplate 未装配（未配置 pg.datasource.url）时，所有方法安全降级为空操作，
 * 不影响 RuleEngine 内存模式运行。</p>
 */
@Slf4j
@Repository
public class AlertRuleMapper {

    @Autowired(required = false)
    @Qualifier("pgJdbcTemplate")
    private JdbcTemplate pgJdbcTemplate;

    private static final RowMapper<AlertRule> ROW_MAPPER = (ResultSet rs, int rowNum) -> {
        AlertRule r = new AlertRule();
        r.setId(rs.getLong("id"));
        r.setRuleId(rs.getString("rule_id"));
        r.setRuleName(rs.getString("rule_name"));
        r.setDeviceId(rs.getString("device_id"));
        r.setMetric(rs.getString("metric"));
        r.setOperator(rs.getString("operator"));
        double threshold = rs.getDouble("threshold");
        r.setThreshold(rs.wasNull() ? null : threshold);
        r.setLogic(rs.getString("logic"));
        r.setAction(rs.getString("action"));
        r.setSeverity(rs.getString("severity"));
        long cooldown = rs.getLong("cooldown_ms");
        r.setCooldownMs(rs.wasNull() ? null : cooldown);
        r.setEnabled(rs.getBoolean("enabled"));
        Timestamp ct = rs.getTimestamp("create_time");
        r.setCreateTime(ct == null ? null : ct.toLocalDateTime());
        Timestamp ut = rs.getTimestamp("update_time");
        r.setUpdateTime(ut == null ? null : ut.toLocalDateTime());
        return r;
    };

    /** pgJdbcTemplate 是否可用 */
    public boolean isAvailable() {
        return pgJdbcTemplate != null;
    }

    /** 查询全部规则，按 id 排序 */
    public List<AlertRule> findAll() {
        if (!isAvailable()) {
            return List.of();
        }
        try {
            return pgJdbcTemplate.query("SELECT * FROM alert_rule ORDER BY id", ROW_MAPPER);
        } catch (Exception e) {
            log.warn("alert_rule findAll failed: {}", e.getMessage());
            return List.of();
        }
    }

    /** 按 rule_id 查询单条 */
    public Optional<AlertRule> findByRuleId(String ruleId) {
        if (!isAvailable()) {
            return Optional.empty();
        }
        try {
            List<AlertRule> list = pgJdbcTemplate.query(
                    "SELECT * FROM alert_rule WHERE rule_id = ?", ROW_MAPPER, ruleId);
            return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
        } catch (Exception e) {
            log.warn("alert_rule findByRuleId failed ({}): {}", ruleId, e.getMessage());
            return Optional.empty();
        }
    }

    /** 表中规则数量（用于判断是否需要初始化默认规则） */
    public int count() {
        if (!isAvailable()) {
            return 0;
        }
        try {
            Integer c = pgJdbcTemplate.queryForObject("SELECT COUNT(*) FROM alert_rule", Integer.class);
            return c == null ? 0 : c;
        } catch (Exception e) {
            log.warn("alert_rule count failed: {}", e.getMessage());
            return 0;
        }
    }

    /** 插入一条规则（create_time / update_time 由数据库 NOW() 填充） */
    public void insert(AlertRule r) {
        if (!isAvailable()) {
            return;
        }
        String sql = "INSERT INTO alert_rule (rule_id, rule_name, device_id, metric, operator, " +
                "threshold, logic, action, severity, cooldown_ms, enabled, create_time, update_time) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())";
        try {
            pgJdbcTemplate.update(sql,
                    r.getRuleId(),
                    r.getRuleName(),
                    r.getDeviceId(),
                    r.getMetric(),
                    r.getOperator(),
                    r.getThreshold(),
                    r.getLogic(),
                    r.getAction(),
                    r.getSeverity(),
                    r.getCooldownMs() == null ? 0L : r.getCooldownMs(),
                    r.getEnabled() == null ? Boolean.TRUE : r.getEnabled());
        } catch (Exception e) {
            log.warn("alert_rule insert failed ({}): {}", r.getRuleId(), e.getMessage());
        }
    }

    /** 更新启用状态 */
    public int updateEnabled(String ruleId, boolean enabled) {
        if (!isAvailable()) {
            return 0;
        }
        try {
            return pgJdbcTemplate.update(
                    "UPDATE alert_rule SET enabled = ?, update_time = NOW() WHERE rule_id = ?",
                    enabled, ruleId);
        } catch (Exception e) {
            log.warn("alert_rule updateEnabled failed ({}): {}", ruleId, e.getMessage());
            return 0;
        }
    }

    /** 按 rule_id 删除 */
    public int deleteByRuleId(String ruleId) {
        if (!isAvailable()) {
            return 0;
        }
        try {
            return pgJdbcTemplate.update("DELETE FROM alert_rule WHERE rule_id = ?", ruleId);
        } catch (Exception e) {
            log.warn("alert_rule deleteByRuleId failed ({}): {}", ruleId, e.getMessage());
            return 0;
        }
    }
}
