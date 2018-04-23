#!/bin/bash

set -o errexit

# ======================================= FUNCTIONS START =======================================

# CLOUD FOUNDRY -- START

# can have 2 params , username and password
function login(){
    echo "Logging in to Cloud Foundry at ${CLOUD_TARGET}"
    cf api | grep ${CLOUD_TARGET} || cf api ${CLOUD_TARGET} --skip-ssl-validation
    if [[ -z "$1" ]] ; then
        echo "No username and password provided - will ask for all data"
        cf apps | grep OK || cf login --skip-ssl-validation -a ${CLOUD_TARGET}
    else
        echo "Username and password provided - will try to log in"
        {
            cf apps | grep OK || cf login --skip-ssl-validation -a ${CLOUD_TARGET} -u $1 -p $2 -o ${CLOUD_ORG} -s ${CLOUD_SPACE}
        } &> /dev/null
    fi

}

function app_domain(){
    D=`cf apps | grep $1 | tr -s ' ' | cut -d' ' -f 6 | cut -d, -f1`
    echo ${D}
}

function deploy_app(){
    deploy_app_with_name $1 $1
}

function deploy_zookeeper_app(){
    APP_DIR=$1
    APP_NAME=$1
    cd ${APP_DIR}
    cf push ${APP_NAME} --no-start
    APPLICATION_DOMAIN=`app_domain ${APP_NAME}`
    echo determined that application_domain for ${APP_NAME} is ${APPLICATION_DOMAIN}.
    cf env ${APP_NAME} | grep APPLICATION_DOMAIN || cf set-env ${APP_NAME} APPLICATION_DOMAIN ${APPLICATION_DOMAIN}
    cf env ${APP_NAME} | grep arguments || cf set-env ${APP_NAME} "spring.cloud.zookeeper.connectString" "$2:2181"
    cf restart ${APP_NAME}
    cd ..
}

function deploy_app_with_name(){
    APP_DIR=$1
    APP_NAME=$2
    cd ${APP_DIR}
    cf push ${APP_NAME} --no-start -f "manifest-${CLOUD_PREFIX}.yml" -b https://github.com/cloudfoundry/java-buildpack.git#v3.8.1
    APPLICATION_DOMAIN=`app_domain ${APP_NAME}`
    echo -e "\n\nDetermined that application_domain for $APP_NAME is $APPLICATION_DOMAIN\n\n"
    cf env ${APP_NAME} | grep APPLICATION_DOMAIN || cf set-env ${APP_NAME} APPLICATION_DOMAIN ${APPLICATION_DOMAIN}

    if [[ "${WHAT_TO_TEST}" == "SCS" ]] ; then
      cf se ${APP_NAME} CF_TARGET "https://"${CLOUD_TARGET}
    fi

    cf restart ${APP_NAME}
    cd ..
}

function deploy_app_with_name_parallel(){
    xargs -n 2 -P 4 bash -c 'deploy_app_with_name "$@"'
}

function deploy_service(){
    N=$1
    D=`app_domain ${N}`
    JSON='{"uri":"http://'${D}'"}'
    cf create-user-provided-service ${N} -p ${JSON}
}

function reset(){
    app_name=$1
    echo "going to remove ${app_name} if it exists"
    cf apps | grep ${app_name} && cf d -f ${app_name}
    echo "deleted ${app_name}"
}
# CLOUD FOUNDRY -- FINISH


# Tails the log
function tail_log() {
    echo -e "\n\nLogs of [$1] jar app"
    if [[ -z "${CLOUD_FOUNDRY}" ]] ; then
        tail -n ${NUMBER_OF_LINES_TO_LOG} build/"$1".log || echo "Failed to open log"
    else
        cf logs "${CLOUD_PREFIX}-$1" --recent || echo "Failed to open log"
    fi
}

