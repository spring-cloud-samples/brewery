#!/bin/bash

HEX_VALUE="2fb8094330bba913"
DEFAULT_HEALTH_HOST="127.0.0.1"
echo -e "\nHex value [$HEX_VALUE]"
echo -e "\nHealth host [$DEFAULT_HEALTH_HOST]\n"
curl -X POST -d '{"items": ["MALT", "WATER", "HOP", "YEAST"]}' -i -H "PROCESS-ID: $HEX_VALUE" -H "Content-Type: application/json" -H "X-TRACE-ID: $HEX_VALUE" $DEFAULT_HEALTH_HOST:9991/present/order
