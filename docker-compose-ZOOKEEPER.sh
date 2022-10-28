#!/bin/bash

#  -Dspring.profiles.active=deps <-- remove this profile
SYSTEM_PROPS="-DRABBIT_HOST=${HEALTH_HOST} -Dspring.rabbitmq.port=5672"

dockerComposeFile="docker-compose-${WHAT_TO_TEST}.yml"
kill_docker
docker-compose -f $dockerComposeFile kill
docker-compose -f $dockerComposeFile pull

if [[ "${SHOULD_START_RABBIT}" == "yes" ]] ; then
    echo -e "\n\nBooting up RabbitMQ"
    docker-compose -f $dockerComposeFile up -d rabbitmq
fi

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
docker-compose -f $dockerComposeFile up -d zookeeper
netcat_local_port $PORT_TO_CHECK && READY_FOR_TESTS="yes"

if [[ "${READY_FOR_TESTS}" == "no" ]] ; then
    echo "Zookeeper failed to start..."
    exit 1
fi

READY_FOR_TESTS="no"
PORT_TO_CHECK=3100
echo "Run the rest of infra"
docker-compose -f $dockerComposeFile  up -d zipkin loki prometheus grafana
netcat_local_port $PORT_TO_CHECK && READY_FOR_TESTS="yes"

if [[ "${READY_FOR_TESTS}" == "no" ]] ; then
    echo "Loki failed to start..."
    exit 1
fi


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
