# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.3.0] - 2020-07-08
### Fixed
- Removed a potential deadlock that could occur if the user's code required a lock in `toString`.
- Output directories will be recursively created

### Added
- `appmap.event.valueSize` can be used to specify the length of a value string before truncation
  occurs.
- `Recorder.record` will record and output the execution of a `Runnable`.

### Changed
- In `appmap.yml` a package item can now have an empty `path`, allowing the user to specify
  a list of exclusions only. This can be useful for excluding groups of tests not meant to
  be captured.
- The default output directory for appmap files is now `./appmap`. This directory will be
  created automatically if it does not exist.

## [0.2.1] - 2020-07-01
### Fixed
- `appmap.output.directory` will be created if it does not exist. Previously this resulted in a
  null pointer exception when recording.
- Parsing errors in `appmap.yml` will stop the application from running and emit a useful error.

## [0.2.0] - 2020-06-29
### Added
- Support for Jakarta

### Removed
- `javax.servlet` dependency

### Changed
- All logging is disabled by default. To enable logging use the `appmap.debug` system property.
- Loading times have been reduced significantly.

## [0.1.0] - 2020-06-05
### Added
- Name property added to appmap metadata
- HTTP request parameters are captured
- Spring path params are captured
- Spring normalized path info is captured
- Support for JUnit Jupiter

### Fixed
- Feature group metadata is now written to the correct key

## [0.0.2] - 2020-05-15
### Added
- `language` object in `metadata` now includes `version` and `engine` based on
  the Java runtime.
- `sql_query` events now include `database_type` as reported by the database
  driver.

## [0.0.1] - 2020-04-28
### Added
- Versioning has begun. The authoritative `version` is now declared in
  [`build.gradle`](build.gradle).
- Feature and feature group metadata fields are now written to scenarios
  captured from test cases.
