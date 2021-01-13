FROM openjdk:11

# requirements
COPY target/agh-pp-simulator-*.jar app.jar
COPY config/config.json config/config.json

# HTTP services
EXPOSE 80
# JDWP debugging
EXPOSE 8080

ENTRYPOINT ["java", \
    "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:8080", \
    "-Dapp.config_path=config/config.json", \
    "-Xmx2G", \
    "-jar", \
    "app.jar"]
