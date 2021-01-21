#191.13MB jdk-alpine
FROM openjdk:12-jdk-alpine
EXPOSE 8080
COPY ./target/user-service.jar app/user-service.jar
ENTRYPOINT ["java", "-jar", "/app/user-service.jar"]