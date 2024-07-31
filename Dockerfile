FROM openjdk:22-jdk-slim
VOLUME /tmp
COPY target/your-application.jar app.jar
COPY classes/application.properties application.properties
COPY classes/token.properties token.properties
COPY classes/credentials.json credentials.json
ENTRYPOINT ["java", "-jar", "/app.jar"]
