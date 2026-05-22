FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY target/boonya-io-device-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8086
ENTRYPOINT ["java", "-jar", "app.jar"]
