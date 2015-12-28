#!/bin/bash
set -e

echo -e "\n\nRunning acceptance tests with the following parameters [$@] and additional test opts [${TEST_OPTS}]"

./gradlew :acceptance-tests:acceptanceTests "$@" ${TEST_OPTS} --stacktrace --no-daemon --configure-on-demand