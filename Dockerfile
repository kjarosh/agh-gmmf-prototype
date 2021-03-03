FROM openjdk:11

RUN apt-get update && \
    apt-get install -y redis && \
    apt-get clean

COPY docker/ /

# HTTP services + JDWP debugging
EXPOSE 80 8080

ENV JMX_PORT=9010 \
    JMX_HOST=localhost \
    REDIS=redisson

COPY target/agh-pp-simulator-*.jar app.jar

ENTRYPOINT ["./entrypoint.sh"]