# Iterates over active containers and prints their logs to stdout
function print_logs() {
    echo -e "\n\nSomething went wrong... Printing logs:\n"
    if [[ -z "${CLOUD_FOUNDRY}" ]] ; then
            docker ps | sed -n '1!p' > /tmp/containers.txt
            while read field1 field2 field3; do
              echo -e "\n\nContainer name [$field2] with id [$field1] logs: \n\n"
              docker logs --tail=${NUMBER_OF_LINES_TO_LOG} -t ${field1}
            done < /tmp/containers.txt
    fi
    echo -e "\n\nPrinting docker compose logs - start\n\n"
    docker-compose -f "docker-compose-${WHAT_TO_TEST}.yml" logs || echo "Failed to print docker compose logs"
    echo -e "\n\nPrinting docker compose logs - end\n\n"
    tail_log "brewing"
    tail_log "zuul"
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
    for i in $( seq 1 "${RETRIES}" ); do
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
    for i in $( seq 1 "${RETRIES}" ); do
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
    echo ${pid} > ${APP_JAVA_PATH}/app.pid
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
    java_jar "zuul" "$1 $REMOTE_DEBUG=8993"
    java_jar "ingredients" "$1 $REMOTE_DEBUG=8994"
    java_jar "reporting" "$1 $REMOTE_DEBUG=8995"
    return 0
}

function kill_and_log() {
    kill -9 $(cat "$1"/build/libs/app.pid) && echo "Killed $1" || echo "Can't find $1 in running processes"
    pkill -f "$1" && echo "Killed $1 via pkill" ||  echo "Can't find $1 in running processes (tried with pkill)"
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
    if [[ -z "${CLOUD_FOUNDRY}" ]] ; then
            echo `pwd`
            kill_and_log "brewing"
            kill_and_log "zuul"
            kill_and_log "presenting"
            kill_and_log "ingredients"
            kill_and_log "reporting"
            kill_and_log "config-server"
            kill_and_log "eureka"
            kill_and_log "zookeeper"
            kill_and_log "zipkin-server"
            kill_all_apps_with_port
            if [[ -z "${KILL_NOW_APPS}" ]] ; then
                kill_docker
            fi
            pkill -15 -f JarLauncher || echo "No kafka was running"
        else
            reset "${CLOUD_PREFIX}-brewing" || echo "Failed to kill the app"
            reset "${CLOUD_PREFIX}-zuul" || echo "Failed to kill the app"
            reset "${CLOUD_PREFIX}-presenting" || echo "Failed to kill the app"
            reset "${CLOUD_PREFIX}-ingredients" || echo "Failed to kill the app"
            reset "${CLOUD_PREFIX}-reporting" || echo "Failed to kill the app"
            reset "${CLOUD_PREFIX}-zipkin-server" || echo "Failed to kill the app"
            reset "${CLOUD_PREFIX}-zipkin-web" || echo "Failed to kill the app"
            reset "${CLOUD_PREFIX}-discovery" || echo "Failed to kill the app"
            yes | cf delete-service "${CLOUD_PREFIX}-config-server" -f || echo "Failed to kill the app"
            reset "${CLOUD_PREFIX}-config-server" || echo "Failed to kill the app"
            yes | cf delete-service "${CLOUD_PREFIX}-discovery" -f || echo "Failed to kill the app"
            yes | cf delete-orphaned-routes || echo "Failed to delete routes"
    fi
    return 0
}

