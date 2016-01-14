#!/bin/bash
set -e

echo -e "\n\nRunning acceptance tests with the following parameters [-DWHAT_TO_TEST=${WHAT_TO_TEST} -DLOCAL_URL=http://${HEALTH_HOST}]"

./gradlew :acceptance-tests:acceptanceTests "-DWHAT_TO_TEST=${WHAT_TO_TEST}" "-DLOCAL_URL=http://${HEALTH_HOST}" --stacktrace --no-daemon --configure-on-demand