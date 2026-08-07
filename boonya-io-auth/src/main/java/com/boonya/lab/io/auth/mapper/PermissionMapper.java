package com.boonya.lab.io.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.boonya.lab.io.auth.entity.Permission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface PermissionMapper extends BaseMapper<Permission> {

    /**
     * 查询角色关联的权限列表（JOIN sys_role_permission）
     */
    @Select({
            "<script>",
            "SELECT p.* FROM sys_permission p ",
            "JOIN sys_role_permission rp ON p.id = rp.permission_id ",
            "WHERE rp.role_id IN ",
            "<foreach item='id' collection='roleIds' open='(' separator=',' close=')'>#{id}</foreach>",
            "ORDER BY p.sort ASC",
            "</script>"
    })
    List<Permission> selectPermissionsByRoleIds(List<Long> roleIds);

    /**
     * 查询用户权限编码列表（user→role→permission 三表关联）
     */
    @Select("SELECT DISTINCT p.code FROM sys_user_role ur " +
            "JOIN sys_role_permission rp ON ur.role_id = rp.role_id " +
            "JOIN sys_permission p ON rp.permission_id = p.id " +
            "WHERE ur.user_id = #{userId} AND p.enabled = 1")
    List<String> selectPermissionCodesByUserId(Long userId);
}
