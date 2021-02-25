AppLand AppMap Maven Plugin for Java
--------------------------------


- [Building](#building)
- [Agent Configuration](#agent-configuration)
- [Maven Plugin Configuration](#maven-plugin-config)
    - [Plugin Goals](#plugin-goals)
    - [Plugin configuration options](#plugin-configuration)
    - [Example](#example)


# Building
Artifacts will be written to `target/` use `appmap-java-plugin-[VERSION].jar`. as your maven plugin.
```bash
$ mvn clean install
```

# Agent Configuration
When you run your program, the agent reads configuration settings from `appmap.yml` by default.

Please read configuration options from [AppMap Java Agent README.md](../README.md)

# Maven Plugin Configuration

## Plugin goals
prepare-agent : adds appmap.jar to JVM execution as javaagent

## Plugin configuration options
outputDirectory (default: ./target/appmap/)
configFile (default: ./appmap.yml)
debug (enabled|disabled, default: disabled)
eventValueSize (integer, default 1024)
skip(Boolean, default false)

## Example plugin config in a standard POM.xml file
```xml
<!-- AppMap Java agent, default parameters -->
<plugin>
    <groupId>com.appland</groupId>
    <artifactId>appmap-maven-plugin</artifactId>
    <version>${appmap-java.version}</version>
        <configuration>
            <outputDirectory></outputDirectory>
            <configFile>appmap.yml</configFile>
            <debug>enabled</debug>
            <eventValueSize>1024</eventValueSize>
            <skip>false</skip>
        </configuration>
    <executions>
        <execution>
            <goals>
                <goal>prepare-agent</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```


# Running
To run the java agent with correct plugin configuration you only need to build your project as usual without skipping
the test goal.

```bash
$ mvn clean install
```
