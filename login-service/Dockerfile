FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} /app/login-service.jar

EXPOSE 8083

ENTRYPOINT ["java", "-jar", "/app/login-service.jar"]
