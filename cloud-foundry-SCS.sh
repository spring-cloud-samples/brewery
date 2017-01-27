#!/bin/bash

set -e

root=`pwd`
export CLOUD_PREFIX="scsbrewery"

if [[ -z "${SKIP_DEPLOYMENT}" ]] ; then
    # ====================================================
    if [[ -z "${DEPLOY_ONLY_APPS}" ]] ; then
        echo -e "Killing all apps"
        kill_all_apps
        echo -e "\nDeploying infrastructure apps\n\n"

        READY_FOR_TESTS="no"
        echo -e "\n\nStarting RabbitMQ\n"
        # create RabbitMQ
        APP_NAME="${CLOUD_PREFIX}-rabbitmq"
        (cf s | grep ${APP_NAME} && echo "found ${APP_NAME}") && READY_FOR_TESTS="yes" ||
            cf cs p-rabbitmq standard ${APP_NAME}  && echo "Started RabbitMQ for PCF Dev" && READY_FOR_TESTS="yes"

        if [[ "${READY_FOR_TESTS}" == "no" ]] ; then
            echo "RabbitMQ failed to start..."
            exit 1
        fi

        # ====================================================

        # Boot config-server
        echo -e "\n\nStarting SCS Config Server\n"
        READY_FOR_TESTS="no"
        (yes | cf delete-service "${CLOUD_PREFIX}-config-server" -f) || echo "Failed to kill the app"
        (cf s | grep "${CLOUD_PREFIX}-config-server" && cf ds -f "${CLOUD_PREFIX}-config-server") || echo "Failed to delete the app...  Continuing with the script"
        {
        cf cs p-config-server standard "${CLOUD_PREFIX}-config-server" -c '{"git": { "uri": "https://github.com/spring-cloud-samples/brewery-config.git" } }' && READY_FOR_TESTS="yes"
        } &> /dev/null
        until [ `cf service "${CLOUD_PREFIX}-config-server" | grep -c "succeeded"` -eq 1  ]
        do
          echo -n "."
        done

        if [[ "${READY_FOR_TESTS}" == "no" ]] ; then
            echo "Config server failed to start..."
            exit 1
        fi

        # ====================================================]
        echo -e "\n\nStarting SCS Discovery\n"
        READY_FOR_TESTS="no"
        (yes | cf delete-service "${CLOUD_PREFIX}-discovery" -f) || echo "Failed to kill the app...  Continuing with the script"
        (cf s | grep "${CLOUD_PREFIX}-discovery" && cf ds -f "${CLOUD_PREFIX}-discovery") || echo "Failed to delete the app...  Continuing with the script"
        {
        cf cs p-service-registry standard "${CLOUD_PREFIX}-discovery" && READY_FOR_TESTS="yes"
        }  &> /dev/null
        until [ `cf service "${CLOUD_PREFIX}-discovery" | grep -c "succeeded"` -eq 1  ]
        do
          echo -n "."
        done

        if [[ "${READY_FOR_TESTS}" == "no" ]] ; then
            echo "Eureka failed to start..."
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
echo -e "Presenting host is [${PRESENTING_HOST}]"

ACCEPTANCE_TEST_OPTS="-Dpresenting.url=http://${PRESENTING_HOST}"
echo -e "\n\nSetting test opts for sleuth stream to call ${ACCEPTANCE_TEST_OPTS}"
