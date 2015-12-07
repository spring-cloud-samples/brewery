# Brewery

Ever wanted to brew your beer using microservices? This repository will allow you to do so!

## How does the brewery work?

Since pictures say more than words...

![Diagram](img/Brewery.png)

### Presenting service (point of entry to the system)

- Go to the presenting service (http://localhost:9090) and order ingredients
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

├── aggregating (service that aggregates ingredients)
├── bottling    (service that bottles the beer)
├── common      (common code for the services)
├── gradle      (gradle related stuff)
├── img         (the fabulous diagram of the brewery)
├── maturing    (service that matures the beer)
├── presenting  (UI of the brewery)
└── zookeeper   (embedded zookeeper)

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

## Authors

The code is ported from https://github.com/uservices-hackathon

The authors of the code are:
- Marcin Grzejszczak (marcingrzejszczak)
- Tomasz Szymanski (szimano)
