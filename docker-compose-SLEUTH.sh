#!/bin/bash

dockerComposeFile="docker-compose-${WHAT_TO_TEST}.yml"
sudo docker-compose -f $dockerComposeFile kill
#sudo docker-compose -f $dockerComposeFile rm -f
#sudo docker-compose -f $dockerComposeFile build

# First boot up Zipkin Web and all of it's dependencies
sudo docker-compose -f $dockerComposeFile up -d mysql web collector query

# Wait for the Zipkin apps to boot up
READY_FOR_TESTS="no"

echo "Waiting for the apps to boot for [$(( WAIT_TIME * RETRIES ))] seconds"
for i in $( seq 1 "${RETRIES}" ); do
    sleep "${WAIT_TIME}"
    nc -v -z -w 1 $HEALTH_HOST 9410 && READY_FOR_TESTS="yes" && break
    echo "Fail #$i/${RETRIES}... will try again in [${WAIT_TIME}] seconds"
done

# Then the rest
sudo docker-compose -f $dockerComposeFile up -d
