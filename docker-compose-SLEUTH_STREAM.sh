#!/bin/bash

SYSTEM_PROPS="-DRABBIT_HOST=${HEALTH_HOST} -Dspring.rabbitmq.port=9672 -Dspring.zipkin.host=localhost"

dockerComposeFile="docker-compose-${WHAT_TO_TEST}.yml"
docker-compose -f $dockerComposeFile kill
docker-compose -f $dockerComposeFile build

if [[ "${SHOULD_START_RABBIT}" == "yes" ]] ; then
    if [[ "${KAFKA}" == "yes" ]] ; then
        echo -e "\nThe following containers are running:"
        docker ps
        echo -e "\nWill try to stop kafka if it's running"
        docker stop `docker ps -a -q --filter="image=wurstmeister/zookeeper"` || echo "No docker with Zookeeper was running - won't stop anything"
        docker stop `docker ps -a -q --filter="image=wurstmeister/kafka"` || echo "No docker with Kafka was running - won't stop anything"

        echo -e "\nTrying to run Zookeeper in Docker\n"
        docker run -d -p 2181:2181 wurstmeister/zookeeper
        READY_FOR_TESTS="no"
        PORT_TO_CHECK=2181
        echo "Waiting for Zookeeper to boot for [$(( WAIT_TIME * RETRIES ))] seconds"
        netcat_port $PORT_TO_CHECK && READY_FOR_TESTS="yes"

        if [[ "${READY_FOR_TESTS}" == "no" ]] ; then
            echo "Zookeeper failed to start..."
            exit 1
        fi

        echo -e "\nTrying to run Kafka in Docker\n"
        docker run -d -p 9092:9092 --env KAFKA_ADVERTISED_HOST_NAME="${DEFAULT_HEALTH_HOST}" --env KAFKA_ADVERTISED_PORT=9092 --env KAFKA_ZOOKEEPER_CONNECT="${DEFAULT_HEALTH_HOST}":2181 wurstmeister/kafka:0.10.0.0
        READY_FOR_TESTS="no"
        PORT_TO_CHECK=9092
        echo "Waiting for Kafka to boot for [$(( WAIT_TIME * RETRIES ))] seconds"
        netcat_port $PORT_TO_CHECK && READY_FOR_TESTS="yes"

        if [[ "${READY_FOR_TESTS}" == "no" ]] ; then
            echo "Kafka failed to start..."
            exit 1
        fi
    else
        echo -e "\n\nBooting up RabbitMQ"
        docker-compose -f $dockerComposeFile up -d
    fi
fi

if [[ "${KAFKA}" != "yes" ]] ; then
    READY_FOR_TESTS="no"
    PORT_TO_CHECK=9672
    echo "Waiting for RabbitMQ to boot for [$(( WAIT_TIME * RETRIES ))] seconds"
    netcat_port $PORT_TO_CHECK && READY_FOR_TESTS="yes"

    if [[ "${READY_FOR_TESTS}" == "no" ]] ; then
        echo "RabbitMQ failed to start..."
        exit 1
    fi
fi

READY_FOR_TESTS="no"
PORT_TO_CHECK=8761
echo "Waiting for Eureka to boot for [$(( WAIT_TIME * RETRIES ))] seconds"
if [[ "${KAFKA}" == "yes" ]] ; then
    java_jar "eureka" "-Dspring.profiles.active=kafka"
else
    java_jar "eureka"
fi

netcat_local_port $PORT_TO_CHECK && READY_FOR_TESTS="yes"

if [[ "${READY_FOR_TESTS}" == "no" ]] ; then
    echo "Eureka failed to start..."
    exit 1
fi

# Boot zipkin-server
READY_FOR_TESTS="no"
PORT_TO_CHECK=9411
echo "Waiting for the Zipkin Server app to boot for [$(( WAIT_TIME * RETRIES ))] seconds"
if [[ "${KAFKA}" == "yes" ]] ; then
    SYSTEM_PROPS="${SYSTEM_PROPS} -Dspring.profiles.active=kafka"
fi
java_jar "zipkin-server" "${SYSTEM_PROPS} -Dzipkin.collector.sample-rate=1.0 -Dzipkin.query.lookback=86400000"
curl_local_health_endpoint $PORT_TO_CHECK  && READY_FOR_TESTS="yes"

if [[ "${READY_FOR_TESTS}" == "no" ]] ; then
    echo "Config server failed to start..."
    exit 1
fi

echo -e "\n\nZipkin Web is available under 9411 port"

# Boot config-server
READY_FOR_TESTS="no"
PORT_TO_CHECK=8888
echo "Waiting for the Config Server app to boot for [$(( WAIT_TIME * RETRIES ))] seconds"
java_jar "config-server" "$SYSTEM_PROPS"
curl_local_health_endpoint $PORT_TO_CHECK  && READY_FOR_TESTS="yes"

if [[ "${READY_FOR_TESTS}" == "no" ]] ; then
    echo "Config server failed to start..."
    exit 1
fi

echo -e "\n\nStarting brewery apps..."
start_brewery_apps "$SYSTEM_PROPS"

echo -e "\n\nSetting test opts for sleuth stream to call localhost"
ACCEPTANCE_TEST_OPTS="-DLOCAL_URL=http://localhost"
