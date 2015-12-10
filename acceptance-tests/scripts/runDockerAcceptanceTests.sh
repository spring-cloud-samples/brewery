#!/bin/sh
set -e

# Parse the script arguments
while [[ $# > 1 ]]
do
key="$1"

case $key in
    -t|--test)
    WHAT_TO_TEST="$2"
    shift
    ;;
    -to|--testopts)
    TEST_OPTS="$2"
    shift
    ;;
    -v|--version)
    VERSION="$2"
    shift
    ;;
    *)
    ;;
esac
shift
done

# Clone or update the brewery repository
REPOSRC=https://github.com/spring-cloud-samples/brewery.git
LOCALREPO=brewery

LOCALREPO_VC_DIR=$LOCALREPO/.git

if [ ! -d $LOCALREPO_VC_DIR ]
then
    git clone $REPOSRC $LOCALREPO
    cd $LOCALREPO
else
    cd $LOCALREPO
    git reset --hard
    git pull $REPOSRC master
fi

# Update the desired library version
echo "$WHAT_TO_TEST=$VERSION" >> gradle.properties

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
totalWaitingTime=240
n=0
success=false

echo "Waiting for the apps to boot for [$totalWaitingTime] seconds"
until [ $n -ge $retries ]
do
  echo "Pinging applications if they're alive..."
  curl $url:9991/health &&
  curl $url:9992/health &&
  curl $url:9993/health &&
  curl $url:9994/health && success=true && break
  n=$[$n+1]
  echo "Failed... will try again in [$waitTime] seconds"
  sleep $waitTime
done

# Run acceptance tests
if [ "$success" = true ] ; then
  echo -e "\n\nSuccessfully booted up all the apps. Proceeding with the acceptance tests"
  bash -e runAcceptanceTests.sh -DWHAT_TO_TEST=$WHAT_TO_TEST "$TEST_OPTS"
else
  echo -e "\n\nFailed to boot the apps."
  exit 1
fi

cd ..
