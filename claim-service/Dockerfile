FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} /app/claim-service.jar

EXPOSE 8087

ENTRYPOINT ["java", "-jar", "/app/claim-service.jar"]
