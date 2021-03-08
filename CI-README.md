# Prerequisites


1. Every release master (person allowed to manage releases), should do following:
    1.1. Generate GPG pair for signing releases (please use password protection!)
See [Guide 1](https://www.gnupg.org/gph/en/manual/c14.html)  
also [Guide 2](https://www.redhat.com/sysadmin/creating-gpg-keypairs) 
1.2 Publish key on public servers via `gpg2 --keyserver hkp://pool.sks-keyservers.net --send-keys <KEYID>`
1.3. [Register sonatype account](https://issues.sonatype.org/secure/Signup!default.jspa)  
2. The same operations (sonatype account and published GPG identity) should be done for CI bot (please create password-protected key, as suggested by default)
3. [Create a New Project ticket, requesting new project/namespace to be created.](https://issues.sonatype.org/secure/CreateIssue.jspa?issuetype=21&pid=10134)
Mention all release masters and CI bot in a list of collaborators.
4. For CI bot, generate travis-compatible version of the GPG key (use gpg2 to export ascii-armored secret key into the file first) : 
`cat secret_key.ascii.gpg | python bin/multiline_input_to_travis_env.py > /tmp/travis_compatible_env_value.txt`
5. At least one account on https://hub.docker.com is needed, which credentials will be used to avoid hitting limits for anonymous docker pulls, often exceeded by Travis servers.
It is recommended to use separate CI robot account for security purposes, as credentials would be stored (encrypted) on Travis side.
 
# Configuration
## Travis 
Following environment variables need to be configured on a repository level:
1. `DOCKERHUB_USERNAME` _(visible)_ – any Docker Hub account, preferably CI bot’s.
2. `DOCKERHUB_PASSWORD` _(secret)_
3. `ORG_GRADLE_PROJECT_ossrhUsername` _(visible)_ – Sonatype username for CI bot
4. `ORG_GRADLE_PROJECT_ossrhPassword` _(secret)_
5. `ORG_GRADLE_PROJECT_signingKey` _(secret)_ – the exact contents of CI bot’s GPG key in a form suitable for Travis environment ( content of `/tmp/travis_compatible_env_value.txt`, generated above)
6. `ORG_GRADLE_PROJECT_signingPassword` _(secret)_ -- passphrase for bot's GPG key
7. `ORG_GRADLE_PROJECT_publishGroupId` _(optional, visible)_ – if releasing to the Maven namespace different from default `com.appland` 

## Developer environment  

Export variable `TRAVIS_BUILD` setting its value to the desired version number 
(or change hardcoded `defaultVersion` in a `build.gradle`) .

Export all variables listed in Travis configuration section except 
`DOCKERHUB_USERNAME`, `DOCKERHUB_PASSWORD`. 

For the variable `ORG_GRADLE_PROJECT_signingKey` use actual multiline key body 
(depending on your environment, setting up multiline variables could be 
challenging or not) . 

Following bash expression could be used to set up multiline variable in bash:
 
```
export  ORG_GRADLE_PROJECT_signingKey=<EXACT CONTENT OF /tmp/travis_compatible_env_value.txt>
```

Also, every variable which name starts with `ORG_GRADLE_PROJECT_<varname>` 
could be instead configured as gradle setting `<varname>` (prefix stripped)


If you omit `ossrhUsername` setting, publishing would be done locally into 
directory  `buid/repo`

If you omit `sighingKey` setting, signatures won’t be generated

# Artifacts

The default artifact name base (`appmap-agent`) could be changed 
via variable `ORG_GRADLE_PROJECT_publishArtifactId`

 

# Publishing 

## Manual 

`./gradlew publish`

## Travis 

Maven publication is triggered by semantic-versioned tags in the form `x.y.z`, 
`x.y.z-whatever` (releases) or `x.y.z-SNAPSHOT` (snapshots)

# Releases

Follow the steps in 
[Official guide](https://central.sonatype.org/pages/releasing-the-deployment.html) 
to manually close-validate-release the repository after successful publication.

*After first release in the namespace happens, do not forget to comment on the 
OSSRH ticket which was used to claim the namespace.*

# Backlog

## Done


* Generate Source Jar
* Generate JavaDoc Jar
* Publish to local repo
* Sign artifacts
* Plug together signing and publishing
* Fill POM metadata according to Maven requirements
* Parameterizable coordinates (version, group, artifact)
* Support Java8
* Release into real (remote, sonatype) repo
* BUG: POM contains prohibited syntax for mockito dependency
* Automate via Travis
* Regresion in Java8 test


## TODO:

* Support release cycle for appmap-java-maven-plugin
* BUG? Javadoc is intentionally empty due to errors in javadoc generation.
