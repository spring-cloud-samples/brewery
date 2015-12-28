# Brewery Acceptance Tests 

Tests that check various Spring Cloud functionalities on the Brewery project.

## What are we testing?

### Service Discovery 

Do applications talk to each other properly via:

- Zookeeper
- Eureka
- Consul 

### Tracing 

Does request instrumentalization work properly with:

- Sleuth with Zipkin
- Sleuth Stream with Zipkin

In tracing approach we're checking the following integrations:

- WebAsyncTask returning Controller's methods
- Explicit TraceCommand calls
- AsyncRestTemplate with @LoadBalanced RestTemplate
- @Async annotated methods
- CompletableFuture.supplyAsync(...) with TraceableExecutorService
- Controllers with Callable returning methods
- Javanica (@HystrixCommand annotated methods)

## How to run it?

This project is automatically ran from the Brewery root project via a bash script during PR builds. Since this
is a normal Gradle project, you can run the tests by simply passing Gradle commands. Check the configuration section
for more info on how to parametrize the tests.

When ran in a PR build acceptance tests are placed in a docker container so that they can access the 
Docker Virtual Network. Only then will the tests be able to find and successfully call the Presenting 
service via service discovery tool.

## Configuration

Check the `io.spring.cloud.samples.brewery.acceptance.common.WhatToTest` to see what exactly you can test. 
In order to execute the those tests you have to provide a system parameter `WHAT_TO_TEST` with the value
from that enum. E.g.

```
./gradlew test -DWHAT_TO_TEST=SLEUTH
```

### Parameters

In addition to this you can provide a couple of more parameters:

```
# will point to the desired URL to find the all services (e.g. http://192.168.99.100), Useful when working
# with docker-machine
-DLOCAL_URL=http://192.168.99.100
```