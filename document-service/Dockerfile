FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} /app/document-service.jar

EXPOSE 8086

ENTRYPOINT ["java", "-jar", "/app/document-service.jar"]
