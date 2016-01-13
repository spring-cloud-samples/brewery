#!/bin/bash
set -e

echo -e "\n\nRunning acceptance tests with the following parameters [-DWHAT_TO_TEST=${WHAT_TO_TEST} -DLOCAL_URL=http://${LOCALHOST}]"

./gradlew :acceptance-tests:acceptanceTests "-DWHAT_TO_TEST=${WHAT_TO_TEST}" "-DLOCAL_URL=http://${LOCALHOST}" --stacktrace --no-daemon --configure-on-demand