# Kills all docker related elements
function kill_docker() {
    docker kill $(docker ps -q) || echo "No running docker containers are left"
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

function print_usage() {
cat <<EOF

USAGE:

You can use the following options:

GLOBAL:
-t  |--whattotest  - define what you want to test (i.e. ZOOKEEPER, SLEUTH, EUREKA, CONSUL, SCS)
-v  |--version - which version of BOM do you want to use? Defaults to Finchley snapshot
-sv |--scsversion - which version of BOM for Spring Cloud Services do you want to use? Defaults to 1.3.2.BUILD-SNAPSHOT
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
-b  |--bootversion - Which version of Boot should be used? Defaults to 1.5.9.RELEASE for the plugin and to boot version used by libs
-cli|--cliversion - which version of Spring Cloud CLI should be used (it's used to start Kafka)? Defaults to 1.2.3.RELEASE
-ve |--verbose - Will print all library versions
-br |--branch - Which repo branch of the brewery repo should be checked out. Defaults to "master"

CLOUD FOUNDRY RELATED PROPERTIES:
-c  |--usecloudfoundry - should run tests for cloud foundry? (works only for SLEUTH) Defaults to "no"
-cd |--cloudfoundrydomain - what's the domain of your cloud foundry? Defaults to "run.pivotal.io"
-cu |--username - username to log in with to CF
-cp |--password - password to log in with to CF
-cpr|--cloudfoundryprefix - provides the prefix to the brewery app name. Defaults to 'brewery'
-cs |--space - provides the space for Cloud Foundry. Defaults to 'brewery'
-co |--org - provides the prefix to the brewery app name. Defaults to 'brewery'

EOF
}

# ======================================= FUNCTIONS END =======================================


# ======================================= VARIABLES START =======================================
CURRENT_DIR=`pwd`
REPO_URL="${REPO_URL:-https://github.com/spring-cloud-samples/brewery.git}"
if [[ -d acceptance-tests ]]; then
  REPO_LOCAL="${REPO_LOCAL:-.}"
else
  REPO_LOCAL="${REPO_LOCAL:-brewery}"
fi
WAIT_TIME="${WAIT_TIME:-5}"
RETRIES="${RETRIES:-70}"
DEFAULT_VERSION="${DEFAULT_VERSION:-Finchley.BUILD-SNAPSHOT}"
DEFAULT_HEALTH_HOST="${DEFAULT_HEALTH_HOST:-127.0.0.1}"
DEFAULT_NUMBER_OF_LINES_TO_LOG="${DEFAULT_NUMBER_OF_LINES_TO_LOG:-1000}"
SHOULD_START_RABBIT="${SHOULD_START_RABBIT:-yes}"
JAVA_PATH_TO_BIN="${JAVA_HOME}/bin/"
if [[ -z "${JAVA_HOME}" ]] ; then
    JAVA_PATH_TO_BIN=""
fi
LOCALHOST="127.0.0.1"
MEM_ARGS="-Xmx128m -Xss1024k"
CLOUD_PREFIX="brewery"
DEFAULT_SCS_VERSION=""
SLEEP_TIME_FOR_DISCOVERY="${SLEEP_TIME_FOR_DISCOVERY:-90}"

BOOT_VERSION_PROP_NAME="BOOT_VERSION"
BOM_VERSION_PROP_NAME="BOM_VERSION"
SCS_BOM_VERSION_PROP_NAME="SCS_VERSION"
BOOT_VERSION_PROP_NAME="BOOT_VERSION"

DEFAULT_ORG="${DEFAULT_ORG:-brewery}"
DEFAULT_SPACE="${DEFAULT_SPACE:-scs}"

# ======================================= VARIABLES END =======================================


# ======================================= PARSING ARGS START =======================================
if [[ $# == 0 ]] ; then
    print_usage
    exit 0
fi

while [[ $# > 0 ]]
do
key="$1"
case ${key} in
    -t|--whattotest)
    WHAT_TO_TEST="$2"
    shift # past argument
    ;;
    -v|--version)
    VERSION="$2"
    shift # past argument
    ;;
    -sv|--scsversion)
    SCS_VERSION="$2"
    shift # past argument
    ;;
    -h|--healthhost)
    HEALTH_HOST="$2"
    shift # past argument
    ;;
    -l|--numberoflines)
    NUMBER_OF_LINES_TO_LOG="$2"
    shift # past argument
    ;;
    -r|--reset)
    RESET="yes"
    ;;
    -ke|--killattheend)
    KILL_AT_THE_END="yes"
    ;;
    -n|--killnow)
    KILL_NOW="yes"
    ;;
    -na|--killnowapps)
    KILL_NOW="yes"
    KILL_NOW_APPS="yes"
    ;;
    -x|--skiptests)
    NO_TESTS="yes"
    ;;
    -s|--skipbuilding)
    SKIP_BUILDING="yes"
    ;;
    -c|--usecloudfoundry)
    CLOUD_FOUNDRY="true"
    ;;
    -cd|--cloudfoundrydomain)
    DOMAIN="$2"
    shift # past argument
    ;;
    -a|--deployonlyapps)
    DEPLOY_ONLY_APPS="yes"
    ;;
    -d|--skipdeployment)
    SKIP_DEPLOYMENT="yes"
    ;;
    -cu|--username)
    USERNAME="$2"
    shift # past argument
    ;;
    -cp|--password)
    PASSWORD="$2"
    shift # past argument
    ;;
    -cpr|--cloudfoundryprefix)
    CLOUD_PREFIX="$2"
    shift # past argument
    ;;
    -cs|--space)
    CLOUD_SPACE="$2"
    shift # past argument
    ;;
    -co|--org)
    CLOUD_ORG="$2"
    shift # past argument
    ;;
    -k|--kafka)
    KAFKA="yes"
    ;;
    -b|--bootversion)
    BOOT_VERSION="$2"
    shift
    ;;
    -cli|--cliversion)
    CLI_VERSION="$2"
    shift
    ;;
    -br|--branch)
    REPO_BRANCH="$2"
    shift
    ;;
    -ve|--verbose)
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
[[ -z "${SCS_VERSION}" ]] && SCS_VERSION="${DEFAULT_SCS_VERSION}"
[[ -z "${HEALTH_HOST}" ]] && HEALTH_HOST="${DEFAULT_HEALTH_HOST}"
[[ -z "${NUMBER_OF_LINES_TO_LOG}" ]] && NUMBER_OF_LINES_TO_LOG="${DEFAULT_NUMBER_OF_LINES_TO_LOG}"
[[ -z "${DOMAIN}" ]] && DOMAIN="run.pivotal.io"
[[ -z "${CLOUD_SPACE}" ]] && CLOUD_SPACE="${DEFAULT_SPACE}"
[[ -z "${CLOUD_ORG}" ]] && CLOUD_ORG="${DEFAULT_ORG}"
[[ -z "${REPO_BRANCH}" ]] && REPO_BRANCH="master"

