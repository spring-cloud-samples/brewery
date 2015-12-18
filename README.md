[![Build Status](https://travis-ci.org/spring-cloud-samples/brewery.svg)](https://travis-ci.org/spring-cloud-samples/brewery)

# Brewery

Ever wanted to brew your beer using microservices? This repository will allow you to do so!

## How does the brewery work?

Since pictures say more than words...

![Diagram](img/Brewery.png)

### Presenting service (point of entry to the system)

- Go to the presenting service (http://localhost:9991) and order ingredients
- A request from the presenting service is sent to the aggregating service when order is placed
- A "PROCESS-ID" header is set and will be passed through each part of beer brewing

### Aggregating service

- Service contains a warehouse ("database") where is stores the ingredients
- Basing on the order placed it will contact the external services to retrieve the real ingredients
- You have to have all 4 ingredients reach their threshold (1000) to start maturing the beer
- Once the threshold is met the application sends a request to the maturing service
- Each time a request is sent to the aggregating service it returns as a response its warehouse state

### Maturing service

- It receives a request with ingredients needed to brew a beer
- The brewing process starts thanks to the `Thread.sleep` method
- Once it's done a request to the bottling service is sent with number of worts
- Presenting service is called to update the current status of the beer brewing process

### Bottling service

- Waits some time to bottle the beer
- Presenting service is called to update the current status of the beer brewing process

## Project structure

```
├── acceptance-tests (code containing acceptace-tests of brewery)
├── aggregating      (service that aggregates ingredients)
├── bottling         (service that bottles the beer)
├── common           (common code for the services)
├── eureka           (Eureka server needed for Eureka tests)
├── gradle           (gradle related stuff)
├── img              (the fabulous diagram of the brewery)
├── maturing         (service that matures the beer)
├── presenting       (UI of the brewery)
├── zipkin-server    (Zipkin Server for Sleuth Stream tests)
└── zookeeper        (embedded zookeeper)
```

## How to build it?

```
./gradlew clean build
```

## How to build one module?

E.g. `aggregating` module

```
./gradlew aggregating:clean aggregating:build
```

## How to run it?

### Using Docker

Create the Dockerfiles. By default Zipkin integration is disabled.

```
./gradlew clean docker --parallel
```

And run docker compose

```
docker-compose up
```

This will build and run all the apps from jars. Also Zookeeper will be set up automatically.

To kill containers just type

```
docker-compose kill -f
```

To remove the containers just type

```
docker-compose rm -f
```

### Using Gradle with embedded Zookeeper

To run it all without local Zipkin server and with an embedded Zookeeper server just execute:

```
./gradlew bootRun -Dspring.profiles.active=dev --parallel
```

Your logs will be visible in the console and in the respective `build/logs/application.log` folder.

## How to run a single module?

To run a single module just execute (e.g. `presenting` module):

```
./gradlew presenting:bootRun -Dspring.profiles.active=dev
```

## How to test it?

The easiest way is to:

* Create a symbolic link somewhere on your drive to the `acceptance-tests/scripts/runDockerAcceptanceTests.sh` file.
* You can execute that script with such options
    * `-t` what do you want to test (`SLEUTH`, `ZOOKEEPER` etc.)
    * `-v` in which version (`1.0.0.BUILD-SNAPSHOT`)
    * `-r` is brewery repo already in place and needs to be reset?
     
Once you run the script, the brewery app will be cloned, built with proper lib versions and proper tests
will be executed

## Authors

The code is ported from https://github.com/uservices-hackathon

The authors of the code are:
- Marcin Grzejszczak (marcingrzejszczak)
- Tomasz Szymanski (szimano)
