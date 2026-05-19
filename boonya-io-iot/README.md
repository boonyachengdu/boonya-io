
# 快速上手指南
1. 第一天：Docker启动EMQX + TDengine，用MQTT.fx（桌面工具）模拟设备发送数据
2. 第二天：写Spring Boot订阅MQTT，存储到TDengine，查询验证
3. 第三天：加Redis缓存 + WebSocket推送到前端页面
4. 第四天：集成MinIO，实现设备日志上传
5. 第五天：加一个简单的可视化仪表板（ECharts）

整个系统落地后，你就能理解物联网平台的核心流：设备 → MQTT → 规则引擎 → 时序DB + 缓存 + 告警 → 应用展示

# 测试和生产
两种方式可选

* 测试阶段：嵌入式 Moquette Broker（用 EmbeddedMqttBroker 类）
* 生产阶段：外部 EMQX Broker（用 MqttBroker + MqttClient）

```bash
docker run -d --name emqx -p 1883:1883 -p 18083:18083 emqx/emqx:latest
```