FROM openjdk:11

RUN apt-get update && \
    apt-get install -y redis && \
    apt-get clean

COPY docker/entrypoint.sh .

# HTTP services
EXPOSE 80
# JDWP debugging
EXPOSE 8080

ENV JMX_PORT=9010
ENV JMX_HOST=localhost
ENV REDIS=false

COPY config/config.json config/config.json
COPY target/agh-pp-simulator-*.jar app.jar

ENTRYPOINT ["./entrypoint.sh"]
