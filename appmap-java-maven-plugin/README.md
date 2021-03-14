- [About](#about)
  - [Typical entry in pom.xml](#typical-entry-in-pomxml)
- [Plugin goals](#plugin-goals)
- [Plugin configuration](#plugin-configuration)
  - [Example](#example)
- [Running](#running)
- [Building](#building)

# About

The AppMap Maven Plugin provides simple method for recording AppMaps in running tests in Maven projects and a seamless integration into CI/CD pipelines. The recording agent requires `appmap.yml` configuration file, see [appmap-java](https://github.com/applandinc/appmap-java/blob/master/README.md) for details.

## Typical entry in pom.xml
```xml
<!-- this goes to the properties section -->
<appmap-java.version>0.5</appmap-java.version>

<!-- -snip- -->

<!-- the plugin element goes to plugins -->
<!-- AppMap Java agent, default parameters -->
<plugin>
    <groupId>com.appland</groupId>
    <artifactId>appmap-maven-plugin</artifactId>
    <version>${appmap-java.version}</version>
    <executions>
        <execution>
            <goals>
                <goal>prepare-agent</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

# Plugin goals
- `prepare-agent` : adds `appmap.jar` to JVM execution as javaagent

# Plugin configuration
- `configFile` Path to the `appmap.yml` config file. Default: _./appmap.yml_
- `outputDirectory` Output directory for `.appmap.json` files. Default: _./tmp/appmap_
- `skip` Agent won't record tests when set to true. Default: _false_ 
- `debug` Enable debug logging. Default: _disabled_
- `eventValueSize` Specifies the length of a value string before truncation occurs. If set to 0, truncation is disabled. Default: _1024_

## Configuration example
```xml
<!-- AppMap Java agent, all parameters -->
<plugin>
    <groupId>com.appland</groupId>
    <artifactId>appmap-maven-plugin</artifactId>
    <version>${appmap-java.version}</version>
        <configuration>
            <configFile>/mnt/builds/nightly/my-app/appmap.yml</configFile>
            <outputDirectory>/mnt/builds/nightly/my-app/dist/tmp/appmap</outputDirectory>
            <skip>false</skip>
            <debug>disabled</debug>
            <eventValueSize>1024</eventValueSize>
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
Add the plugin to `pom.xml` and run tests as usual. AppMaps will be recorded when the plugin is active and tests are run.

Alertnatively, you can run the tests with the AppMap agent with this command:
```shell
mvn com.appland:appmap-maven-plugin:prepare-agent test
```

# Building

Artifacts will be written to `target/` use `appmap-java-plugin-[VERSION].jar`. as your maven plugin.

```shell
mvn clean install
```