CLOUD_DOMAIN=${DOMAIN}
CLOUD_TARGET=api.${DOMAIN}

HEALTH_PORTS=('9991' '9992' '9993' '9994' '9995')
HEALTH_ENDPOINTS="$( printf "http://${LOCALHOST}:%s/health " "${HEALTH_PORTS[@]}" )"
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
CLI_VERSION=${CLI_VERSION}

CLOUD FOUNDRY PROPS:

SCS_VERSION=${SCS_VERSION}
CLOUD_FOUNDRY=${CLOUD_FOUNDRY}
CLOUD_PREFIX=${CLOUD_PREFIX}
CLOUD_ORG=${CLOUD_ORG}
CLOUD_SPACE=${CLOUD_SPACE}

EOF

# ======================================= PARSING ARGS END =======================================

# ======================================= EXPORTING VARS START =======================================
export WHAT_TO_TEST=${WHAT_TO_TEST}
export VERSION=${VERSION}
export SCS_VERSION=${SCS_VERSION}
export HEALTH_HOST=${HEALTH_HOST}
export WAIT_TIME=${WAIT_TIME}
export RETRIES=${RETRIES}
export BOOT_VERSION_PROP_NAME=${BOOT_VERSION_PROP_NAME}
export BOM_VERSION_PROP_NAME=${BOM_VERSION_PROP_NAME}
export SCS_BOM_VERSION_PROP_NAME=${SCS_BOM_VERSION_PROP_NAME}
export NUMBER_OF_LINES_TO_LOG=${NUMBER_OF_LINES_TO_LOG}
export KILL_AT_THE_END=${KILL_AT_THE_END}
export KILL_NOW_APPS=${KILL_NOW_APPS}
export LOCALHOST=${LOCALHOST}
export MEM_ARGS=${MEM_ARGS}
export SHOULD_START_RABBIT=${SHOULD_START_RABBIT}
export ACCEPTANCE_TEST_OPTS=${ACCEPTANCE_TEST_OPTS}
export CLOUD_FOUNDRY=${CLOUD_FOUNDRY}
export DEPLOY_ONLY_APPS=${DEPLOY_ONLY_APPS}
export SKIP_DEPLOYMENT=${SKIP_DEPLOYMENT}
export CLOUD_PREFIX=${CLOUD_PREFIX}
export JAVA_PATH_TO_BIN=${JAVA_PATH_TO_BIN}
export KAFKA=${KAFKA}
export DEFAULT_HEALTH_HOST=${DEFAULT_HEALTH_HOST}
export CLOUD_ORG=${CLOUD_ORG}
export CLOUD_SPACE=${CLOUD_SPACE}
export USERNAME=${USERNAME}
export PASSWORD=${PASSWORD}
export BOOT_VERSION=${BOOT_VERSION}
export VERBOSE=${VERBOSE}
export CLI_VERSION=${CLI_VERSION:-1.3.2.RELEASE}

