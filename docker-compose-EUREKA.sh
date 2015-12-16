#!/bin/bash

dockerComposeFile="docker-compose-${WHAT_TO_TEST}.yml"
docker-compose -f $dockerComposeFile kill
#docker-compose -f $dockerComposeFile rm -f
docker-compose -f $dockerComposeFile build

# First boot up Zipkin Web and all of it's dependencies
docker-compose -f $dockerComposeFile up -d eureka

# Wait for the Zipkin apps to boot up
READY_FOR_TESTS="no"
PORT_TO_CHECK=8761

echo "Waiting for the Eureka to boot for [$(( WAIT_TIME * RETRIES ))] seconds"
for i in $( seq 1 "${RETRIES}" ); do
    sleep "${WAIT_TIME}"
    nc -v -z -w 1 $HEALTH_HOST $PORT_TO_CHECK && READY_FOR_TESTS="yes" && break
    echo "Fail #$i/${RETRIES}... will try again in [${WAIT_TIME}] seconds"
done

# Then the rest
echo "Starting brewery apps..."
docker-compose -f $dockerComposeFile up -d
