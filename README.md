# AppMap for Java

## Installation, configuration and usage

Visit [https://appland.com/docs/reference/appmap-java](https://appland.com/docs/reference/appmap-java)
for quickstart instructions and full documentation.

## Developing

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

Unit tests are run via the `test` task.

Docker is required to run integration tests. Run the following command:

```sh
./bin/test
```
