#!/bin/bash

SYSTEM_PROPS="-Dspring.zipkin.enabled=false -Dspring.profiles.active=deps -Dspring.cloud.zookeeper.connectString=${HEALTH_HOST}:2181"
echo -e "\nSetting system props [$SYSTEM_PROPS]"

dockerComposeFile="docker-compose-${WHAT_TO_TEST}.yml"
docker-compose -f $dockerComposeFile kill
docker-compose -f $dockerComposeFile build

echo -e "\n\nBooting up RabbitMQ"
docker-compose -f $dockerComposeFile up -d rabbitmq discovery

# Boot config-server
READY_FOR_TESTS="no"
PORT_TO_CHECK=8888

echo "Waiting for the Config Server app to boot for [$(( WAIT_TIME * RETRIES ))] seconds"
java_jar "config-server"
curl_local_health_endpoint $PORT_TO_CHECK  && READY_FOR_TESTS="yes"

if [[ "${READY_FOR_TESTS}" == "no" ]] ; then
    echo "Config server failed to start..."
    print_docker_logs
    exit 1
fi

echo -e "\n\nStarting brewery apps..."
start_brewery_apps "$SYSTEM_PROPS"