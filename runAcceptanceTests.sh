#!/bin/bash

docker-compose run acceptance-tests ./gradlew test "$@" --stacktrace --info --no-daemon