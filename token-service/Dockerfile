FROM alpine/java:21

LABEL maintainer="himalj@dtechlk.com" authors="himal_j"

ARG JAR_FILE=target/*.jar

ADD ${JAR_FILE} token_service.jar

ENTRYPOINT ["java" ,"-jar","/token_service.jar"]

EXPOSE 8082