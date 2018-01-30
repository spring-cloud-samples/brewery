#!/bin/bash

set -e

root=`pwd`

if [[ -z "${SKIP_DEPLOYMENT}" ]] ; then
    # ====================================================
    if [[ -z "${DEPLOY_ONLY_APPS}" ]] ; then
        echo -e "\nDeploying infrastructure apps\n\n"

        READY_FOR_TESTS="no"
        echo "Waiting for RabbitMQ to boot for [$(( WAIT_TIME * RETRIES ))] seconds"
        # create RabbitMQ
        APP_NAME="${CLOUD_PREFIX}-rabbitmq"
        cf s | grep ${APP_NAME} && echo "found ${APP_NAME}" && READY_FOR_TESTS="yes" ||
            cf cs cloudamqp lemur ${APP_NAME} && echo "Started RabbitMQ" && READY_FOR_TESTS="yes" ||
            cf cs p-rabbitmq standard ${APP_NAME}  && echo "Started RabbitMQ for PCF Dev" && READY_FOR_TESTS="yes"

        if [[ "${READY_FOR_TESTS}" == "no" ]] ; then
            echo "RabbitMQ failed to start..."
            exit 1
        fi

        # ====================================================]

        READY_FOR_TESTS="no"
        echo "Waiting for Eureka to boot for [$(( WAIT_TIME * RETRIES ))] seconds"
        yes | cf delete-service "${CLOUD_PREFIX}-discovery" || echo "Failed to kill the app...  Continuing with the script"
        cf s | grep "${CLOUD_PREFIX}-discovery" && cf ds -f "${CLOUD_PREFIX}-discovery" || echo "Failed to delete the app...  Continuing with the script"
        deploy_app_with_name "eureka" "${CLOUD_PREFIX}-discovery" && READY_FOR_TESTS="yes"
        deploy_service "${CLOUD_PREFIX}-discovery" && READY_FOR_TESTS="yes"

        if [[ "${READY_FOR_TESTS}" == "no" ]] ; then
            echo "Eureka failed to start..."
            exit 1
        fi

        DISCOVERY_HOST=`app_domain "${CLOUD_PREFIX}-discovery"`
        echo -e "Discovery host is [${DISCOVERY_HOST}]"

        # ====================================================
        # Boot zipkin-stuff
        echo -e "\n\nBooting up MySQL"
        READY_FOR_TESTS="no"
        # create MySQL DB
        APP_NAME="${CLOUD_PREFIX}-mysql"
        cf s | grep ${APP_NAME} && echo "found ${APP_NAME}" && READY_FOR_TESTS="yes" ||
            cf cs cleardb spark ${APP_NAME} && echo "Started ${APP_NAME}" && READY_FOR_TESTS="yes" ||
            cf cs p-mysql 512mb ${APP_NAME} && echo "Started ${APP_NAME} for PCF Dev" && READY_FOR_TESTS="yes"

        if [[ "${READY_FOR_TESTS}" == "no" ]] ; then
            echo "MySQL failed to start..."
            exit 1
        fi

        # ====================================================
        cd $root

        echo -e "\n\nDeploying Zipkin Server"
        zq=zipkin-server
        ZQ_APP_NAME="${CLOUD_PREFIX}-$zq"
        cd $root/$zq
        reset $ZQ_APP_NAME
        echo "Downloading Zipkin Server jar"
        rm -rf build
        mkdir build
        cd build
        curl -sSL https://zipkin.io/quickstart.sh | bash -s
        cd ..
        cf d -f $ZQ_APP_NAME
        cd $root/zipkin-server
        cf push -f "manifest-${CLOUD_PREFIX}.yml" && READY_FOR_TESTS="yes"

        if [[ "${READY_FOR_TESTS}" == "no" ]] ; then
            echo "Zipkin Server failed to start..."
            exit 1
        fi
        cd $root

        # ====================================================
        echo -e "\n\nZipkin web is a part of the Zipkin Server"

        # ====================================================

        # Boot config-server
        READY_FOR_TESTS="no"
        echo "Waiting for the Config Server app to boot for [$(( WAIT_TIME * RETRIES ))] seconds"
        yes | cf delete-service "${CLOUD_PREFIX}-config-server" || echo "Failed to kill the app"
        cf s | grep "${CLOUD_PREFIX}-config-server" && cf ds -f "${CLOUD_PREFIX}-config-server" || echo "Failed to delete the app...  Continuing with the script"
        deploy_app_with_name "config-server" "${CLOUD_PREFIX}-config-server" && READY_FOR_TESTS="yes"
        deploy_service "${CLOUD_PREFIX}-config-server" || echo "Failed to bind the service... Continuing with the script"

        if [[ "${READY_FOR_TESTS}" == "no" ]] ; then
            echo "Config server failed to start..."
            exit 1
        fi
    else
        echo -e "\nWill not deploy infrastructure apps. Proceeding to brewery apps deployment.\n\n"
    fi

    # ====================================================

    cd $root
    echo -e "\n\nStarting brewery apps..."
    echo "Starting presenting"
    deploy_app_with_name "presenting" "${CLOUD_PREFIX}-presenting" &
    echo "Starting brewing"
    deploy_app_with_name "brewing" "${CLOUD_PREFIX}-brewing" &
    echo "Starting zuul"
    deploy_app_with_name "zuul" "${CLOUD_PREFIX}-zuul" &
    echo "Starting ingredients"
    deploy_app_with_name "ingredients" "${CLOUD_PREFIX}-ingredients" &
    echo "Starting reporting"
    deploy_app_with_name "reporting" "${CLOUD_PREFIX}-reporting" &
    wait

else
    INITIALIZATION_FAILED="no"
    echo -e "\n\nSkipping deployment of apps, proceeding with e2e tests"
fi

# ====================================================

PRESENTING_HOST=`app_domain ${CLOUD_PREFIX}-presenting`
ZIPKIN_SERVER_HOST=`app_domain ${CLOUD_PREFIX}-zipkin-server`
echo -e "Presenting host is [${PRESENTING_HOST}]"
echo -e "Zikpin server host is [${ZIPKIN_SERVER_HOST}]"

ACCEPTANCE_TEST_OPTS="-DLOCAL_URL=http://${ZIPKIN_SERVER_HOST} -Dpresenting.url=http://${PRESENTING_HOST} -Dzipkin.query.port=80"
echo -e "\n\nSetting test opts for sleuth stream to call ${ACCEPTANCE_TEST_OPTS}"
