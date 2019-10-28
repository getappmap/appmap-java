FROM openjdk:12 AS build

# Build the Spring "Pet Clinic" sample application
WORKDIR /

RUN mkdir -p /build \
    && yum install -y git \
    && git clone https://github.com/spring-projects/spring-petclinic.git \
    && cd spring-petclinic \
    && ./mvnw package -Dmaven.test.skip=true \
    && mv target/*.jar /build/petclinic.jar

# Build the source from this repository
WORKDIR /appmap

ADD gradle              ./gradle/
ADD gradlew             ./
ADD settings.gradle.kts ./
ADD build.gradle.kts    ./
ADD src                 ./src/

RUN ./gradlew --no-daemon build-release \
    && mv build/libs/*.jar /build/appmap.jar

FROM openjdk:12-alpine
LABEL maintainer="Dustin Byrne <dustin@app.land>"

RUN apk add -U bash curl git jq \
    && git clone https://github.com/sstephenson/bats.git \
    && cd bats \
    && ./install.sh /usr/local

COPY --from=build /build /
COPY test/appmap.yml     /
COPY test/entrypoint     /sbin/
COPY test/*.bats         /tests/

ENTRYPOINT '/sbin/entrypoint'