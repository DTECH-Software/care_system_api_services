FROM alpine/java:21
LABEL maintainer="himalj@dtechlk.com" authors="himal_j"
EXPOSE 8083
ARG JAR_FILE=target/login-service-v1.0.0.jar
ADD ${JAR_FILE} login_service.jar
ENTRYPOINT ["java","-jar","/login_service.jar"]