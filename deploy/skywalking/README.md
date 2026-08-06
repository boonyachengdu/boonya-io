# SkyWalking 链路追踪集成说明

本文档说明如何在 boonya-io 项目中集成 Apache SkyWalking Java Agent，实现对微服务调用的全链路追踪。

## 一、架构说明

```
微服务(JVM + skywalking-agent.jar) --> SkyWalking OAP(11800) --> Elasticsearch(9200)
                                          |
                                          v
                                    SkyWalking UI(12800/8088)
```

- **SkyWalking OAP Server**：接收 Agent 上报的链路数据并聚合分析，端口 `11800`(gRPC) / `12800`(HTTP)。
- **Elasticsearch**：OAP 的后端存储。
- **SkyWalking UI**：可视化界面，对外端口 `8088`。
- **Java Agent**：以 `-javaagent` 方式挂载到各微服务 JVM，无需修改业务代码。

## 二、基础设施启动

`docker-compose.yml` 已包含 `skywalking-oap`、`skywalking-ui`、`elasticsearch` 三个服务：

```bash
docker compose up -d elasticsearch skywalking-oap skywalking-ui
```

启动后访问 UI：http://localhost:8088

## 三、Java Agent 接入步骤

### 1. 下载 Agent

从 Apache 官方仓库下载对应版本的 Java Agent 压缩包（建议与 OAP 版本一致，本项目使用 9.7.0）：

```bash
# 下载地址示例（请替换为实际版本号）
wget https://archive.apache.org/dist/skywalking/java-agent/9.3.0/apache-skywalking-java-agent-9.3.0.tgz
tar -xzf apache-skywalking-java-agent-9.3.0.tgz
```

解压后得到 `skywalking-agent` 目录，其中核心文件为 `skywalking-agent.jar`。

### 2. 放置 Agent

将解压后的目录放置到固定路径，例如：

```bash
mv skywalking-agent /opt/skywalking-agent/
```

最终 Agent jar 路径为：`/opt/skywalking-agent/skywalking-agent.jar`

### 3. 配置 Agent 参数

在 `skywalking-agent/config/agent.config` 中（或通过项目内的 `src/main/resources/skywalking-agent.config` 覆盖）配置以下关键项：

```properties
# 服务名称（按各微服务修改，如 boonya-io-iot / boonya-io-device 等）
agent.service_name=boonya-io-iot
# OAP 后端地址（gRPC 端口 11800）
agent.collector.backend_service=oap:11800
# 每 3 秒采样链路数
agent.sample_n_per_3_secs=100
```

> 说明：`oap` 为 docker-compose 中 SkyWalking OAP 服务名，容器间通过 `iot-network` 网络互通。

### 4. 在 Dockerfile 中挂载 Agent

在各微服务的 `Dockerfile` 中，通过 `ENTRYPOINT` 或 `JAVA_OPTS` 添加 `-javaagent` 参数。示例：

```dockerfile
FROM eclipse-temurin:17-jre

WORKDIR /app
COPY target/*.jar app.jar
COPY skywalking-agent /opt/skywalking-agent

ENV JAVA_OPTS="-javaagent:/opt/skywalking-agent/skywalking-agent.jar"
ENV SW_AGENT_NAME=boonya-io-iot
ENV SW_AGENT_COLLECTOR_BACKEND_SERVICES=oap:11800

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
```

### 5. 关键配置项说明

| 配置项 | 说明 | 示例值 |
|--------|------|--------|
| `agent.service_name` | 当前服务在 UI 中展示的名称 | `boonya-io-iot` |
| `agent.collector.backend_service` | OAP gRPC 地址 | `oap:11800` |
| `agent.sample_n_per_3_secs` | 每 3 秒采样数，-1 表示全量 | `100` |
| `agent.ignore_suffix` | 忽略的后缀（如本地调用） | `.jpg,.png` |

## 四、验证

1. 启动微服务后，发起若干 HTTP / MQTT 请求。
2. 打开 http://localhost:8088 ，在 **Trace** / **Topology** 页面可看到链路调用拓扑与耗时详情。
3. 若未出现数据，检查：
   - Agent 是否成功挂载（启动日志含 `SkyWalking agent initialized`）。
   - OAP 是否可达（`telnet oap 11800`）。
   - Elasticsearch 是否正常运行。
