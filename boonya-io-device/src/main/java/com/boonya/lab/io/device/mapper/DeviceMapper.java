package com.boonya.lab.io.device.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.boonya.lab.io.device.entity.Device;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DeviceMapper extends BaseMapper<Device> {
}
