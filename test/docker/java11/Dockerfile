FROM openjdk:11-jdk-slim AS build

RUN apt-get update \
    && apt-get install -y git

# Build the source from this repository
RUN mkdir -p /build

WORKDIR /appmap

ADD gradle              ./gradle
ADD gradlew             ./
ADD settings.gradle     ./
ADD build.gradle        ./
ADD src                 ./src
ADD buildSrc            ./buildSrc

RUN ./gradlew --no-daemon build  \
    && cd build/libs \
    && mv $(ls | grep 'appmap-[[:digit:]]') /build/appmap.jar

FROM openjdk:13-alpine
LABEL maintainer="Dustin Byrne <dustin@app.land>"

RUN apk add --no-cache -U bash curl git jq ncurses less

RUN git clone https://github.com/bats-core/bats-core.git \
    && git clone https://github.com/bats-core/bats-support.git /test/test_helper/bats-support \
    && git clone https://github.com/bats-core/bats-assert.git /test/test_helper/bats-assert \
    && cd bats-core \
    && ./install.sh /usr/local

COPY --from=petclinic:latest /build /
COPY --from=build /build /
COPY test/appmap.yml     /
COPY test/entrypoint     /sbin/
COPY test/*.bats         /test/
COPY test/Props.java     /test/
COPY test/helper.bash    /test/
COPY test/HttpClientTest.java /test/

ENTRYPOINT ["/sbin/entrypoint"]
