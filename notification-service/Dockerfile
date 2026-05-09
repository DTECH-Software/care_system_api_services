FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} /app/notification-service.jar

EXPOSE 8088

ENTRYPOINT ["java", "-jar", "/app/notification-service.jar"]
