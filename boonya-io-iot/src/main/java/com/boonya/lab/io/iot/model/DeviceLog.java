package com.boonya.lab.io.iot.model;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class DeviceLog {
    private Long id;
    private String deviceId;
    private String fileUrl;
    private String fileName;
    private LocalDateTime uploadTime;
}
