#!/bin/bash

set -o errexit

# Functions

# Iterates over active containers and prints their logs to stdout
function print_docker_logs {
    echo -e "\n\nSomething went wrong... Printing logs of active containers:\n"
    docker ps | sed -n '1!p' > /tmp/containers.txt
    while read field1 field2 field3; do
      echo -e "\n\nContainer name [$field2] with id [$field1] logs: \n\n"
      docker logs -t $field1
    done < /tmp/containers.txt
}

# ${RETRIES} number of times will try to netcat to passed port $1
function netcat_port {
    local READY_FOR_TESTS=1
    for i in $( seq 1 "${RETRIES}" ); do
        sleep "${WAIT_TIME}"
        nc -v -z -w 1 $HEALTH_HOST $1 && READY_FOR_TESTS=0 && break
        echo "Fail #$i/${RETRIES}... will try again in [${WAIT_TIME}] seconds"
    done
    return $READY_FOR_TESTS
}

# ${RETRIES} number of times will try to curl to /health endpoint to passed port $1
function curl_health_endpoint {
    local READY_FOR_TESTS=1
    for i in $( seq 1 "${RETRIES}" ); do
        sleep "${WAIT_TIME}"
        curl -m 5 "${HEALTH_HOST}:$1/health" && READY_FOR_TESTS=0 && break
        echo "Fail #$i/${RETRIES}... will try again in [${WAIT_TIME}] seconds"
    done
    return $READY_FOR_TESTS
}

# Variables
REPO_URL="${REPO_URL:-https://github.com/spring-cloud-samples/brewery.git}"
REPO_BRANCH="${REPO_BRANCH:-master}"
if [[ -d acceptance-tests ]]; then
  REPO_LOCAL="${REPO_LOCAL:-.}"
else
  REPO_LOCAL="${REPO_LOCAL:-brewery}"
fi
WAIT_TIME="${WAIT_TIME:-5}"
RETRIES="${RETRIES:-48}"
DEFAULT_VERSION="${DEFAULT_VERSION:-Brixton.BUILD-SNAPSHOT}"
DEFAULT_HEALTH_HOST="${DEFAULT_HEALTH_HOST:-127.0.0.1}"

BOM_VERSION_PROP_NAME="BOM_VERSION"

# Parse the script arguments
while getopts ":t:v:h:r" opt; do
    case $opt in
        t)
            WHAT_TO_TEST="${OPTARG}"
            ;;
        v)
            VERSION="${OPTARG}"
            ;;
        h)
            HEALTH_HOST="${OPTARG}"
            ;;
        r)
            RESET=0
            ;;
        \?)
            echo "Invalid option: -$OPTARG" >&2
            exit 1
            ;;
        :)
            echo "Option -$OPTARG requires an argument." >&2
            exit 1
            ;;
    esac
done

[[ -z "${WHAT_TO_TEST}" ]] && WHAT_TO_TEST=ZOOKEEPER
[[ -z "${VERSION}" ]] && VERSION="${DEFAULT_VERSION}"
[[ -z "${HEALTH_HOST}" ]] && HEALTH_HOST="${DEFAULT_HEALTH_HOST}"

HEALTH_PORTS=('9991' '9992' '9993' '9994' '9995' '9996' '9997')
HEALTH_ENDPOINTS="$( printf "http://${HEALTH_HOST}:%s/health " "${HEALTH_PORTS[@]}" )"

cat <<EOF

Running tests with the following parameters

HEALTH_HOST=${HEALTH_HOST}
WHAT_TO_TEST=${WHAT_TO_TEST}
VERSION=${VERSION}

EOF

export WHAT_TO_TEST=$WHAT_TO_TEST
export VERSION=$VERSION
export HEALTH_HOST=$HEALTH_HOST
export WAIT_TIME=$WAIT_TIME
export RETRIES=$RETRIES
export BOM_VERSION_PROP_NAME=$BOM_VERSION_PROP_NAME

export -f print_docker_logs
export -f netcat_port
export -f curl_health_endpoint

# Clone or update the brewery repository
if [[ ! -e "${REPO_LOCAL}/.git" ]]; then
    git clone "${REPO_URL}" "${REPO_LOCAL}"
    cd "${REPO_LOCAL}"
else
    cd "${REPO_LOCAL}"
    if [[ $RESET ]]; then
        git reset --hard
        git pull "${REPO_URL}" "${REPO_BRANCH}"
    fi
fi

echo -e "\nAppending if not present the following entry to gradle.properties\n"

# Update the desired BOM version
grep "${BOM_VERSION_PROP_NAME}=${VERSION}" gradle.properties || echo -e "\n${BOM_VERSION_PROP_NAME}=${VERSION}" >> gradle.properties

echo -e "\n\nUsing the following gradle.properties"
cat gradle.properties

echo -e "\n\n"

# Build and run docker images
./gradlew clean build docker --parallel --configure-on-demand
./docker-compose-$WHAT_TO_TEST.sh

# Wait for the apps to boot up
APPS_ARE_RUNNING="no"

echo -e "\n\nWaiting for the apps to boot for [$(( WAIT_TIME * RETRIES ))] seconds"
for i in $( seq 1 "${RETRIES}" ); do
    sleep "${WAIT_TIME}"
    curl -m 5 ${HEALTH_ENDPOINTS} && APPS_ARE_RUNNING="yes" && break
    echo "Fail #$i/${RETRIES}... will try again in [${WAIT_TIME}] seconds"
done

if [[ "${APPS_ARE_RUNNING}" == "no" ]] ; then
    echo "\n\nFailed to boot the apps!"
    print_docker_logs
    exit 1
fi

# Wait for the apps to register in Service Discovery
READY_FOR_TESTS="no"

echo -e "\n\nChecking for the presence of all services in Service Discovery for [$(( WAIT_TIME * RETRIES ))] seconds"
for i in $( seq 1 "${RETRIES}" ); do
    sleep "${WAIT_TIME}"
    curl -m 5 http://localhost:9991/health | grep presenting | grep aggregating |
        grep maturing | grep bottling | grep ingredients | grep reporting && READY_FOR_TESTS="yes" && break
    echo "Fail #$i/${RETRIES}... will try again in [${WAIT_TIME}] seconds"
done

if [[ "${READY_FOR_TESTS}" == "no" ]] ; then
    echo "\n\nThe apps failed to register in Service Discovery!"
    print_docker_logs
    exit 1
fi

echo

# Run acceptance tests
TESTS_PASSED="no"

if [[ "${READY_FOR_TESTS}" == "yes" ]] ; then
    echo -e "\n\nSuccessfully booted up all the apps. Proceeding with the acceptance tests"
    bash -e runAcceptanceTests.sh && TESTS_PASSED="yes"
fi

# Check the result of tests execution
if [[ "${TESTS_PASSED}" == "yes" ]] ; then
    echo -e "\n\nTests passed successfully."
    exit 0
else
    echo -e "\n\nTests failed..."
    print_docker_logs
    exit 1
fi
