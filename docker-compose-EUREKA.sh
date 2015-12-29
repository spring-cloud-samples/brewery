#!/bin/bash

dockerComposeFile="docker-compose-${WHAT_TO_TEST}.yml"
docker-compose -f $dockerComposeFile kill
docker-compose -f $dockerComposeFile build

echo -e "\n\nBooting up RabbitMQ"
docker-compose -f $dockerComposeFile up -d rabbitmq

# First boot up Eureka and all of it's dependencies
docker-compose -f $dockerComposeFile up -d discovery

# Wait for the Eureka apps to boot up
READY_FOR_TESTS="no"
PORT_TO_CHECK=8761

echo "Waiting for the Eureka to boot for [$(( WAIT_TIME * RETRIES ))] seconds"
curl_health_endpoint $PORT_TO_CHECK && READY_FOR_TESTS="yes"

if [[ "${READY_FOR_TESTS}" == "no" ]] ; then
    echo "Discovery failed to start..."
    print_docker_logs
    exit 1
fi

# Boot config-server
READY_FOR_TESTS="no"
PORT_TO_CHECK=8888

docker-compose -f $dockerComposeFile up -d configserver

echo "Waiting for the Config Server app to boot for [$(( WAIT_TIME * RETRIES ))] seconds"
curl_health_endpoint $PORT_TO_CHECK && READY_FOR_TESTS="yes"

if [[ "${READY_FOR_TESTS}" == "no" ]] ; then
    echo "Config server failed to start..."
    print_docker_logs
    exit 1
fi

# Then the rest
echo -e "\n\nStarting brewery apps..."
docker-compose -f $dockerComposeFile up -d