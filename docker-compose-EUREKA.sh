#!/bin/bash

dockerComposeFile="docker-compose-${WHAT_TO_TEST}.yml"
docker-compose -f $dockerComposeFile kill
docker-compose -f $dockerComposeFile build

# First boot up Eureka and all of it's dependencies
docker-compose -f $dockerComposeFile up -d discovery

# Wait for the Eureka apps to boot up
READY_FOR_TESTS="no"
PORT_TO_CHECK=8761

echo "Waiting for the Eureka to boot for [$(( WAIT_TIME * RETRIES ))] seconds"
for i in $( seq 1 "${RETRIES}" ); do
    sleep "${WAIT_TIME}"
    nc -v -z -w 1 $HEALTH_HOST $PORT_TO_CHECK && READY_FOR_TESTS="yes" && break
    echo "Fail #$i/${RETRIES}... will try again in [${WAIT_TIME}] seconds"
done

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

# Then the rest
echo -e "\n\nStarting brewery apps..."
docker-compose -f $dockerComposeFile up -d
