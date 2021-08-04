# Overview

The ultimate goal of release process is publishing artifacts into [Maven Central repo](https://search.maven.org/search?q=com.appland%20appmap-agent)

Maven Central does not support direct uploads, instead it syncs from a number of partner repositories, one of which is [Sonatype OSSRH](https://ossindex.sonatype.org/component/pkg:maven/com.appland/appmap-agent)

## Release process from Maven perspective:

* **Build**:      Jar artifacts are built (in build environment or on dev machine)
* **Publishing**: Artifacts are uploaded to OSSRH, forming a (temporary) _"staging repository"_ -- a set of artifacts and metatata files with unique name/version.
* **Closing**:    Next step, explicitly taken by submitter (via Sonatype UI or API), which freezes _"staging repository"_ and triggers automated integrity checks on it
* **Releasing**:  Next step, explcitly taken by submitter (via Sonatype UI or API), which initiates **syncing of published and validated artifacts from OSSRH to Maven Central**. 
* **Dropping**:   After artifacts are "released" to Maven Central, _"staging repository"_ lifecycle is over and it should be deleted from OSSRH.

After releasing, package becomes available via [Maven Central search](https://search.maven.org) within few hours.

*See also: official Sonatype docs on [publication](https://central.sonatype.org/publish/publish-guide/) and [releasing](https://central.sonatype.org/publish/release/)*


## Release process from Appland perspective

* **[Travis](https://app.travis-ci.com/github/applandinc/appmap-java) build is triggered by Github push**:
    * Travis configuration parameters are described below, in "Configuration" section
    * Most params are exposed to underlying scripts as environment variables 
* **Travis build execution steps** (settings file: [travis.yml](./travis.yml)):
    * `script`: a number of tests is executed using [Gradle](https://gradle.org/). Settings file: [build.gradle](./build.gradle) 
    * `deploy`: further execution is passed to [Semantic Release](https://github.com/semantic-release/semantic-release) utility
* **Semantic Release execution steps** (settings file: [.releaserc.yml](./.releaserc.yml)):
    * `commit-analyzer, release-notes-generator, changelog`: new version number is figured out [from commits history](https://github.com/semantic-release/semantic-release#commit-message-format), changelog is updated
    * `release-replace-plugin`: file [build.gradle](./build.gradle) is patched with new version number
    * `git`: generated changes are stored as a new Git commit
    * `exec`: **Gradle command is run to build jar artifacts and publish them to OSSRH/Maven. See details below.**
    * `github`: changes are pushed to Github as a new Github release
* **Gradle build specifics** (settings file: [build.gradle](./build.gradle)):
    * Gradle targets are configured in [build.gradle](./build.gradle) and/or are provided by Gradle plugins included with directives `import`/`plugins`/`apply plugin`
    * Gradle command is invoked with the top-level targets as parameters (e.g. `publishToSonatype closeAndReleaseSonatypeStagingRepository`)
    * For the provided target, Gradle evaluates dependency graph (consisting of other targets), executes it in reverse order
* **Gradle build execution steps** (from the bottom to the top of dependency graph):
    * *parameterization*: build parameters such as maven package names, signing keys are evaluated (some from environment vars)
    * `shadowJar`: main jar file is built
    * `sourcesJar`, `mockJavadocJar`: additional jar files, [required by Maven standards](https://central.sonatype.org/publish/requirements/)
    * *publishing*: metadata files (with package name, version, description etc.) are generated
    * *signing*: artifacts are signed (*signing key body* is exposed to script via env variable)  
    * `publishToSonatype closeAndReleaseSonatypeStagingRepository` (targets provided by Gradle Nexus Publishing plugin): artifacts are published to OSSRH (see above), then a chain of `close`-`release`-`drop` actions is processed. 


# CI setup: Prerequisites


1. Every release master (person allowed to manage releases), should do following:
    1. Generate GPG pair for signing releases (please use password protection!)
        See [Guide 1](https://www.gnupg.org/gph/en/manual/c14.html)  
        also [Guide 2](https://www.redhat.com/sysadmin/creating-gpg-keypairs) 
    2. Publish key on public servers via `gpg2 --keyserver hkp://pool.sks-keyservers.net --send-keys <KEYID>`
    3. [Register sonatype account](https://issues.sonatype.org/secure/Signup!default.jspa)  
2. The same operations (sonatype account and published GPG identity) should be done for CI bot (please create password-protected key, as suggested by default)
3. [Create a New Project ticket, requesting new project/namespace to be created.](https://issues.sonatype.org/secure/CreateIssue.jspa?issuetype=21&pid=10134)
Mention all release masters and CI bot in a list of collaborators.
4. For CI bot, generate travis-compatible version of the GPG key (use gpg2 to export ascii-armored secret key into the file first) : 
`cat secret_key.ascii.gpg | python bin/multiline_input_to_travis_env.py > /tmp/travis_compatible_env_value.txt`
5. At least one account on https://hub.docker.com is needed, which credentials will be used to avoid hitting limits for anonymous docker pulls, often exceeded by Travis servers.
It is recommended to use separate CI robot account for security purposes, as credentials would be stored (encrypted) on Travis side.
 
# CI Configuration

## Travis 
Following environment variables should be configured on a repository level:
1. `DOCKERHUB_USERNAME` _(visible)_ – any Docker Hub account, preferably CI bot’s. Used with password to prevent Docker pulls throttling.
2. `DOCKERHUB_PASSWORD` _(secret)_
3. `GH_TOKEN` _(secret)_ -- Github token to be used for pushes done by Semantic Release
4. `ORG_GRADLE_PROJECT_ossrhUsername` _(visible)_ – Sonatype username for CI bot
5. `ORG_GRADLE_PROJECT_ossrhPassword` _(secret)_
6. `ORG_GRADLE_PROJECT_signingKey` _(secret)_ – the exact contents of CI bot’s GPG key in a form suitable for Travis environment ( content of `/tmp/travis_compatible_env_value.txt`, generated above)
7. `ORG_GRADLE_PROJECT_signingPassword` _(secret)_ -- passphrase for bot's GPG key

There is also a number of optional, non-secret settings, mostly used for unofficial builds

1. `ORG_GRADLE_PROJECT_publishGroupId`: Maven namespace (default: `com.appland`)
2. `ORG_GRADLE_PROJECT_publicationArtifactId`: Maven package name (default: `appmap-agent`)
3. `ORG_GRADLE_PROJECT_mavenRepo`: used to alter OSSRH url if non-default endpoint is used (default: `https://s01.oss.sonatype.org`, legacy: `https://oss.sonatype.org`)
4. `ORG_GRADLE_PROJECT_artifactDescription`: used to alter the package description for non-official builds, default is defined in [build.gradle](./build.gradle)

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

`semantic-release` invoked by Travis on master branch, 
triggers publishing task as a hook defined in `.releaserc.yml`


Since April 2021 we're using Gradle Nexus Publish plugin, 
old `./gradlew publish` command is deprecated 
and likely won't work in "local filesystem" mode any more 
(not tested and not needed).

Nexus Publishing plugin takes care not only of uploading artifacts 
(like previously used `maven-publish` plugin does)
but also automates 
[the procedures of "closing" and "releasing" repository in OSSRH](https://central.sonatype.org/pages/releasing-the-deployment.html) 

## Manual invocation of publication task:

```./gradlew publishToSonatype closeAndReleaseSonatypeStagingRepository```
(triggered by semantic-release, most complete flow)

or 

```./gradlew publishToSonatype closeReleaseSonatypeStagingRepository```
(more conservative, skips the step of pushing to Maven Central)

# Backlog

* BUG? Javadoc is intentionally empty due to errors in javadoc generation.
