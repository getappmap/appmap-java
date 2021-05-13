# AppMap for Java

- [AppMap for Java](#appmap-for-java)
  - [About](#about)
    - [Supported versions](#supported-versions)
  - [Configuration](#configuration)
  - [Running the AppMap agent](#running-the-appmap-agent)
    - [Maven](#maven)
    - [Other than Maven](#other-than-maven)
      - [Gradle](#gradle)
  - [System Properties](#system-properties)
  - [Operation](#operation)
    - [Recording test cases](#recording-test-cases)
    - [Remote recording](#remote-recording)
      - [`GET /_appmap/record`](#get-_appmaprecord)
      - [`POST /_appmap/record`](#post-_appmaprecord)
      - [`DELETE /_appmap/record`](#delete-_appmaprecord)
  - [Developing](#developing)
  - [Building](#building)
  - [Testing](#testing)
- [Visual Studio Code integration](#visual-studio-code-integration)
- [Uploading AppMaps](#uploading-appmaps)

## About

`appmap-java` is a Java agent for recording
[AppMaps](https://github.com/applandinc/appmap) of your code. "AppMap" is a data
format which records code structure (modules, classes, and methods), code
execution events (function calls and returns), and code metadata (repo name,
repo URL, commit SHA, labels, etc). It's more granular than a performance
profile, but it's less granular than a full debug trace. It's designed to be
optimal for understanding the design intent and structure of code and key data
flows.

There are several ways to record AppMaps of your Java program using the `appmap`
agent:

- Run your tests (JUnit, TestNG) with the Java agent. An AppMap will be
  generated for each test.
- Run your application server with AppMap remote recording enabled, and use the
  [AppLand browser extension](https://github.com/applandinc/appland-browser-extension)
  to start, stop, and upload recordings.
- Record the code with `AppMap.record` API, which returns JSON containing the
  code execution trace.

Once you have made a recording, there are two ways to view automatically
generated diagrams of the AppMaps.

The first option is to load the diagrams directly in your IDE, using the
[AppMap for Visual Studio Code](https://marketplace.visualstudio.com/items?itemName=appland.appmap).

The second option is to upload them to the [AppLand server](https://app.land)
using the [AppLand CLI](https://github.com/applandinc/appland-cli/releases).

### Supported versions

- JDK 8+
- JUnit, TestNG

## Configuration

When you run your program, the agent reads configuration settings from
`appmap.yml`. Here's a sample configuration file for a typical Java project:

```yaml
# 'name' should generally be the same as the code repo name.
name: MyProject
packages:
  - path: com.mycorp.myproject
  - exclude: com.mycorp.myproject.MyClass#MyMethod
```

- **name** Provides the project name (required)
- **packages** A list of packages, classes and methods which should be
  instrumented.

**packages**

Each entry in the `packages` list is a YAML object which has the following keys:

- **path** Java packages, clases and methods that will be included in the
  instrumentation.
- **exclude** A list of packages, classes and methods that will be ignored. By
  default, all included, classes and public methods are inspected.

## Running the AppMap agent

### Maven

We recommend using the
[AppMap Maven plugin](https://github.com/applandinc/appmap-maven-plugin#quickstart).

### Other than Maven

Download the latest release from
[https://github.com/applandinc/appmap-java/releases](https://github.com/applandinc/appmap-java/releases).

The recorder is run as a Java agent. Currently, it must be started along with
the JVM. This is typically done by passing the `-javaagent` argument to your
JVM. For example:

```sh
java -javaagent:lib/appmap.jar myapp.jar
```

#### Gradle

```groovy
test {
  jvmArgs "-javaagent:$rootDir/lib/appmap.jar"
}
```

## System Properties

- `appmap.config.file` Path to the `appmap.yml` config file. Default:
  _appmap.yml_
- `appmap.output.directory` Output directory for `.appmap.json` files. Default:
  `./tmp/appmap`
- `appmap.debug` Enable debug logging. Default: `null` (disabled)
- `appmap.event.valueSize` Specifies the length of a value string before
  truncation occurs. If set to `0`, truncation is disabled. Default: `1024`
- `appmap.recording.auto` Automatically begin recording at boot time. Default:
  `false`
- `appmap.recording.file` The file name of the automatic recording to be
  emitted. Note that the file name will still be prefixed by
  `appmap.output.directory`. Default: `$TIMESTAMP.appmap.json`
- `appmap.recording.name` Populates the `metadata.name` field of the AppMap.
  Default: `$TIMESTAMP`

## Operation

### Recording test cases

When running test cases with the agent attached to the JVM, methods marked with
JUnit's `@Test` annotation will be recorded. A new AppMap file will be created
for each unique test case.

To disable recording for a particular JUnit test (for example, a performance
test), list the class or methods under an `exclude` in appmap.yml.

### Remote recording

The agent will hook an existing servlet, serving HTTP requests to toggle
recording on and off. These routes are used by the
[AppLand browser extention](https://github.com/applandinc/appland-browser-extension).

#### `GET /_appmap/record`

Retreive the current recording status

**Status** `200`

**Body** _`application/json`_

```json
{
  "enabled": boolean
}
```

#### `POST /_appmap/record`

Start a new recording session

**Status** `200` If a new recording session was started successfully `409` If an
existing recording session was already in progess

**Body** _Empty_

#### `DELETE /_appmap/record`

Stop an active recording session

**Status** `200` If an active recording session was stopped successfully, and
the body contains AppMap JSON `404` If there was no active recording session to
be stopped

**Body** If successful, scenario data is returned.

_`application/json`_

```json
{
  "version": "1.x",
  "metadata": {},
  "classMap": [],
  "events": []
}
```

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

# Visual Studio Code integration

The
[AppMap for Visual Studio Code](https://marketplace.visualstudio.com/items?itemName=appland.appmap)
extension is a great way to onboard developers to new code, and troubleshoot
hard-to-understand bugs with visuals.

# Uploading AppMaps

[https://app.land](https://app.land) can be used to store, analyze, and share
AppMaps.

For instructions on uploading, see the documentation of the
[AppLand CLI](https://github.com/applandinc/appland-cli).
