FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY target/boonya-io-iot-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]

# 1.构建命令： docker build -t boonya-io-iot .
# 2.清楚缓存：docker-compose build --no-cache
# 3.启动命令：docker-compose up -d