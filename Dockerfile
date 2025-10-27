FROM openjdk:17-slim
VOLUME /tmp
COPY target/miniproject3-backend.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar","--spring.profiles.active=prod"]