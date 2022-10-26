#!/bin/bash
# openfeign
# spring-cloud-openfeign

set -o errexit

mkdir -p build
pushd build
    git clone https://github.com/marcingrzejszczak/feign.git || echo "Project already cloned"
    pushd feign
        git fetch
        git checkout micrometerObservations || echo "Already checked out"
        git reset --hard origin/micrometerObservations
        ./mvnw clean install -DskipTests -T 4
    popd
#    git clone https://github.com/marcingrzejszczak/spring-framework.git
#    pushd spring-framework || echo "Project already cloned"
#    git fetch
#        git checkout gh-29241  || echo "Already checked out"
#        git reset --hard origin/gh-29241
#        ./gradlew pTML -x test -x javadoc -x dokkaHtmlPartial -x api
#    popd
#    git clone https://github.com/marcingrzejszczak/spring-boot.git
#    pushd spring-boot || echo "Project already cloned"
#    git fetch
#        git checkout observability  || echo "Already checked out"
#        git reset --hard origin/observability
#        ./gradlew build pTML -x test -x asciidoctor -x asciidoctorPdf -x checkstyleNohttp -x intTest -x zip
#    popd
    git clone https://github.com/spring-cloud/spring-cloud-openfeign.git
    pushd spring-cloud-openfeign || echo "Project already cloned"
    git fetch
        git checkout micrometerObservationsViaFeignCapabilities  || echo "Already checked out"
        git reset --hard origin/micrometerObservationsViaFeignCapabilities
        ./mvnw clean install -DskipTests -T 4
    popd
#    git clone https://github.com/marcingrzejszczak/datasource-micrometer.git
#    pushd datasource-micrometer || echo "Project already cloned"
#    git fetch
#        git checkout observation  || echo "Already checked out"
#        git reset --hard origin/observation
#        ./mvnw clean install -DskipTests -T 4
#    popd
popd
