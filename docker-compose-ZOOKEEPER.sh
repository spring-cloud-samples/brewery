#!/bin/bash

dockerComposeFile="docker-compose-${WHAT_TO_TEST}.yml"
docker-compose -f $dockerComposeFile kill
docker-compose -f $dockerComposeFile build

# Boot config-server
READY_FOR_TESTS="no"
PORT_TO_CHECK=8888

docker-compose -f $dockerComposeFile up -d configserver

echo "Waiting for the Config Server app to boot for [$(( WAIT_TIME * RETRIES ))] seconds"
for i in $( seq 1 "${RETRIES}" ); do
    sleep "${WAIT_TIME}"
    curl -m 5 "${HEALTH_HOST}:${PORT_TO_CHECK}/health" && READY_FOR_TESTS="yes" && break
    echo "Fail #$i/${RETRIES}... will try again in [${WAIT_TIME}] seconds"
done

if [[ "${READY_FOR_TESTS}" == "no" ]] ; then
    echo "Config server failed to start..."
    exit 1
fi

echo -e "\n\nStarting brewery apps..."
docker-compose -f $dockerComposeFile up -d
