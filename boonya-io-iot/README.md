
# 快速上手指南
1. 第一天：Docker启动EMQX + TDengine，用MQTT.fx（桌面工具）模拟设备发送数据
2. 第二天：写Spring Boot订阅MQTT，存储到TDengine，查询验证
3. 第三天：加Redis缓存 + WebSocket推送到前端页面
4. 第四天：集成MinIO，实现设备日志上传
5. 第五天：加一个简单的可视化仪表板（ECharts）

整个系统落地后，你就能理解物联网平台的核心流：设备 → MQTT → 规则引擎 → 时序DB + 缓存 + 告警 → 应用展示
# 核心流程设计

```
设备模拟器 (DeviceSimulator)
    ↓ 发布 MQTT 消息
MQTT Broker (Moquette/EMQX)
    ↓ 订阅 topic: device/+/telemetry
MqttSubscriber (后端服务)
    ↓ 解析 JSON，判断温度 > 30℃
ApplicationEventPublisher (Spring 事件)
    ↓ 发布 OverTempEvent
AlertHandler (事件监听器)
    ↓ 通过 WebSocket 推送
SimpMessagingTemplate
    ↓ 发送到 /topic/alerts
前端页面 (index.html)
    ↓ STOMP 客户端接收
实时显示告警信息
```

# 运行minio模块

## 构建镜像

```
docker build -t boonya-io-iot .
```
## 卸载和构建
```
docker-compose down
docker-compose build --no-cache
docker-compose up -d
```

# 测试和生产
两种方式可选

* 测试阶段：嵌入式 Moquette Broker（用 EmbeddedMqttBroker 类）
* 生产阶段：外部 EMQX Broker（用 MqttBroker + MqttClient）

```bash
docker run -d --name emqx -p 1883:1883 -p 18083:18083 emqx/emqx:latest
```

# 方式一：启动所有服务
```bash
# 构建并启动所有服务
docker-compose up -d

# 查看启动日志
docker-compose logs -f

# 只查看 Spring Boot 应用日志
docker-compose logs -f spring-app

```

# 方式二：仅启动基础设施

```bash
# 启动基础服务（EMQX, TDengine, Redis, MinIO）
docker-compose up -d emqx tdengine redis minio

# 查看服务状态
docker-compose ps
```

# 初始化Tdengine 时序数据库

powershell

```bash
Invoke-WebRequest -Uri "http://localhost:6041/rest/sql" -Method POST -Body "CREATE DATABASE IF NOT EXISTS iot" -Headers @{Authorization="Basic cm9vdDp0YW9zZGF0YQ=="}
```

# 访问地址
服务启动后，可以通过以下地址访问：

```
Spring Boot 应用	http://localhost:8080	API 接口
EMQX Dashboard	http://localhost:18083	MQTT 管理后台（admin/public）
MQTT Broker	tcp://localhost:1883	MQTT 连接地址
MinIO Console	http://localhost:9001	对象存储管理（minioadmin/minioadmin）
MinIO API	http://localhost:9000	对象存储 API
TDengine	jdbc:TAOS://localhost:6030/iot	时序数据库
Redis	localhost:6379	缓存数据库
```

# 测试MQTT连接

```
# 使用 curl 测试 WebSocket
curl http://localhost:8080/device/list

# 或使用 MQTT 客户端工具（如 MQTT.fx、MQTT Explorer）
# 连接地址: tcp://localhost:1883
# 订阅主题: device/+/telemetry

```
# 查看设备告警

浏览器打开：http://localhost:8080/ws（WebSocket 实时告警页面）