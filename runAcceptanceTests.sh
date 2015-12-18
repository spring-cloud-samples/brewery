#!/bin/bash
set -e

echo -e "\n\nRunning acceptance tests with the following parameters [$@]"

./gradlew :acceptance-tests:acceptanceTests "$@" -Dspring.profiles.active=local --stacktrace --no-daemon