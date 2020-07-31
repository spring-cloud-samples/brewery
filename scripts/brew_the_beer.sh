#!/bin/bash

set -o errexit

PRESENTING_URL="${PRESENTING_URL:-http://localhost:9991}"

curl -X POST -v -s "${PRESENTING_URL}"/present/order -d '{"items": ["MALT", "WATER", "HOP", "YEAST"]}' -H'Content-Type: application/json' -H'TEST-COMMUNICATION-TYPE: REST-TEMPLATE'