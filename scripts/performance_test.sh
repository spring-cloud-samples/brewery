#!/bin/bash

set -o errexit

PRESENTING_URL="${PRESENTING_URL:-http://localhost:9991}"

wrk -t4 -c128 --timeout 15s -d1m "${PRESENTING_URL}"/present/order -s brewery.lua --latency