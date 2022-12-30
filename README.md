- [About](#about)
- [Usage](#usage)
- [Development](#development)
  - [Building](#building)
  - [Testing](#testing)

# About

`appmap-java` is a Java agent for recording
[AppMaps](https://github.com/getappmap/appmap) of your code. "AppMap" is a data
format which records code structure (modules, classes, and methods), code
execution events (function calls and returns), and code metadata (repo name,
repo URL, commit SHA, labels, etc). It's more granular than a performance
profile, but it's less granular than a full debug trace. It's designed to be
optimal for understanding the design intent and structure of code and key data
flows.

# Usage

Visit the [AppMap for Java](https://appmap.io/docs/reference/appmap-java.html)
reference page on AppLand.com for a complete reference guide.

# Development

[![Build Status](https://travis-ci.com/applandinc/appmap-java.svg?branch=master)](https://travis-ci.com/applandinc/appmap-java)

The [Spring PetClinic](https://github.com/spring-projects/spring-petclinic)
provides a convenient way to develop on `appmap-java`.

Obtain the `spring-petclinic` JAR file, and launch it with the AppLand Java
agent:

```sh
export PETCLINIC_DIR=<path-to-petclinic>
java -Dappmap.debug \
  -javaagent:build/libs/appmap.jar \
  -Dappmap.config.file=test/appmap.yml \
  -jar $(PETCLINIC_DIR)/target/spring-petclinic-2.2.0.BUILD-SNAPSHOT.jar
```

You can use Java remote debug settings to attach a debugger:

```sh
export PETCLINIC_DIR=<path-to-petclinic>
java -Dappmap.debug \
  -javaagent:build/libs/appmap.jar \
  -Dappmap.config.file=test/appmap.yml \
  -Xdebug \
  -Xrunjdwp:server=y,transport=dt_socket,address=5005,suspend=y \
  -jar $PETCLINIC_DIR/target/spring-petclinic-2.2.0.BUILD-SNAPSHOT.jar
```

## Building

Artifacts will be written to `build/libs`. Use `appmap.jar` as your agent.

```sh
./gradlew build
```

## Testing


## Unit tests

Unit tests are run with `./gradlew test`.

## Integration tests

The integration tests use [bats](https://github.com/sstephenson/bats) and Docker.

Run the following command:

```sh
./bin/test
```

To get an interactive shell to write/debug the tests, try:

```shell
./bin/test /bin/bash
```

Once in the shell, run:

```shell
DEBUG_JSON=true bats --tap /test/*.bats
```

Or:

```shell
DEBUG_JSON=true bats --tap /test/*.bats -f 'http'
```
