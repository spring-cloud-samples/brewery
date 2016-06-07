#!/bin/bash

# $1 path to the json with spans

curl -X POST -H'Content-Type: application/json' -s localhost:9411/api/v1/spans -d @$1