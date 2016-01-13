#!/bin/bash
TIME=`date +%s`
DEFAULT_HEALTH_HOST="${DEFAULT_HEALTH_HOST:-127.0.0.1}"
echo -e "\nCurrent time [$TIME]"
echo -e "\nHealth host [$DEFAULT_HEALTH_HOST]\n"
curl -X POST -d '{"items": ["MALT", "WATER", "HOP", "YEAST"]}' -i -H "PROCESS-ID: $TIME" -H "Content-Type: application/json" -H "X-TRACE-ID: $TIME" -H "X-SPAN-ID: #TIME" $DEFAULT_HEALTH_HOST:9991/present/order
