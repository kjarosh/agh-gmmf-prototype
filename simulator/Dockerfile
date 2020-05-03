FROM openjdk:11

# requirements
COPY target/agh-pp-simulator-1.0-SNAPSHOT.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:8080", "-jar", "app.jar"]
