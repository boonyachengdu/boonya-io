package com.boonya.lab.io.iot.event;

import lombok.Getter;

@Getter
public class OverTempEvent {
    private final String deviceId;
    private final double temp;
    private final long timestamp;

    public OverTempEvent(String deviceId, double temp, long timestamp) {
        this.deviceId = deviceId;
        this.temp = temp;
        this.timestamp = timestamp;
    }
}
