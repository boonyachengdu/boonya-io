package com.boonya.lab.io.common.constant;

public class CommonConstants {

    public static final String UTF_8 = "UTF-8";

    public static final String APPLICATION_JSON = "application/json";

    public static final String AUTHORIZATION_HEADER = "Authorization";

    public static final String BEARER_PREFIX = "Bearer ";

    public static final String DEFAULT_PAGE_NUM = "1";

    public static final String DEFAULT_PAGE_SIZE = "10";

    public static final int MAX_PAGE_SIZE = 1000;

    public static class DeviceStatus {
        public static final String ONLINE = "online";
        public static final String OFFLINE = "offline";
        public static final String INACTIVE = "inactive";
        public static final String DISABLED = "disabled";
    }

    public static class MqttTopics {
        public static final String DEVICE_TELEMETRY = "device/+/telemetry";
        public static final String DEVICE_EVENT = "device/+/event";
        public static final String DEVICE_COMMAND = "device/{deviceId}/command";
        public static final String DEVICE_RESPONSE = "device/{deviceId}/response";
    }

    public static class RedisKeys {
        public static final String DEVICE_STATUS = "device:status:";
        public static final String DEVICE_INFO = "device:info:";
        public static final String DEVICE_TOKEN = "device:token:";
    }
}
