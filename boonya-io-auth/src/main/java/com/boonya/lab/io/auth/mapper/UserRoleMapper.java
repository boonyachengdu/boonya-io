package com.boonya.lab.io.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.boonya.lab.io.auth.entity.UserRole;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface UserRoleMapper extends BaseMapper<UserRole> {

    /**
     * 查询用户关联的角色ID列表
     */
    @Select("SELECT role_id FROM sys_user_role WHERE user_id = #{userId}")
    List<Long> selectRoleIdsByUserId(Long userId);

    /**
     * 查询用户关联的角色编码列表（JOIN sys_role）
     */
    @Select("SELECT r.role_code FROM sys_user_role ur " +
            "JOIN sys_role r ON ur.role_id = r.id " +
            "WHERE ur.user_id = #{userId} AND r.deleted = 0")
    List<String> selectRoleCodesByUserId(Long userId);
}
