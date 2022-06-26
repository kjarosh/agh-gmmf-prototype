FROM openjdk:11
ENV DEBIAN_FRONTEND=noninteractive

RUN apt-get update && \
    apt-get install -y redis xauth xorg openssh-server && \
    apt-get clean

RUN wget https://github.com/oracle/visualvm/releases/download/2.0.7/visualvm_207.zip && \
    mkdir visualvm && \
    unzip visualvm_207.zip -d visualvm && \
    rm -rf visualvm_207.zip

COPY docker/ /

# HTTP services + JDWP debugging
EXPOSE 80 8080

ENV JMX_PORT=9010 \
    JMX_HOST=localhost \
    REDIS=redisson

COPY target/gmmf-zone-simulator-*.jar app.jar

ENTRYPOINT ["./entrypoint.sh"]
