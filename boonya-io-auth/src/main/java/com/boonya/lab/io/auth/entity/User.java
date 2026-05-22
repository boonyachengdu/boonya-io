package com.boonya.lab.io.auth.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_user")
@Schema(description = "系统用户")
public class User {

    @TableId(type = IdType.AUTO)
    @Schema(description = "用户ID")
    private Long id;

    @Schema(description = "用户名", example = "admin")
    private String username;

    @Schema(description = "密码（加密存储）")
    private String password;

    @Schema(description = "邮箱", example = "admin@example.com")
    private String email;

    @Schema(description = "手机号", example = "13800138000")
    private String phone;

    @Schema(description = "真实姓名", example = "管理员")
    private String realName;

    @Schema(description = "用户状态：active-激活, inactive-未激活, locked-锁定")
    private String status;

    @Schema(description = "最后登录时间")
    private LocalDateTime lastLoginTime;

    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    @TableLogic
    @Schema(description = "逻辑删除：0-未删除, 1-已删除")
    private Integer deleted;
}
