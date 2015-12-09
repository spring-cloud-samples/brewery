#!/bin/bash

# TODO: Retry takes place due to issues with SocketTimeouts on first try (will have to investigate that)
./gradlew test "$@" --stacktrace --info --no-daemon || ./gradlew test "$@" --stacktrace --info --no-daemon