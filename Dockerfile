FROM eclipse-temurin:17.0.9_9-jre-alpine
VOLUME /tmp
ARG JAR_FILE
COPY build/libs/pulceo-service-manager-0.0.1-SNAPSHOT.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]