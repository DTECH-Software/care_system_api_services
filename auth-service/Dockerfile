FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} /app/auth-service.jar

EXPOSE 8085

ENTRYPOINT ["java", "-jar", "/app/auth-service.jar"]
