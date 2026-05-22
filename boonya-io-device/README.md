# 🚀 使用说明
启动步骤

## 1.启动 PostgreSQL 数据库
```bash
docker run -d --name postgres \
-e POSTGRES_PASSWORD=postgres \
-p 5432:5432 \
-v postgres-data:/var/lib/postgresql/data \
postgres:15
```

## 2.初始化数据库
```bash
psql -U postgres -h localhost -f src/main/resources/schema.sql

```

## 3.启动Redis
```bash
docker run -d --name redis -p 6379:6379 redis:alpine
```

## 4.运行应用
```bash
cd boonya-io-device
mvn spring-boot:run
```
## 5.访问swagger文档

http://localhost:8081/swagger-ui.html

# 🚀 测试说明

## 1. 注册设备

```bash
curl -X POST http://localhost:8081/api/devices/register \
-H "Content-Type: application/json" \
-d '{
"deviceId": "sensor_001",
"deviceName": "温度传感器1号",
"deviceType": "temperature_sensor",
"model": "TMP-100",
"location": "车间A-区域1",
"description": "用于监测车间温度"
}'
```

## 2. 设备心跳

```bash
curl -X POST http://localhost:8081/api/devices/sensor_001/heartbeat
```

## 2. 获取设备列表
```bash
curl http://localhost:8081/api/devices/query?pageNum=1&pageSize=10
```