#!/bin/bash
set -e

echo -e "\n\nRunning acceptance tests with the following parameters [$@]"

#sudo docker-compose -f docker-compose-$WHAT_TO_TEST.yml run acceptance-tests ./gradlew clean test "$@" --stacktrace --info --no-daemon
./gradlew acceptanceTests "$@" -Dspring.profiles.active=local --stacktrace --info --no-daemon