FROM openjdk:17

#ARG JAR_FILE=target/*.jar
ARG JAR_FILE=target/backend-service:0.0.1-SNAPSHOT.jar

COPY ${JAR_FILE} backend-service.jar

ENTRYPOINT ["java","-jar","backend-service.jar"]

EXPOSE 8080