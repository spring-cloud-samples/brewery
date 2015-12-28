#!/bin/bash

dockerComposeFile="docker-compose-${WHAT_TO_TEST}.yml"
docker-compose -f $dockerComposeFile kill
docker-compose -f $dockerComposeFile build

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

echo -e "\n\nStarting brewery apps..."
docker-compose -f $dockerComposeFile up -d
