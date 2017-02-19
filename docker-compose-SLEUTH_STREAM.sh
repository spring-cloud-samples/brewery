#!/bin/bash
BOOT_VERSION=1.4.3.RELEASE
CLI_VERSION=1.2.3.RELEASE

function run_kafka() {
    local APP_JAVA_PATH=${CURRENT_DIR}/build/
    mkdir -p ${APP_JAVA_PATH}
    local EXPRESSION="nohup ${CLI_PATH}spring cloud kafka >$APP_JAVA_PATH/kafka.log &"
    echo -e "\nTrying to run [$EXPRESSION]"
    eval ${EXPRESSION}
    pid=$!
    echo ${pid} > ${APP_JAVA_PATH}/app.pid
    echo -e "[kafka] process pid is [$pid]"
    echo -e "Logs are under [build/kafka.log]\n"
    return 0
}

SYSTEM_PROPS="-DRABBIT_HOST=${HEALTH_HOST} -Dspring.rabbitmq.port=9672 -Dspring.zipkin.host=localhost"

dockerComposeRoot="docker-compose-${WHAT_TO_TEST}"
dockerComposeFile="${dockerComposeRoot}.yml"
docker-compose -f $dockerComposeFile kill
docker-compose -f $dockerComposeFile build

if [[ "${SHOULD_START_RABBIT}" == "yes" ]] ; then
    if [[ "${KAFKA}" == "yes" ]] ; then
        echo -e "\nCheck if sdkman is installed"
        SDK_INSTALLED="no"
        [[ -s "${HOME}/.sdkman/bin/sdkman-init.sh" ]] && source "${HOME}/.sdkman/bin/sdkman-init.sh"
        sdk version && SDK_INSTALLED="true" || echo "Failed to execute SDKman"
        CLI_PATH="${HOME}/.sdkman/candidates/springboot/${BOOT_VERSION}/bin/"
        if [[ "${SDK_INSTALLED}" == "no" ]] ; then
          echo "Installing SDKman"
          curl -s "https://get.sdkman.io" | bash
          source "${HOME}/.sdkman/bin/sdkman-init.sh"
        fi
        echo -e "\nInstalling spring boot and spring cloud plugins"
        yes | sdk use springboot "${BOOT_VERSION}"
        echo "Path to Spring CLI [${CLI_PATH}]"
        yes | ${CLI_PATH}spring install org.springframework.cloud:spring-cloud-cli:${CLI_VERSION}
        echo -e "\nPrinting versions"
        ${CLI_PATH}spring version
        ${CLI_PATH}spring cloud --version

        echo -e "\nRunning Kafka"
        run_kafka
        READY_FOR_TESTS="no"
        PORT_TO_CHECK=9092
        echo "Waiting for Kafka to boot for [$(( WAIT_TIME * RETRIES ))] seconds"
        netcat_local_port $PORT_TO_CHECK && READY_FOR_TESTS="yes"
        if [[ "${READY_FOR_TESTS}" == "no" ]] ; then
            echo "Kafka failed to start..."
            print_logs
            kill_all_apps_if_switch_on
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
        print_logs
        kill_all_apps_if_switch_on
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
    print_logs
    kill_all_apps_if_switch_on
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
    print_logs
    kill_all_apps_if_switch_on
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
    print_logs
    kill_all_apps_if_switch_on
    exit 1
fi

echo -e "\n\nStarting brewery apps..."
start_brewery_apps "$SYSTEM_PROPS"

echo -e "\n\nSetting test opts for sleuth stream to call localhost"
ACCEPTANCE_TEST_OPTS="-DLOCAL_URL=http://localhost"
