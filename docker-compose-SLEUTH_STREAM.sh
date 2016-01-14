#!/bin/bash

SYSTEM_PROPS="-DRABBIT_HOST=${HEALTH_HOST} -Dspring.zipkin.host=${HEALTH_HOST} -Dspring.cloud.zookeeper.connectString=${HEALTH_HOST}:2181"

dockerComposeFile="docker-compose-${WHAT_TO_TEST}.yml"
docker-compose -f $dockerComposeFile kill
docker-compose -f $dockerComposeFile build

if [[ "${SHOULD_START_RABBIT}" == "yes" ]] ; then
    echo -e "\n\nBooting up RabbitMQ"
    docker-compose -f $dockerComposeFile up -d rabbitmq
fi
echo -e "\n\nBooting up Discovery"
docker-compose -f $dockerComposeFile up -d discovery

READY_FOR_TESTS="no"
PORT_TO_CHECK=5672
echo "Waiting for RabbitMQ to boot for [$(( WAIT_TIME * RETRIES ))] seconds"
netcat_port $PORT_TO_CHECK && READY_FOR_TESTS="yes"

if [[ "${READY_FOR_TESTS}" == "no" ]] ; then
    echo "RabbitMQ failed to start..."
    exit 1
fi

READY_FOR_TESTS="no"
PORT_TO_CHECK=2181
echo "Waiting for Zookeeper to boot for [$(( WAIT_TIME * RETRIES ))] seconds"
netcat_port $PORT_TO_CHECK && READY_FOR_TESTS="yes"

if [[ "${READY_FOR_TESTS}" == "no" ]] ; then
    echo "Zookeeper failed to start..."
    exit 1
fi

# Boot zipkin-server
READY_FOR_TESTS="no"
PORT_TO_CHECK=9411
echo "Waiting for the Zipkin Server app to boot for [$(( WAIT_TIME * RETRIES ))] seconds"
java_jar "zipkin-server" "${SYSTEM_PROPS} -Dzipkin.collector.sample-rate=1.0 -Dzipkin.query.lookback=86400000"
curl_local_health_endpoint $PORT_TO_CHECK  && READY_FOR_TESTS="yes"

if [[ "${READY_FOR_TESTS}" == "no" ]] ; then
    echo "Config server failed to start..."
    exit 1
fi

echo -e "\n\nBooting up Zipkin web"
docker-compose -f $dockerComposeFile up -d web

# Boot config-server
READY_FOR_TESTS="no"
PORT_TO_CHECK=8888
echo "Waiting for the Config Server app to boot for [$(( WAIT_TIME * RETRIES ))] seconds"
java_jar "config-server"
curl_local_health_endpoint $PORT_TO_CHECK  && READY_FOR_TESTS="yes"

if [[ "${READY_FOR_TESTS}" == "no" ]] ; then
    echo "Config server failed to start..."
    exit 1
fi

echo -e "\n\nStarting brewery apps..."
start_brewery_apps "$SYSTEM_PROPS"