export -f login
export -f app_domain
export -f deploy_app
export -f deploy_zookeeper_app
export -f deploy_app_with_name
export -f deploy_app_with_name_parallel
export -f deploy_service
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

# ======================================= EXPORTING VARS END =======================================

# ======================================= Kill all apps and exit if switch set =======================================
if [[ ${KILL_NOW} ]] ; then
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
CURRENT_DIR=`pwd`

# ======================================= Building the apps =======================================
echo -e "\n\nUsing the following gradle.properties"
cat gradle.properties

echo -e "\n\n"

# Build the apps
APP_BUILDING_RETRIES=3
APP_WAIT_TIME=1
APP_FAILED="yes"
WORK_OFFLINE="${WORK_OFFLINE:-false}"

if [[ "${CLOUD_FOUNDRY}" == "true" ]] ; then
    WHAT_TO_TEST="SLEUTH"
fi

PARAMS="--no-daemon";
if [[ "${WORK_OFFLINE}" == "false" ]]; then
    PARAMS="${PARAMS} --refresh-dependencies";
else
    PARAMS="${PARAMS} --offline";
fi
if [[ "${BOOT_VERSION}" != "" ]] ; then
    echo "Will use Boot in version [${BOOT_VERSION}]"
    PARAMS="${PARAMS} -D${BOOT_VERSION_PROP_NAME}=${BOOT_VERSION}"
fi
if [[ "${VERSION}" != "" ]] ; then
    echo "Will use BOM in version [${VERSION}]"
    PARAMS="${PARAMS} -D${BOM_VERSION_PROP_NAME}=${VERSION}"
fi
if [[ "${SCS_VERSION}" != "" ]] ; then
    echo "Will use SCS in version [${SCS_VERSION}]"
    PARAMS="${PARAMS} -D${SCS_BOM_VERSION_PROP_NAME}=${SCS_VERSION}"
fi
echo -e "\n\nPassing following Gradle parameters [${PARAMS}]\n\n"

if [[ -z "${SKIP_BUILDING}" ]] ; then
    if [[ "${KAFKA}" == "yes" ]] ; then
        echo "Will use Kafka as a message broker"
        PARAMS="${PARAMS} -Pkafka"
    fi
    for i in $( seq 1 "${APP_BUILDING_RETRIES}" ); do
          ./gradlew clean ${PARAMS} --parallel
          if [[ "${VERBOSE}" == "yes" ]] ; then
            echo -e "\n\nPrinting the dependency tree for all projects\n\n"
            ./gradlew allDeps
          fi
          ./gradlew build ${PARAMS} && APP_FAILED="no" && break
          echo "Fail #$i/${APP_BUILDING_RETRIES}... will try again in [${APP_WAIT_TIME}] seconds"
    done
else
    APP_FAILED="no"
fi

if [[ "${APP_FAILED}" == "yes" ]] ; then
    echo -e "\n\nFailed to build the apps!"
    exit 1
fi


# ======================================= Deploying apps locally or to cloud foundry =======================================
if [[ "${CLOUD_FOUNDRY}" == "true" ]] ; then
    login ${USERNAME} ${PASSWORD}
fi

INITIALIZATION_FAILED="yes"
if [[ -z "${CLOUD_FOUNDRY}" &&  "${WHAT_TO_TEST}" == "SCS" ]] ; then
    echo -e "You have to pass the CF flag (-c) - you can't test SCS without it"
    exit 1
elif [[ -z "${CLOUD_FOUNDRY}" ]] ; then
    if [[ -z "${SKIP_DEPLOYMENT}" ]] ; then
        . ./docker-compose-${WHAT_TO_TEST}.sh && INITIALIZATION_FAILED="no"
    else
      INITIALIZATION_FAILED="no"
    fi
else
    . ./cloud-foundry-${WHAT_TO_TEST}.sh && INITIALIZATION_FAILED="no"
fi

if [[ "${INITIALIZATION_FAILED}" == "yes" ]] ; then
    echo -e "\n\nFailed to initialize the apps!"
    print_logs
    kill_all_apps_if_switch_on
    exit 1
fi

