#!/bin/bash

java \
    -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:8080 \
    -Dapp.config_path=config/config.json \
    -Dcom.sun.management.jmxremote=true \
    -Dcom.sun.management.jmxremote.host=0.0.0.0 \
    -Dcom.sun.management.jmxremote.port=$JMX_PORT \
    -Dcom.sun.management.jmxremote.rmi.port=$JMX_PORT \
    -Dcom.sun.management.jmxremote.local.only=false \
    -Dcom.sun.management.jmxremote.authenticate=false \
    -Dcom.sun.management.jmxremote.ssl=false \
    -Djava.rmi.server.hostname=$JMX_HOST \
    -Xmx2G \
    -jar \
    app.jar "$@"
