#!/bin/bash

cd acceptance-tests
./gradlew test "$@"
cd ..