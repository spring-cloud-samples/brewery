#!/bin/bash

set -e

source ${BUILD_DIRECTORY:-.}/scripts/cf-common.sh
root=`pwd`

if [[ -z "${SKIP_DEPLOYMENT}" ]] ; then
    # ====================================================
    if [[ -z "${DEPLOY_ONLY_APPS}" ]] ; then
        echo -e "\nDeploying infrastructure apps\n\n"

        READY_FOR_TESTS="no"
        echo "Waiting for RabbitMQ to boot for [$(( WAIT_TIME * RETRIES ))] seconds"
        # create RabbitMQ
        APP_NAME=brewery-rabbitmq
        cf s | grep ${APP_NAME} && echo "found ${APP_NAME}" && READY_FOR_TESTS="yes" ||
            cf cs cloudamqp lemur ${APP_NAME} && echo "Started RabbitMQ" && READY_FOR_TESTS="yes"

        if [[ "${READY_FOR_TESTS}" == "no" ]] ; then
            echo "RabbitMQ failed to start..."
            exit 1
        fi

        # ====================================================]

        READY_FOR_TESTS="no"
        echo "Waiting for Eureka to boot for [$(( WAIT_TIME * RETRIES ))] seconds"
        yes | cf delete-service "brewery-discovery" || echo "Failed to kill the app...  Continuing with the script"
        cf s | grep "brewery-discovery" && cf ds -f "brewery-discovery" || echo "Failed to delete the app...  Continuing with the script"
        deploy_app_with_name "eureka" "brewery-discovery" && READY_FOR_TESTS="yes"
        deploy_service "brewery-discovery" && READY_FOR_TESTS="yes"

        if [[ "${READY_FOR_TESTS}" == "no" ]] ; then
            echo "Eureka failed to start..."
            exit 1
        fi

        DISCOVERY_HOST=`app_domain discovery`
        echo -e "Discovery host is [${DISCOVERY_HOST}]"

        # ====================================================
        # Boot zipkin-stuff
        echo -e "\n\nBooting up MySQL"
        READY_FOR_TESTS="no"
        # create MySQL DB
        APP_NAME=brewery-mysql
        cf s | grep ${APP_NAME} && echo "found ${APP_NAME}" && READY_FOR_TESTS="yes" ||
            cf cs cleardb spark ${APP_NAME} && echo "Started ${APP_NAME}" && READY_FOR_TESTS="yes"

        if [[ "${READY_FOR_TESTS}" == "no" ]] ; then
            echo "MySQL failed to start..."
            exit 1
        fi

        # ====================================================
        cd $root

        echo -e "\n\nDeploying Zipkin Server"
        zq=zipkin-server
        ZQ_APP_NAME="brewery-$zq"
        cd $root/$zq
        reset $ZQ_APP_NAME
        cf d -f $ZQ_APP_NAME
        cd $root/zipkin-server
        cf push && READY_FOR_TESTS="yes"

        if [[ "${READY_FOR_TESTS}" == "no" ]] ; then
            echo "Zipkin Server failed to start..."
            exit 1
        fi
        cd $root

        # ====================================================
        echo -e "\n\nDeploying Zipkin Web"
        zw=zipkin-web
        ZW_APP_NAME="brewery-$zw"
        reset $ZW_APP_NAME
        cf d -f $ZW_APP_NAME
        zqs_name=`app_domain $ZQ_APP_NAME`
        echo -e "Zipkin Query server host is [${zqs_name}]"
        cd $root/zipkin-web
        cf push --no-start
        jcjm=`$root/scripts/zipkin-deploy-helper.py $zqs_name`
        echo -e "Setting env vars [${jcjm}]"
        cf set-env $ZW_APP_NAME JBP_CONFIG_JAVA_MAIN "${jcjm}"
        cf restart $ZW_APP_NAME && READY_FOR_TESTS="yes"

        if [[ "${READY_FOR_TESTS}" == "no" ]] ; then
            echo "Zipkin Web failed to start..."
            exit 1
        fi
        cd $root

        # ====================================================

        # Boot config-server
        READY_FOR_TESTS="no"
        echo "Waiting for the Config Server app to boot for [$(( WAIT_TIME * RETRIES ))] seconds"
        yes | cf delete-service "brewery-config-server" || echo "Failed to kill the app"
        cf s | grep "brewery-config-server" && cf ds -f "brewery-config-server" || echo "Failed to delete the app...  Continuing with the script"
        deploy_app_with_name "config-server" "brewery-config-server" && READY_FOR_TESTS="yes"
        deploy_service "brewery-config-server" || echo "Failed to bind the service... Continuing with the script"

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
    deploy_app_with_name "presenting" "brewery-presenting"
    deploy_app_with_name "brewing" "brewery-brewing"
    deploy_app_with_name "zuul" "brewery-zuul"

else
    INITIALIZATION_FAILED="no"
    echo -e "\n\nSkipping deployment of apps, proceeding with e2e tests"
fi

# ====================================================

PRESENTING_HOST=`app_domain presenting`
ZIPKIN_SERVER_HOST=`app_domain zipkin-server`
echo -e "Presenting host is [${PRESENTING_HOST}]"
echo -e "Zikpin server host is [${ZIPKIN_SERVER_HOST}]"

ACCEPTANCE_TEST_OPTS="-DLOCAL_URL=http://${ZIPKIN_SERVER_HOST} -Dpresenting.url=http://${PRESENTING_HOST} -Dzipkin.query.port=80"
echo -e "\n\nSetting test opts for sleuth stream to call ${ACCEPTANCE_TEST_OPTS}"
