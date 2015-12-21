#!/bin/bash

set -o errexit

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

HEALTH_HOST="127.0.0.1"
HEALTH_PORTS=('9991' '9992' '9993' '9994')
HEALTH_ENDPOINTS="$( printf "http://${HEALTH_HOST}:%s/health " "${HEALTH_PORTS[@]}" )"

BOM_VERSION_PROP_NAME="BOM_VERSION"

# Parse the script arguments
while getopts ":t:o:v:r" opt; do
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
export BOM_VERSION_PROP_NAME=$BOM_VERSION_PROP_NAME

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
echo "Waiting for the apps to boot for [$(( WAIT_TIME * RETRIES ))] seconds"
for i in $( seq 1 "${RETRIES}" ); do
    sleep "${WAIT_TIME}"
    curl -m 5 ${HEALTH_ENDPOINTS} && READY_FOR_TESTS="yes" && break
    echo "Fail #$i/${RETRIES}... will try again in [${WAIT_TIME}] seconds"
done

echo

TESTS_PASSED="no"

# Run acceptance tests
if [[ "${READY_FOR_TESTS}" == "yes" ]] ; then
    echo "Successfully booted up all the apps. Proceeding with the acceptance tests"
    bash -e runAcceptanceTests.sh "-DWHAT_TO_TEST=${WHAT_TO_TEST}" "${TEST_OPTS}" && TESTS_PASSED="yes"
else
    echo "\n\nTests failed - printing docker logs."
    docker-compose -f docker-compose-$WHAT_TO_TEST.yml logs
    exit 1
fi

# Run acceptance tests
if [[ "${TESTS_PASSED}" == "yes" ]] ; then
    echo -e "\n\nTests passed successfully."
    exit 0
else
    echo -e "\n\nTests failed..."
    docker ps | sed -n '1!p' > /tmp/containers.txt
    while read field1 field2 field3; do
      echo -e "\n\nContainer name [$field2] with id [$field1] logs: \n\n"
      docker logs -t $field1
    done < /tmp/containers.txt
    exit 1
fi
