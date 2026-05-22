# Boonya IoT - OTA 升级模块

## 📖 模块简介

**boonya-io-ota** 是 Boonya IoT 平台的 OTA（Over-The-Air）固件升级管理模块，提供固件上传、版本管理、升级任务调度等功能。

### 核心功能

1. **固件管理**
   - 固件上传到 MinIO 对象存储
   - 自动计算 MD5 校验值
   - 版本控制（同一设备型号不能重复版本）
   - 状态管理：草稿 → 已发布 → 已归档
   - 支持强制升级标记

2. **任务管理**
   - 为指定设备创建升级任务
   - 任务状态跟踪：pending → downloading → installing → success/failed
   - 进度上报（0-100%）
   - 并发控制（同一设备不能有多个进行中的任务）
   - 任务取消

---

## 🚀 快速开始

### 1. 配置依赖

确保已启动 MinIO 和 PostgreSQL：

```bash
docker-compose up -d minio postgres
```

### 2. 初始化数据库

```bash
psql -U postgres -h localhost -f src/main/resources/schema.sql
```

### 3. 修改配置

编辑 `application.yml`：

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/iot_ota
    username: postgres
    password: postgres

minio:
  endpoint: http://localhost:9000
  access-key: minioadmin
  secret-key: minioadmin
  bucket-name: ota-firmware
```

### 4. 启动服务

```bash
mvn spring-boot:run
```

访问 Swagger：http://localhost:8085/swagger-ui.html

---

## 📡 API 接口

### 固件管理

#### 1. 上传固件
```http
POST /api/firmware
Content-Type: multipart/form-data

deviceModel: sensor-v1
version: v1.0.0
description: 修复温度传感器精度问题
forceUpdate: false
file: <firmware.bin>
```

**响应示例**：
```json
{
  "code": 200,
  "message": "固件上传成功",
  "data": {
    "id": 1,
    "deviceModel": "sensor-v1",
    "version": "v1.0.0",
    "status": "draft",
    "fileName": "firmware.bin",
    "fileSize": 1048576,
    "md5Checksum": "d41d8cd98f00b204e9800998ecf8427e"
  }
}
```

#### 2. 获取固件列表
```http
GET /api/firmware?deviceModel=sensor-v1&status=published
```

#### 3. 发布固件
```http
POST /api/firmware/{id}/publish
```

#### 4. 归档固件
```http
POST /api/firmware/{id}/archive
```

#### 5. 删除固件
```http
DELETE /api/firmware/{id}
```

> 注意：只能删除草稿状态的固件

---

### OTA 任务管理

#### 1. 创建任务
```http
POST /api/ota/tasks?deviceId=device_001&firmwareId=1
```

**响应示例**：
```json
{
  "code": 200,
  "message": "OTA任务创建成功",
  "data": {
    "id": 1,
    "deviceId": "device_001",
    "firmwareId": 1,
    "status": "pending",
    "progress": 0
  }
}
```

#### 2. 获取任务详情
```http
GET /api/ota/tasks/{id}
```

#### 3. 获取设备任务列表
```http
GET /api/ota/tasks/device/{deviceId}
```

#### 4. 更新任务状态（设备端调用）
```http
PUT /api/ota/tasks/{id}/status
Content-Type: application/json

{
  "status": "downloading",
  "progress": 50
}
```

**状态流转**：
```
pending → downloading → installing → success
                                    → failed
         → cancelled
```

#### 5. 取消任务
```http
POST /api/ota/tasks/{id}/cancel
```

---

## 🔧 技术栈

- **Spring Boot 3.3.5** + **Java 17**
- **Spring Data JPA**：数据持久化
- **PostgreSQL**：关系型数据库
- **MinIO**：对象存储（固件文件）
- **Swagger/Knife4j**：API 文档
- **Lombok**：简化代码

---

## 🗄️ 数据库设计

### firmware 表

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGSERIAL | 主键 |
| device_model | VARCHAR(64) | 设备型号 |
| version | VARCHAR(32) | 版本号 |
| description | TEXT | 更新说明 |
| file_path | VARCHAR(512) | MinIO 路径 |
| file_name | VARCHAR(256) | 文件名 |
| file_size | BIGINT | 文件大小（字节） |
| md5_checksum | VARCHAR(64) | MD5 校验值 |
| status | VARCHAR(16) | 状态（draft/published/archived） |
| force_update | BOOLEAN | 是否强制升级 |
| create_time | TIMESTAMP | 创建时间 |
| update_time | TIMESTAMP | 更新时间 |
| publish_time | TIMESTAMP | 发布时间 |

**约束**：
- UNIQUE (device_model, version)：同一设备型号不能有重复版本

### ota_task 表

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGSERIAL | 主键 |
| device_id | VARCHAR(64) | 设备ID |
| firmware_id | BIGINT | 固件ID（外键） |
| status | VARCHAR(32) | 任务状态 |
| error_message | TEXT | 失败原因 |
| progress | INTEGER | 下载进度（0-100） |
| start_time | TIMESTAMP | 开始时间 |
| complete_time | TIMESTAMP | 完成时间 |
| create_time | TIMESTAMP | 创建时间 |
| update_time | TIMESTAMP | 更新时间 |

---

## 💡 使用场景

### 场景 1：批量设备升级

1. 上传新固件并标记为强制升级
2. 遍历所有在线设备，创建升级任务
3. 监控任务进度，处理失败任务

### 场景 2：灰度发布

1. 先为少量设备创建升级任务
2. 观察运行状态，确认无问题
3. 逐步扩大升级范围

### 场景 3：紧急修复

1. 上传紧急修复固件
2. 标记为强制升级
3. 立即为所有设备创建任务

---

## 🔐 安全考虑

1. **固件完整性**：自动计算 MD5 校验值
2. **版本控制**：防止重复版本覆盖
3. **状态保护**：已发布的固件不能删除
4. **并发控制**：防止设备同时执行多个升级任务

---

## 📝 注意事项

1. **MinIO Bucket**：首次启动前需创建 `ota-firmware` bucket
2. **文件大小**：建议单个固件不超过 100MB
3. **网络带宽**：大量设备同时升级时注意带宽限制
4. **回滚机制**：当前版本暂不支持自动回滚，需在设备端实现

---

## 🎯 后续优化方向

1. 支持断点续传
2. 添加固件签名验证
3. 实现灰度发布策略
4. 支持定时升级任务
5. 添加升级成功率统计