# ======================================= Checking if apps are booted =======================================
if [[ -z "${CLOUD_FOUNDRY}" ]] ; then

        if [[ -z "${SKIP_DEPLOYMENT}" ]] ; then
            # Wait for the apps to boot up
            APPS_ARE_RUNNING="no"

            echo -e "\n\nWaiting for the apps to boot for [$(( WAIT_TIME * RETRIES ))] seconds"
            for i in $( seq 1 "${RETRIES}" ); do
                sleep "${WAIT_TIME}"
                curlResult="$( curl --fail -m 5 ${HEALTH_ENDPOINTS} || echo "DOWN" )"
                echo "${curlResult}" | grep -v DOWN && APPS_ARE_RUNNING="yes" && break
                echo "Fail #$i/${RETRIES}... will try again in [${WAIT_TIME}] seconds"
            done

            if [[ "${APPS_ARE_RUNNING}" == "no" ]] ; then
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
else
    if [[ "${WHAT_TO_TEST}" == "SCS" ]] ; then
        READY_FOR_TESTS="yes"
    else
        DISCOVERY_HOST_NAME="${CLOUD_PREFIX}-discovery"
        DISCOVERY_HOST=`app_domain ${DISCOVERY_HOST_NAME}`
        echo "Resolved discovery host for discovery with name [${DISCOVERY_HOST_NAME}] is [${DISCOVERY_HOST}]"
        echo -e "\n\nChecking for the presence of all services in Service Discovery for [$(( WAIT_TIME * RETRIES ))] seconds"
        for i in $( seq 1 "${RETRIES}" ); do
            sleep "${WAIT_TIME}"
            CURL_RESULT=$( curl --fail -m 5 http://${DISCOVERY_HOST}/eureka/apps/ )
            echo "${CURL_RESULT}" | grep PRESENTING && PRESENTING_PRESENT="yes"
            echo "${CURL_RESULT}" | grep BREWING && BREWING_PRESENT="yes"
            echo "${CURL_RESULT}" | grep ZUUL && ZUUL_PRESENT="yes"
            echo "${CURL_RESULT}" | grep INGREDIENTS && INGREDIENTS_PRESENT="yes"
            echo "${CURL_RESULT}" | grep REPORTING && REPORTING_PRESENT="yes"
            if [[ "${PRESENTING_PRESENT}" == "yes" && "${BREWING_PRESENT}" == "yes" && "${INGREDIENTS_PRESENT}" == "yes" && "${REPORTING_PRESENT}" == "yes"  && "${ZUUL_PRESENT}" == "yes" ]]; then READY_FOR_TESTS="yes" && break; fi
            echo "Fail #$i/${RETRIES}... will try again in [${WAIT_TIME}] seconds"
        done
    fi

    if [[ "${READY_FOR_TESTS}" == "no" ]] ; then
        echo -e "\n\nThe apps failed to register in Service Discovery!"
        print_logs
        kill_all_apps_if_switch_on
        exit 1
    fi
fi

# ======================================= Running acceptance tests =======================================
TESTS_PASSED="no"

if [[ ${NO_TESTS} ]] ; then
    echo -e "\nSkipping end to end tests"
    kill_all_apps_if_switch_on
    exit 0
fi

if [[ "${READY_FOR_TESTS}" == "yes" ]] ; then
    echo -e "\n\nSuccessfully booted up all the apps. Proceeding with the acceptance tests"
    echo -e "\n\nRunning acceptance tests with the following parameters [-DWHAT_TO_TEST=${WHAT_TO_TEST} ${ACCEPTANCE_TEST_OPTS}]"
    ./gradlew ${PARAMS} :acceptance-tests:acceptanceTests "-DWHAT_TO_TEST=${WHAT_TO_TEST}" ${ACCEPTANCE_TEST_OPTS} --stacktrace --no-daemon --configure-on-demand && TESTS_PASSED="yes"
fi

if [[ "${TESTS_PASSED}" == "yes" ]] ; then
    TESTS_PASSED="no"
    echo -e "\n\nTests passed - now checking that there are no ExceptionUtils from Sleuth logs (this means that we broke sth with context passing)"
    grep ExceptionUtils build/*.log && echo "There are ExceptionUtils entries. That's not good..." || TESTS_PASSED="yes"
fi

# Check the result of tests execution
if [[ "${TESTS_PASSED}" == "yes" ]] ; then
    echo -e "\n\nTests passed successfully."
    kill_all_apps_if_switch_on
    exit 0
else
    echo -e "\n\nTests failed..."
    print_logs
    kill_all_apps_if_switch_on
    exit 1
fi
