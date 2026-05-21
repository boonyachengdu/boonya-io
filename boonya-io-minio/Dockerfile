FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY target/boonya-io-minio-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 28080
ENTRYPOINT ["java", "-jar", "app.jar"]

# 1.构建命令： docker build -t boonya-io-minio .
# 2.启动命令：docker-compose up -d