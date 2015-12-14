#!/bin/bash

set -o errexit

REPO_URL="${REPO_URL:-https://github.com/spring-cloud-samples/brewery.git}"
REPO_BRANCH="${REPO_BRANCH:-master}"
REPO_LOCAL="${REPO_LOCAL:-brewery}"
WAIT_TIME="${WAIT_TIME:-5}"
RETRIES="${RETRIES:-48}"

HEALTH_HOST="127.0.0.1"
HEALTH_PORTS=('9991' '9992' '9993' '9994')
HEALTH_ENDPOINTS="$( printf "http://${HEALTH_HOST}:%s/health " "${HEALTH_PORTS[@]}" )"

# Parse the script arguments
while getopts ":t:o:v:" opt; do
    case $opt in
        t)
            WHAT_TO_TEST="${OPTARG}"
            ;;
        o)
            TEST_OPTS="${OPTARG}"
            ;;
        v)
            VERSION="${OPTARG}"
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

if [[ -z "${WHAT_TO_TEST}" || -z "${VERSION}" ]]; then
    echo "You must provide -t and -v options" >&2
    exit 1
fi

cat <<EOF

Running tests with the following parameters

WHAT_TO_TEST=${WHAT_TO_TEST}
TEST_OPTS=${TEST_OPTS}
VERSION=${VERSION}

EOF

export WHAT_TO_TEST=$WHAT_TO_TEST
export TEST_OPTS=$TEST_OPTS
export VERSION=$VERSION
export HEALTH_HOST=$HEALTH_HOST
export WAIT_TIME=$WAIT_TIME
export RETRIES=$RETRIES

# Clone or update the brewery repository
if [[ ! -d "${REPO_LOCAL}/.git" ]]; then
    git clone "${REPO_URL}" "${REPO_LOCAL}"
    cd "${REPO_LOCAL}"
else
    cd "${REPO_LOCAL}"
    git reset --hard
    git pull "${REPO_URL}" "${REPO_BRANCH}"
fi

# Update the desired library version
echo "${WHAT_TO_TEST}=${VERSION}" >> gradle.properties

echo "Using the following gradle.properties"
cat gradle.properties

# Build and run docker images
./gradlew clean build docker --parallel --configure-on-demand
./docker-compose-$WHAT_TO_TEST.sh

# Wait for the apps to boot up
echo "Waiting for the apps to boot for [$(( WAIT_TIME * RETRIES ))] seconds"
for i in $( seq 1 "${RETRIES}" ); do
    sleep "${WAIT_TIME}"
    curl -m 5 ${HEALTH_ENDPOINTS} && READY_FOR_TESTS="yes" && break
    echo "Fail #$i/${RETRIES}... will try again in [${WAIT_TIME}] seconds"
done

echo

# Run acceptance tests
if [[ "${READY_FOR_TESTS}" == "yes" ]] ; then
    echo "Successfully booted up all the apps. Proceeding with the acceptance tests"
    bash -e runAcceptanceTests.sh "-DWHAT_TO_TEST=${WHAT_TO_TEST} ${TEST_OPTS}"
else
    echo "Failed to boot the apps."
    docker-compose -f docker-compose-$WHAT_TO_TEST.yml logs
    exit 1
fi

