package com.boonya.lab.io.iot.controller;

import com.boonya.lab.io.common.response.Result;
import com.boonya.lab.io.iot.ruleengine.Rule;
import com.boonya.lab.io.iot.ruleengine.RuleEngine;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rules")
@RequiredArgsConstructor
@Tag(name = "规则管理", description = "规则引擎管理接口")
public class RuleController {

    private final RuleEngine ruleEngine;

    @PostMapping
    @Operation(summary = "创建规则", description = "创建新的处理规则")
    public Result<Void> createRule(@RequestBody Rule rule) {
        ruleEngine.registerRule(rule);
        return Result.success("规则创建成功", null);
    }

    // 修改内容：修改人：pengjunlin 时间：2026-08-04 18:10:00 -- start ----
    @GetMapping
    @Operation(summary = "获取所有规则", description = "获取所有已注册的规则")
    public Result<List<Rule>> getAllRules() {
        return Result.success(ruleEngine.getAllRules());
    }

    @PutMapping("/{ruleId}/enable")
    @Operation(summary = "启用规则", description = "启用指定的规则")
    public Result<Void> enableRule(@PathVariable String ruleId) {
        boolean success = ruleEngine.enableRule(ruleId);
        if (success) {
            return Result.success("规则已启用", null);
        }
        return Result.error(404, "规则不存在: " + ruleId);
    }

    @PutMapping("/{ruleId}/disable")
    @Operation(summary = "禁用规则", description = "禁用指定的规则")
    public Result<Void> disableRule(@PathVariable String ruleId) {
        boolean success = ruleEngine.disableRule(ruleId);
        if (success) {
            return Result.success("规则已禁用", null);
        }
        return Result.error(404, "规则不存在: " + ruleId);
    }

    @DeleteMapping("/{ruleId}")
    @Operation(summary = "删除规则", description = "删除指定的规则")
    public Result<Void> deleteRule(@PathVariable String ruleId) {
        boolean success = ruleEngine.deleteRule(ruleId);
        if (success) {
            return Result.success("规则已删除", null);
        }
        return Result.error(404, "规则不存在: " + ruleId);
    }
    // 修改内容：修改人：pengjunlin 时间：2026-08-04 18:10:00 -- end ----
}
