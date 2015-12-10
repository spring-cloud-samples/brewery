#!/bin/bash

set -o errexit

REPO_URL="${REPO_URL:-https://github.com/spring-cloud-samples/brewery.git}"
REPO_BRANCH="${REPO_BRANCH:-master}"
REPO_LOCAL="${REPO_LOCAL:-brewery}"

# Parse the script arguments
for i in "$@"; do
    TEST_OPTS=""
    case $i in
        -t=*|--test=*)
            WHAT_TO_TEST="${i#*=}"
            shift
            ;;
        -to=*|--testopts=*)
            TEST_OPTS="${i#*=}"
            shift
            ;;
        -v=*|--version=*)
            VERSION="${i#*=}"
            shift
            ;;
        *)
            ;;
    esac
done

cat <<EOF

Running tests with the following parameters

WHAT_TO_TEST=${WHAT_TO_TEST}
TEST_OPTS=${TEST_OPTS}
VERSION=${VERSION}

EOF

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
echo "$WHAT_TO_TEST=$VERSION" >> gradle.properties

echo "\n\nUsing the following gradle.properties"
cat gradle.properties

# Build and run docker images
./gradlew clean build docker --parallel
docker-compose kill
docker-compose rm -f
docker-compose build
docker-compose up -d

# Wait for the apps to boot up
url="http://127.0.0.1"
waitTime=5
retries=48
totalWaitingTime=$(( waitTime * retries ))
n=0
success=false

echo "\n\nWaiting for the apps to boot for [$totalWaitingTime] seconds"
until [ $n -ge $retries ]; do
    echo "Pinging applications if they're alive..."
    curl $url:9991/health &&
    curl $url:9992/health &&
    curl $url:9993/health &&
    curl $url:9994/health && success=true && break
    n=$(( n+1 ))
    echo "Failed... will try again in [$waitTime] seconds"
    sleep $waitTime
done

# Run acceptance tests
if [ "$success" = true ] ; then
    echo -e "\n\nSuccessfully booted up all the apps. Proceeding with the acceptance tests"
    bash -e runAcceptanceTests.sh "-DWHAT_TO_TEST=$WHAT_TO_TEST"
else
    echo -e "\n\nFailed to boot the apps."
    exit 1
fi
