FROM openjdk:11

RUN apt-get update && \
    apt-get install -y redis && \
    apt-get clean

COPY docker/ /

# HTTP services
EXPOSE 80
# JDWP debugging
EXPOSE 8080

ENV JMX_PORT=9010
ENV JMX_HOST=localhost
ENV REDIS=redisson

COPY config/config.json config/config.json
COPY target/agh-pp-simulator-*.jar app.jar

ENTRYPOINT ["./entrypoint.sh"]
