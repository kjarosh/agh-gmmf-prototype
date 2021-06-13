FROM openjdk:11

RUN apt-get update && \
    apt-get install -y redis && \
    apt-get clean

RUN wget https://github.com/patric-r/jvmtop/releases/download/0.8.0/jvmtop-0.8.0.tar.gz && \
    mkdir -p /jvmtop && \
    tar xvf jvmtop-0.8.0.tar.gz -C /jvmtop && \
    rm -rf jvmtop-0.8.0.tar.gz

COPY docker/ /

# HTTP services + JDWP debugging
EXPOSE 80 8080

ENV JMX_PORT=9010 \
    JMX_HOST=localhost \
    REDIS=redisson

COPY target/agh-pp-simulator-*.jar app.jar

ENTRYPOINT ["./entrypoint.sh"]
