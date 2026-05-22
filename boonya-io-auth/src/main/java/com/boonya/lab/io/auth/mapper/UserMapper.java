package com.boonya.lab.io.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.boonya.lab.io.auth.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}
