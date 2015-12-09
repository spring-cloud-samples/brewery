#!/bin/bash

# TODO: Retry takes place due to issues with SocketTimeouts on first try

waitTime=1
retries=3
n=0
success=false

until [ $n -ge $retries ]
do
  echo "Trying to run the tests..."
  docker-compose run acceptance-tests ./gradlew test "$@" --stacktrace --info --no-daemon && success=true && break
  n=$[$n+1]
  echo "Failed... will try again in [$waitTime] seconds"
  sleep $waitTime
done

if [ "$success" = true ] ; then
  exit 0
else
  exit 1
fi