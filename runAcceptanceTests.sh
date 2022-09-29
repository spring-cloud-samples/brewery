#!/bin/bash

set -o errexit

# ======================================= FUNCTIONS START =======================================

# Tails the log
function tail_log() {
	echo -e "\n\nLogs of [$1] jar app"
	tail -n ${NUMBER_OF_LINES_TO_LOG} "${CURRENT_DIR}/${1}"/build/libs/nohup.log || echo "Failed to open log"
}

# Iterates over active containers and prints their logs to stdout
function print_logs() {
	echo -e "\n\nSomething went wrong... Printing logs:\n"
	docker ps | sed -n '1!p' >/tmp/containers.txt
	while read field1 field2 field3; do
		echo -e "\n\nContainer name [$field2] with id [$field1] logs: \n\n"
		docker logs --tail=${NUMBER_OF_LINES_TO_LOG} -t ${field1}
	done </tmp/containers.txt
	echo -e "\n\nPrinting docker compose logs - start\n\n"
	docker-compose -f "docker-compose-${WHAT_TO_TEST}.yml" logs || echo "Failed to print docker compose logs"
	echo -e "\n\nPrinting docker compose logs - end\n\n"
	tail_log "brewing"
	tail_log "gateway"
	tail_log "proxy"
	tail_log "presenting"
	tail_log "reporting"
	tail_log "ingredients"
	tail_log "config-server"
	tail_log "eureka"
	tail_log "discovery"
	tail_log "zookeeper"
	tail_log "zipkin-server"
	tail_log "kafka"
	echo -e "\n\nPrinting Kafka logs" && cat /tmp/spring-cloud-dataflow-*/launcher-*/launcher.kafka/* || echo "No kafka was running"
}

# ${RETRIES} number of times will try to netcat to passed port $1 and host $2
function netcat_port() {
	local PASSED_HOST="${2:-$HEALTH_HOST}"
	local READY_FOR_TESTS=1
	for i in $(seq 1 "${RETRIES}"); do
		sleep "${WAIT_TIME}"
		nc -v -w 1 ${PASSED_HOST} $1 && READY_FOR_TESTS=0 && break
		echo "Fail #$i/${RETRIES}... will try again in [${WAIT_TIME}] seconds"
	done
	return ${READY_FOR_TESTS}
}

# ${RETRIES} number of times will try to netcat to passed port $1 and localhost
function netcat_local_port() {
	netcat_port $1 "127.0.0.1"
}

# ${RETRIES} number of times will try to curl to /health endpoint to passed port $1 and host $2
function curl_health_endpoint() {
	local PASSED_HOST="${2:-$HEALTH_HOST}"
	local READY_FOR_TESTS=1
	for i in $(seq 1 "${RETRIES}"); do
		sleep "${WAIT_TIME}"
		curl -m 5 "${PASSED_HOST}:$1/health" && READY_FOR_TESTS=0 && break
		echo "Fail #$i/${RETRIES}... will try again in [${WAIT_TIME}] seconds"
	done
	return ${READY_FOR_TESTS}
}

# ${RETRIES} number of times will try to curl to /health endpoint to passed port $1 and localhost
function curl_local_health_endpoint() {
	curl_health_endpoint $1 "127.0.0.1"
}

# Runs the `java -jar` for given application $1 and system properties $2
function java_jar() {
	local APP_JAVA_PATH=$1/build/libs
	local EXPRESSION="nohup ${JAVA_PATH_TO_BIN}java $2 $MEM_ARGS -jar $APP_JAVA_PATH/*.jar >$APP_JAVA_PATH/nohup.log &"
	echo -e "\nTrying to run [$EXPRESSION]"
	eval ${EXPRESSION}
	pid=$!
	echo ${pid} >${APP_JAVA_PATH}/app.pid
	echo -e "[$1] process pid is [$pid]"
	echo -e "System props are [$2]"
	echo -e "Logs are under [build/$1.log] or from nohup [$APP_JAVA_PATH/nohup.log]\n"
	return 0
}

# Starts the main brewery apps with given system props $1
function start_brewery_apps() {
	local REMOTE_DEBUG="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address"
	java_jar "presenting" "$1 $REMOTE_DEBUG=8991"
	java_jar "brewing" "$1 $REMOTE_DEBUG=8992"
	java_jar "gateway" "$1 $REMOTE_DEBUG=8993"
	java_jar "ingredients" "$1 $REMOTE_DEBUG=8994"
	java_jar "reporting" "$1 $REMOTE_DEBUG=8995"
	return 0
}

function kill_and_log() {
	kill -9 $(cat "$1"/build/libs/app.pid) && echo "Killed $1" || echo "Can't find $1 in running processes"
	pkill_app "$1"
}

function pkill_app() {
	pkill -f "$1" && echo "Killed $1 via pkill" || echo "Can't find $1 in running processes (tried with pkill)"
}

function kill_all_apps_with_port() {
	kill_app_with_port 9991
	kill_app_with_port 9992
	kill_app_with_port 9993
	kill_app_with_port 9994
	kill_app_with_port 9995
	kill_app_with_port 9996
	kill_app_with_port 9997
	kill_app_with_port 9998
	kill_app_with_port 9999
	kill_app_with_port 8888
	kill_app_with_port 8761
	kill_app_with_port 9411
	kill_app_with_port 9092
	kill_app_with_port 2181
}

# port is $1
function kill_app_with_port() {
	kill -9 $(lsof -t -i:$1) && echo "Killed an app running on port [$1]" || echo "No app running on port [$1]"
}

# Kills all started aps
function kill_all_apps() {
	echo $(pwd)
	kill_and_log "brewing"
	kill_and_log "proxy"
	kill_and_log "presenting"
	kill_and_log "ingredients"
	kill_and_log "reporting"
	kill_and_log "config-server"
	kill_and_log "eureka"
	kill_and_log "zookeeper"
	kill_and_log "zipkin-server"
	kill_all_apps_with_port
	pkill_app "rabbit"
	if [[ -z "${KILL_NOW_APPS}" ]]; then
		kill_docker
	fi
	pkill -15 -f JarLauncher || echo "No kafka was running"
	return 0
}

# Kills all docker related elements
function kill_docker() {
	docker ps -a -q | xargs -n 1 -P 8 -I {} docker stop {} || echo "No running docker containers are left"
}

# Kills all started aps if the switch is on
function kill_all_apps_if_switch_on() {
	if [[ ${KILL_AT_THE_END} ]]; then
		echo -e "\n\nKilling all the apps"
		kill_all_apps
	else
		echo -e "\n\nNo switch to kill the apps turned on"
		return 0
	fi
	return 0
}

function wavefrontVersion {
	local version="${1}"
	curl --silent https://repo.spring.io/libs-snapshot-local/com/wavefront/wavefront-spring-boot-starter/maven-metadata.xml | grep "<version>${version}." | grep "SNAPSHOT" | tail -1 | sed -ne '/<version>/s#\s*<[^>]*>\s*##gp' | xargs
}

function print_usage() {
	cat <<EOF

USAGE:

You can use the following options:

GLOBAL:
-t  |--whattotest  - define what you want to test (i.e. ZOOKEEPER, SLEUTH, EUREKA, CONSUL, SCS)
-v  |--version - which version of BOM do you want to use? Defaults to current release train snapshot
-h  |--healthhost - what is your health host? where is docker? defaults to localhost
-l  |--numberoflines - how many lines of logs of your app do you want to print? Defaults to 1000
-r  |--reset - do you want to reset the git repo of brewery? Defaults to "no"
-ke |--killattheend - should kill all the running apps at the end of execution? Defaults to "no"
-n  |--killnow - should not run all the logic but only kill the running apps? Defaults to "no"
-x  |--skiptests - should skip running of e2e tests? Defaults to "no"
-s  |--skipbuilding - should skip building of the projects? Defaults to "no"
-k  |--kafka - uses Kafka instead of RabbitMQ
-d  |--skipdeployment - should skip deployment of apps? Defaults to "no"
-a  |--deployonlyapps - should deploy only the brewery business apps instead of the infra too? Defaults to "no"
-b  |--bootversion - Which version of Boot should be used?
-cli|--cliversion - which version of Spring Cloud CLI should be used (it's used to start Kafka)?
-ve |--verbose - Will print all library versions
-br |--branch - Which repo branch of the brewery repo should be checked out. Defaults to "main"

EOF
}

# ======================================= FUNCTIONS END =======================================

# ======================================= VARIABLES START =======================================
CURRENT_DIR=$(pwd)
REPO_URL="${REPO_URL:-https://github.com/spring-cloud-samples/brewery.git}"
if [[ -d acceptance-tests ]]; then
	REPO_LOCAL="${REPO_LOCAL:-.}"
else
	REPO_LOCAL="${REPO_LOCAL:-brewery}"
fi
WAIT_TIME="${WAIT_TIME:-5}"
RETRIES="${RETRIES:-70}"
DEFAULT_VERSION="${DEFAULT_VERSION:-}"
DEFAULT_HEALTH_HOST="${DEFAULT_HEALTH_HOST:-127.0.0.1}"
DEFAULT_NUMBER_OF_LINES_TO_LOG="${DEFAULT_NUMBER_OF_LINES_TO_LOG:-1000}"
SHOULD_START_RABBIT="${SHOULD_START_RABBIT:-yes}"
JAVA_PATH_TO_BIN="${JAVA_HOME}/bin/"
if [[ -z "${JAVA_HOME}" ]]; then
	JAVA_PATH_TO_BIN=""
fi
LOCALHOST="127.0.0.1"
MEM_ARGS="-Xmx128m -Xss1024k"
SLEEP_TIME_FOR_DISCOVERY="${SLEEP_TIME_FOR_DISCOVERY:-90}"

BOOT_VERSION_PROP_NAME="BOOT_VERSION"
BOM_VERSION_PROP_NAME="BOM_VERSION"

# ======================================= VARIABLES END =======================================

# ======================================= PARSING ARGS START =======================================
if [[ $# == 0 ]]; then
	print_usage
	exit 0
fi

while [[ $# > 0 ]]; do
	key="$1"
	case ${key} in
	-t | --whattotest)
		WHAT_TO_TEST="$2"
		shift # past argument
		;;
	-v | --version)
		VERSION="$2"
		shift # past argument
		;;
	-h | --healthhost)
		HEALTH_HOST="$2"
		shift # past argument
		;;
	-l | --numberoflines)
		NUMBER_OF_LINES_TO_LOG="$2"
		shift # past argument
		;;
	-r | --reset)
		RESET="yes"
		;;
	-ke | --killattheend)
		KILL_AT_THE_END="yes"
		;;
	-n | --killnow)
		KILL_NOW="yes"
		;;
	-na | --killnowapps)
		KILL_NOW="yes"
		KILL_NOW_APPS="yes"
		;;
	-x | --skiptests)
		NO_TESTS="yes"
		;;
	-s | --skipbuilding)
		SKIP_BUILDING="yes"
		;;
	-k | --kafka)
		KAFKA="yes"
		;;
	-b | --bootversion)
		BOOT_VERSION="$2"
		shift
		;;
	-cli | --cliversion)
		CLI_VERSION="$2"
		shift
		;;
	-br | --branch)
		REPO_BRANCH="$2"
		shift
		;;
	-ve | --verbose)
		VERBOSE="yes"
		;;
	--help)
		print_usage
		exit 0
		;;
	*)
		echo "Invalid option: [$1]"
		print_usage
		exit 1
		;;
	esac
	shift # past argument or value
done

[[ -z "${WHAT_TO_TEST}" ]] && WHAT_TO_TEST=ZOOKEEPER
[[ -z "${VERSION}" ]] && VERSION="${DEFAULT_VERSION}"
[[ -z "${HEALTH_HOST}" ]] && HEALTH_HOST="${DEFAULT_HEALTH_HOST}"
[[ -z "${NUMBER_OF_LINES_TO_LOG}" ]] && NUMBER_OF_LINES_TO_LOG="${DEFAULT_NUMBER_OF_LINES_TO_LOG}"
[[ -z "${REPO_BRANCH}" ]] && REPO_BRANCH="main"

HEALTH_PORTS=('9991' '9992' '9993' '9994' '9995')
HEALTH_ENDPOINTS="$(printf "http://${LOCALHOST}:%s/health " "${HEALTH_PORTS[@]}")"
ACCEPTANCE_TEST_OPTS="${ACCEPTANCE_TEST_OPTS:--DLOCAL_URL=http://${HEALTH_HOST}}"

cat <<EOF

Running tests with the following parameters

REPO_BRANCH=${REPO_BRANCH}
HEALTH_HOST=${HEALTH_HOST}
WHAT_TO_TEST=${WHAT_TO_TEST}
VERSION=${VERSION}
NUMBER_OF_LINES_TO_LOG=${NUMBER_OF_LINES_TO_LOG}
KILL_AT_THE_END=${KILL_AT_THE_END}
KILL_NOW=${KILL_NOW}
KILL_NOW_APPS=${KILL_NOW_APPS}
NO_TESTS=${NO_TESTS}
SKIP_BUILDING=${SKIP_BUILDING}
SHOULD_START_RABBIT=${SHOULD_START_RABBIT}
ACCEPTANCE_TEST_OPTS=${ACCEPTANCE_TEST_OPTS}
DEPLOY_ONLY_APPS=${DEPLOY_ONLY_APPS}
SKIP_DEPLOYMENT=${SKIP_DEPLOYMENT}
KAFKA=${KAFKA:-"no"}
BOOT_VERSION=${BOOT_VERSION}
VERBOSE=${VERBOSE}

EOF

# ======================================= PARSING ARGS END =======================================

# ======================================= EXPORTING VARS START =======================================
export WHAT_TO_TEST=${WHAT_TO_TEST}
export VERSION=${VERSION}
export HEALTH_HOST=${HEALTH_HOST}
export WAIT_TIME=${WAIT_TIME}
export RETRIES=${RETRIES}
export BOOT_VERSION_PROP_NAME=${BOOT_VERSION_PROP_NAME}
export BOM_VERSION_PROP_NAME=${BOM_VERSION_PROP_NAME}
export NUMBER_OF_LINES_TO_LOG=${NUMBER_OF_LINES_TO_LOG}
export KILL_AT_THE_END=${KILL_AT_THE_END}
export KILL_NOW_APPS=${KILL_NOW_APPS}
export LOCALHOST=${LOCALHOST}
export MEM_ARGS=${MEM_ARGS}
export SHOULD_START_RABBIT=${SHOULD_START_RABBIT}
export ACCEPTANCE_TEST_OPTS=${ACCEPTANCE_TEST_OPTS}
export DEPLOY_ONLY_APPS=${DEPLOY_ONLY_APPS}
export SKIP_DEPLOYMENT=${SKIP_DEPLOYMENT}
export JAVA_PATH_TO_BIN=${JAVA_PATH_TO_BIN}
export KAFKA=${KAFKA}
export DEFAULT_HEALTH_HOST=${DEFAULT_HEALTH_HOST}
export BOOT_VERSION=${BOOT_VERSION}
export VERBOSE=${VERBOSE}

export -f reset
export -f tail_log
export -f print_logs
export -f netcat_port
export -f netcat_local_port
export -f curl_health_endpoint
export -f curl_local_health_endpoint
export -f java_jar
export -f start_brewery_apps
export -f kill_all_apps
export -f kill_and_log
export -f kill_all_apps_with_port
export -f kill_app_with_port
export -f kill_docker

trap "{ kill_all_apps_if_switch_on; }" EXIT

# ======================================= EXPORTING VARS END =======================================

# ======================================= Kill all apps and exit if switch set =======================================
if [[ ${KILL_NOW} ]]; then
	echo -e "\nKilling all apps"
	kill_all_apps
	exit 0
fi

# ======================================= Clone or update the brewery repository =======================================
if [[ ! -e "${REPO_LOCAL}/.git" ]]; then
	git clone "${REPO_URL}" "${REPO_LOCAL}"
	cd "${REPO_LOCAL}"
	git checkout "${REPO_BRANCH}"
else
	cd "${REPO_LOCAL}"
	if [[ ${RESET} ]]; then
		git reset --hard
		git pull "${REPO_URL}" "${REPO_BRANCH}"
	fi
fi
CURRENT_DIR=$(pwd)

# ======================================= Building the apps =======================================
echo -e "\n\nUsing the following gradle.properties"
cat gradle.properties

echo -e "\n\n"

# Build the apps
APP_BUILDING_RETRIES=3
APP_WAIT_TIME=1
APP_FAILED="yes"
WORK_OFFLINE="${WORK_OFFLINE:-false}"

PARAMS="--daemon"
if [[ "${WORK_OFFLINE}" == "false" ]]; then
	PARAMS="${PARAMS} --refresh-dependencies"
else
	PARAMS="${PARAMS} --offline"
fi
if [[ "${BOOT_VERSION}" != "" ]]; then
	echo "Will use Boot in version [${BOOT_VERSION}]"
	PARAMS="${PARAMS} -D${BOOT_VERSION_PROP_NAME}=${BOOT_VERSION}"
fi
if [[ "${VERSION}" != "" ]]; then
	echo "Will use BOM in version [${VERSION}]"
	PARAMS="${PARAMS} -D${BOM_VERSION_PROP_NAME}=${VERSION}"
fi
echo -e "\n\nPassing following Gradle parameters [${PARAMS}]\n\n"

if [[ -z "${SKIP_BUILDING}" ]]; then
	if [[ "${KAFKA}" == "yes" ]]; then
		echo "Will use Kafka as a message broker"
		PARAMS="${PARAMS} -Pkafka"
	fi
	for i in $(seq 1 "${APP_BUILDING_RETRIES}"); do
		./gradlew clean ${PARAMS} --parallel
		if [[ "${VERBOSE}" == "yes" ]]; then
			echo -e "\n\nPrinting the dependency tree for all projects\n\n"
			./gradlew allDeps
		fi
		./gradlew build ${PARAMS} && APP_FAILED="no" && break
		echo "Fail #$i/${APP_BUILDING_RETRIES}... will try again in [${APP_WAIT_TIME}] seconds"
	done
else
	APP_FAILED="no"
fi

if [[ "${APP_FAILED}" == "yes" ]]; then
	echo -e "\n\nFailed to build the apps!"
	exit 1
fi

# ======================================= Deploying apps locally or to cloud foundry =======================================
if [[ -z "${SKIP_DEPLOYMENT}" ]]; then
	echo -e "Killing docker"
	kill_docker
fi

INITIALIZATION_FAILED="yes"
if [[ -z "${SKIP_DEPLOYMENT}" ]]; then
	. ./docker-compose-${WHAT_TO_TEST}.sh && INITIALIZATION_FAILED="no"
else
	INITIALIZATION_FAILED="no"
fi

if [[ "${INITIALIZATION_FAILED}" == "yes" ]]; then
	echo -e "\n\nFailed to initialize the apps!"
	print_logs
	kill_all_apps_if_switch_on
	exit 1
fi

# ======================================= Checking if apps are booted =======================================
if [[ -z "${SKIP_DEPLOYMENT}" ]]; then
	# Wait for the apps to boot up
	APPS_ARE_RUNNING="no"

	echo -e "\n\nWaiting for the apps to boot for [$((WAIT_TIME * RETRIES))] seconds"
	for i in $(seq 1 "${RETRIES}"); do
		sleep "${WAIT_TIME}"
		curlResult="$(curl --fail -m 5 ${HEALTH_ENDPOINTS} || echo "DOWN")"
		countOfUps="$(echo "${curlResult}" | tr '}' '\n' | grep "UP" | wc -w | xargs)"
		countOfDowns="$(echo "${curlResult}" | tr '}' '\n' | grep "DOWN" | wc -w | xargs)"
		if [[ "${countOfUps}" == "5" ]]; then
			APPS_ARE_RUNNING="yes"
			echo "The apps are running!"
			break
		else
			echo "Received [${curlResult}] from curl"
			echo "Count of UPs is [${countOfUps}]"
			echo "Count of DOWNs is [${countOfDowns}]"
		fi
		echo "Fail #$i/${RETRIES}... will try again in [${WAIT_TIME}] seconds"
	done

	if [[ "${APPS_ARE_RUNNING}" == "no" ]]; then
		echo -e "\n\nFailed to boot the apps!"
		print_logs
		kill_all_apps_if_switch_on
		exit 1
	fi
	# legacy
	export SLEEP_TIME_FOR_EUREKA="${SLEEP_TIME_FOR_EUREKA:-}"
	if [[ "${SLEEP_TIME_FOR_EUREKA}" != "" ]]; then
		SLEEP_TIME_FOR_DISCOVERY="${SLEEP_TIME_FOR_EUREKA}"
	fi
	echo -e "\n\nWaiting for [${SLEEP_TIME_FOR_DISCOVERY}] secs for the apps to register in service discovery!"
	sleep ${SLEEP_TIME_FOR_DISCOVERY}
else
	echo "Skipping deployment"
	READY_FOR_TESTS="yes"
fi


# ======================================= Running acceptance tests =======================================
TESTS_PASSED="no"

if [[ ${NO_TESTS} ]]; then
	echo -e "\nSkipping end to end tests"
	kill_all_apps_if_switch_on
	exit 0
fi

if [[ "${READY_FOR_TESTS}" == "yes" ]]; then
	echo -e "\n\nSuccessfully booted up all the apps. Proceeding with the acceptance tests"
	echo -e "\n\nRunning acceptance tests with the following parameters [-DWHAT_TO_TEST=${WHAT_TO_TEST} ${ACCEPTANCE_TEST_OPTS}]"
	./gradlew ${PARAMS} :acceptance-tests:acceptanceTests "-DWHAT_TO_TEST=${WHAT_TO_TEST}" ${ACCEPTANCE_TEST_OPTS} --stacktrace --daemon --configure-on-demand && TESTS_PASSED="yes"
fi

# Check the result of tests execution
if [[ "${TESTS_PASSED}" == "yes" ]]; then
	echo -e "\n\nTests passed successfully."
	kill_all_apps_if_switch_on
	exit 0
else
	echo -e "\n\nTests failed..."
	print_logs
	kill_all_apps_if_switch_on
	exit 1
fi
