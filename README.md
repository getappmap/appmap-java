# appmap-java
## Testing
Docker is required to run integration tests. Run the following command:
```
$ ./bin/test
```

## Running
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

