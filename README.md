# appmap-java
## Building
Artifacts will be written to `build/libs`. Use `appmap-java-all.jar` as your agent.
```
$ ./gradlew build
```
## Testing
Docker is required to run integration tests. Run the following command:
```
$ ./bin/test
```

## Running
These are merely hints for running the java agent. More info to come.


**mvn**
```bash
$ mvn -DargLine="-javaagent:appmap-java.jar" test
```

**gradle**
```groovy
test {
  jvmArgs "-javaagent:appmap-java.jar"
}
```

**surefire**
```xml
<plugin>
  <artifactId>maven-surefire-plugin</artifactId>
  <configuration>
    <argLine>-javaagent:appmap-java.jar</argLine>
  </configuration>
<plugin>
```

## System Properties
`appmap.config.file`  
specify the path of an `appmap.yml` config file  
default: _appmap.yml_

`appmap.output.directory`  
specify the output directory of `appmap.json` files  
default: _./_


# Build status

[![Build Status](https://travis-ci.com/applandinc/appmap-java.svg?branch=master)](https://travis-ci.org/applandinc/appmap-java)


