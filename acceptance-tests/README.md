# Brewery Acceptance Tests 

Tests that check various Spring Cloud functionalities on the Brewey project.

## What are we testing?

Service Discovery (do applications talk to each other properly)

- Zookeeper
- Eureka *
- Consul *

Tracing (does tracing work properly)

- Sleuth
- Zipkin *

(*) in progress

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

### Local test mode

If you don't want the acceptance tests to connect to a Zookeeper in a Docker container (you would have to run
the tests in a Docker container too) you can run the tests in a "local" mode.

```
./gradlew acceptance-tests:clean acceptance-tests:test -Dspring.profiles.active=local -DWHAT_TO_TEST=ZOOKEEPER
```

In addition to this you can provide a couple of more parameters:

```
# will by default point to http://localhost to find the presenting service
-DLOCAL
```

```
# will point to the desired URL to find the presenting service (e.g. http://192.168.99.100)
-DLOCAL_URL=http://192.168.99.100
```

```
# will point to a custom Zipkin Query server
-Dspring.zipkin.query.url=http://192.168.99.100:9411
```