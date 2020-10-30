FROM openjdk:11

# requirements
COPY target/agh-pp-simulator-*.jar app.jar
COPY config/config.json config.json

# HTTP services
EXPOSE 80
# JDWP debugging
EXPOSE 8080

ENTRYPOINT java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:8080 \
    -Dapp.zone_id=${ZONE_ID} \
    -Dapp.config_path=config.json \
    -jar app.jar
