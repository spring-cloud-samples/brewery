#!/bin/bash

cd acceptance-tests
./gradlew test "$@" --stacktrace --info
cd ..
