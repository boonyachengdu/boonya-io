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

    @GetMapping
    @Operation(summary = "获取所有规则", description = "获取所有已注册的规则")
    public Result<List<Rule>> getAllRules() {
        // 这里需要 RuleEngine 提供获取所有规则的方法
        return Result.success(List.of());
    }

    @PutMapping("/{ruleId}/enable")
    @Operation(summary = "启用规则", description = "启用指定的规则")
    public Result<Void> enableRule(@PathVariable String ruleId) {
        // TODO: 实现启用规则逻辑
        return Result.success("规则已启用", null);
    }

    @PutMapping("/{ruleId}/disable")
    @Operation(summary = "禁用规则", description = "禁用指定的规则")
    public Result<Void> disableRule(@PathVariable String ruleId) {
        // TODO: 实现禁用规则逻辑
        return Result.success("规则已禁用", null);
    }

    @DeleteMapping("/{ruleId}")
    @Operation(summary = "删除规则", description = "删除指定的规则")
    public Result<Void> deleteRule(@PathVariable String ruleId) {
        // TODO: 实现删除规则逻辑
        return Result.success("规则已删除", null);
    }
}
