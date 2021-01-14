FROM openjdk:11

COPY docker/entrypoint.sh .

# requirements
COPY target/agh-pp-simulator-*.jar app.jar
COPY config/config.json config/config.json

# HTTP services
EXPOSE 80
# JDWP debugging
EXPOSE 8080

ENV JMX_PORT=9010
ENV JMX_HOST=localhost

ENTRYPOINT ["./entrypoint.sh"]
