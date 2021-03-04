- [About](#about)
- [Plugin goals](#plugin-goals)
- [Plugin configuration](#plugin-configuration)
  - [Example](#example)
- [Running](#running)
- [Building](#building)

# About

The AppMap Maven Plugin provides simple method for recording AppMaps from tests in Maven projects, and a seamless integration into CI/CD pipelines. The recording agent requires `appmap.yml` configuration file, see [appmap-java](../README.md) for details.

```xml
<!-- AppMap Java agent, default parameters -->
<plugin>
    <groupId>com.appland</groupId>
    <artifactId>appmap-maven-plugin</artifactId>
    <version>${appmap-java.version}</version>
        <configuration>
            <configFile>/mnt/app-one/appmap.yml</configFile>
            <outputDirectory>/mnt/app-one/tmp/appmap</outputDirectory>
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

# Plugin goals
- `prepare-agent` : adds `appmap.jar` to JVM execution as javaagent

# Plugin configuration
- `configFile` Path to the `appmap.yml` config file. Default: _./appmap.yml_
- `outputDirectory` Output directory for `.appmap.json` files. Default: _./tmp/appmap_
- `skip` Agent won't record tests when set to true. Default: _false_ 
- `debug` Enable debug logging. Default: _disabled_
- `eventValueSize` Specifies the length of a value string before truncation occurs. If set to 0, truncation is disabled. Default: _1024_

## Example
```xml
<!-- AppMap Java agent, default parameters -->
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
Add the plugin to `pom.xml` and build the project as usual. AppMaps will be recorded when the plugin is active and tests are run.

```bash
$ mvn clean install
```

# Building

Artifacts will be written to `target/` use `appmap-java-plugin-[VERSION].jar`. as your maven plugin.

```bash
$ mvn clean install
```