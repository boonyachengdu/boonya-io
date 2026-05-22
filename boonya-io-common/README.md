# Boonya IO Common Module

公共模块，提供 IoT 平台各模块共用的工具类、异常类和常量。

## 模块结构

```
com.boonya.lab.io.common
├── config/              # 配置类
│   └── GlobalExceptionHandler.java    # 全局异常处理器
├── constant/            # 常量定义
│   └── CommonConstants.java           # 通用常量
├── exception/           # 异常类
│   ├── BusinessException.java         # 业务异常
│   ├── ResourceNotFoundException.java # 资源未找到异常
│   └── ValidationException.java       # 验证异常
├── response/            # 响应封装
│   ├── Result.java                    # 统一响应结果
│   └── PageResult.java                # 分页响应结果
└── util/                # 工具类
    ├── JsonUtils.java                 # JSON 工具
    └── TokenUtils.java                # Token 生成工具
```

## 使用方式

### 1. 添加依赖

在其他模块的 `pom.xml` 中添加：

```xml
<dependency>
    <groupId>com.boonya.lab.io</groupId>
    <artifactId>boonya-io-common</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```

### 2. 使用统一响应

```java
@RestController
@RequestMapping("/api/devices")
public class DeviceController {

    @GetMapping("/{id}")
    public Result<DeviceResponse> getDevice(@PathVariable Long id) {
        DeviceResponse device = deviceService.getDevice(id);
        return Result.success(device);
    }

    @PostMapping
    public Result<DeviceResponse> createDevice(@RequestBody DeviceRequest request) {
        DeviceResponse device = deviceService.create(request);
        return Result.success("Device created successfully", device);
    }
}
```

### 3. 使用异常类

```java
@Service
public class DeviceService {

    public DeviceResponse getDevice(Long id) {
        Device device = deviceMapper.selectById(id);
        if (device == null) {
            throw new ResourceNotFoundException("Device", String.valueOf(id));
        }
        return convertToResponse(device);
    }

    public void validateDevice(DeviceRequest request) {
        if (!isValidDeviceId(request.getDeviceId())) {
            throw new ValidationException("deviceId", "Invalid device ID format");
        }
    }
}
```

### 4. 使用常量

```java
import static com.boonya.lab.io.common.constant.CommonConstants.*;

public class DeviceService {

    public void updateStatus(String deviceId, String status) {
        // 使用设备状态常量
        if (DeviceStatus.ONLINE.equals(status)) {
            // 处理在线状态
        }
        
        // 使用 Redis key 常量
        String redisKey = RedisKeys.DEVICE_STATUS + deviceId;
        redisTemplate.opsForValue().set(redisKey, status);
    }
}
```

### 5. 使用工具类

```java
// JSON 工具
String json = JsonUtils.toJson(device);
Device device = JsonUtils.fromJson(json, Device.class);

// Token 生成工具
String token = TokenUtils.generateDeviceToken();
```

## 响应格式

### 成功响应

```json
{
  "code": 200,
  "message": "success",
  "data": { ... },
  "timestamp": 1234567890
}
```

### 错误响应

```json
{
  "code": 400,
  "message": "Validation failed for field 'deviceId': Invalid format",
  "data": null,
  "timestamp": 1234567890
}
```

### 分页响应

```json
{
  "current": 1,
  "size": 10,
  "total": 100,
  "pages": 10,
  "records": [ ... ]
}
```

## 扩展说明

如需添加新的公共功能，请遵循以下原则：

1. **工具类**：放在 `util` 包下，使用静态方法
2. **异常类**：继承 `BusinessException`，定义明确的错误码和 HTTP 状态
3. **常量类**：使用 `public static final` 或内部静态类组织
4. **响应类**：保持泛型设计，支持各种数据类型
