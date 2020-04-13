AppLand AppMap Recorder for Java
--------------------------------

- [Building](#building)
- [Testing](#testing)
- [Running](#running)
  - [Other examples](#other-examples)
    - [Maven](#maven)
    - [Maven Surefire](#maven-surefire)
    - [Gradle](#gradle)
- [System Properties](#system-properties)
- [Operation](#operation)
  - [Recording test cases](#recording-test-cases)
  - [HTTP recording controls](#http-recording-controls)
    - [`GET /_appmap/record`](#get-appmaprecord)
    - [`POST /_appmap/record`](#post-appmaprecord)
    - [`DELETE /_appmap/record`](#delete-appmaprecord)
- [Developing](#developing)
- [Build status](#build-status)

# Building
Artifacts will be written to `build/libs`. Use `appmap.jar` as your agent.
```
$ ./gradlew build
```

# Testing
Unit tests are run via the `test` task.

Docker is required to run integration tests. Run the following command:

```
$ ./bin/test
```

# Running
The AppMap recorder is run as a Java agent. Currently, it must be started along with the JVM. This is typically done by passing the `-javaagent` argument to your JVM. 
For example:

```bash
$ java -javaagent:lib/appmap.jar myapp.jar
```

## Other examples

### Maven

```bash
$ mvn -DargLine="-javaagent:lib/appmap.jar" test
```

### Maven Surefire

```xml
<plugin>
  <artifactId>maven-surefire-plugin</artifactId>
  <configuration>
    <argLine>-javaagent:${session.executionRootDirectory}/lib/appmap.jar</argLine>
  </configuration>
<plugin>
```

### Gradle

```groovy
test {
  jvmArgs "-javaagent:$rootDir/lib/appmap.jar"
}
```

# System Properties

`appmap.config.file`  
specify the path of an `appmap.yml` config file  
default: _appmap.yml_

`appmap.output.directory`  
specify the output directory of `appmap.json` files  
default: _./_

# Operation

## Recording test cases
When running test cases with the agent attached to the JVM, methods marked with JUnit's `@Test` annotation will be recorded. 
A new AppMap file will be created for each unique test case.

To disable AppMap for a particular JUnit test (for example, a performance test), list the class or methods under an
`exclude` in appmap.yml.

## HTTP recording controls
AppMap will hook an existing servlet, serving HTTP requests to toggle recording on and off. These routes are used by the [AppLand browser extention](https://github.com/applandinc/appland-browser-extension).

### `GET /_appmap/record`
Retreive the current recording status

**Status**  
`200`

**Body**  
_`application/json`_  
```json
{ 
  "enabled": boolean
}
```

### `POST /_appmap/record`
Start a new recording session

**Status**  
`200` If a new recording session was started successfully  
`409` If an existing recording session was already in progess

**Body**  
_Empty_

### `DELETE /_appmap/record`
Stop an active recording session

**Status**  
`200` If an active recording session was stopped successfully, and the body contains AppMap JSON  
`404` If there was no active recording session to be stopped

**Body**  
If successful, scenario data is returned.  

_`application/json`_
```json
{
  "version": "1.x",
  "metadata": {},
  "classMap": [],
  "events": [],
}
```

# Developing

The [Spring PetClinic](https://github.com/spring-projects/spring-petclinic) provides a convenient way to develop on `appmap-java`. 

Obtain the `spring-petclinic` JAR file, and launch it with the AppMap Java agent:

```shell script
$ export PETCLINIC_DIR=<path-to-petclinic>
$ java -Dappmap.debug \
  -javaagent:build/libs/appmap.jar \
  -Dappmap.config.file=test/appmap.yml \
  -jar $(PETCLINIC_DIR)/target/spring-petclinic-2.2.0.BUILD-SNAPSHOT.jar
```

You can use Java remote debug settings to attach a debugger:

```shell script
$ export PETCLINIC_DIR=<path-to-petclinic>
$ java -Dappmap.debug \
  -javaagent:build/libs/appmap.jar \
  -Dappmap.config.file=test/appmap.yml \
  -Xdebug \
  -Xrunjdwp:server=y,transport=dt_socket,address=5005,suspend=y \
  -jar $PETCLINIC_DIR/target/spring-petclinic-2.2.0.BUILD-SNAPSHOT.jar
```

# Build status

[![Build Status](https://travis-ci.com/applandinc/appmap-java.svg?branch=master)](https://travis-ci.org/applandinc/appmap-java)
