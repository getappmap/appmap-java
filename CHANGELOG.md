## [1.0.3](https://github.com/applandinc/appmap-java/compare/v1.0.2...v1.0.3) (2021-03-22)


### Bug Fixes

* Allow classes compiled without locals to be hooked ([0e0a0d3](https://github.com/applandinc/appmap-java/commit/0e0a0d333bf1b3323492765aa29df064a1de027d))
* Capture exceptions thrown from SQL interfaces ([9d1e66f](https://github.com/applandinc/appmap-java/commit/9d1e66fe7254bb262bf6b1ab2b8156d1bd1fee77))

## [1.0.2](https://github.com/applandinc/appmap-java/compare/v1.0.1...v1.0.2) (2021-03-18)


### Bug Fixes

* improve path and package resolution ([#62](https://github.com/applandinc/appmap-java/issues/62)) ([c3ba3df](https://github.com/applandinc/appmap-java/commit/c3ba3df5286cba6efd929e6cdcf583ebc28b96b2))

## [1.0.1](https://github.com/applandinc/appmap-java/compare/v1.0.0...v1.0.1) (2021-03-17)


### Bug Fixes

* disable http client requests ([#60](https://github.com/applandinc/appmap-java/issues/60)) ([2131d82](https://github.com/applandinc/appmap-java/commit/2131d822d8026900f00bacac7ccb8b146c65d464))

# 1.0.0 (2021-03-17)


### Features

* AppMap Maven plugin ([#46](https://github.com/applandinc/appmap-java/issues/46)) ([1798df2](https://github.com/applandinc/appmap-java/commit/1798df254f76155bdae9d74ff1f32b7be6f1d15b))
* appmap.yml errors are logged to stderr ([e746253](https://github.com/applandinc/appmap-java/commit/e7462538725142b50638c036835e04345f1a81d4))

## 0.5.1 (2021-03-09)

### Added

- System.err logs on configuration failures.

## 0.5.0 (2020-11-04)

### Added

- Support for capturing `http_client_request` and `http_client_response` events
  for Java 8 applications.

## 0.4.3 (2020-10-08)

### Fixed

- ReflectiveType.invoke now makes the method accessible before trying to invoke
  it.

## 0.4.2 (2020-10-06 (yanked))

### Added

- The new system property `appmap.debug.http` to show some debugging when
  handling requests for `/_appmap/record`.

### Changed

- Cleaned up reflection in `HttpServletRequest`, `HttpServletResponse`, and
  `FilterChain`.

## 0.4.1 (2020-08-25)

### Added

- The new system property `appmap.record` can be used to specify a method to
  record.
- The system property `appmap.debug.file`. When set, debug output will be
  written to the specified file, instead of to `System.err`.

### Changed

- `appmap-java` now uses significantly less memory when recording.

## 0.4.0 (2020-08-06)

### Added

- Tests annotated with `org.testng.annotations.Test` are now recorded.

## 0.3.2 (2020-07-30)

### Fixed

- Added callback handlers with `METHOD_EXCEPTION` method event to close
  recording of current session for `Junit` and `Jupiter` Tests.

## 0.3.1 (2020-07-15)

### Fixed

- Dependencies of `appmap-java` which use `slf4j` will now use a nop
  implementation in order to stop them from logging if the client application
  provides it's own implementation. If left up to the client application,
  `appmap-java` can interfere with test frameworks by corrupting stdout.
- `appmap-java` will no longer call `Thread` methods that may be extended by the
  client application resulting in a stack overflow.
- `appmap-java` no longer assumes that `Thread` identifiers are unique.
  Previously, this could result in concurrent modificiations to resources not
  meant to be shared across threads.

## 0.3.0 (2020-07-08)

### Fixed

- Removed a potential deadlock that could occur if the user's code required a
  lock in `toString`.
- Output directories will be recursively created

### Added

- `appmap.event.valueSize` can be used to specify the length of a value string
  before truncation occurs.
- `Recorder.record` will record and output the execution of a `Runnable`.

### Changed

- In `appmap.yml` a package item can now have an empty `path`, allowing the user
  to specify a list of exclusions only. This can be useful for excluding groups
  of tests not meant to be captured.
- The default output directory for appmap files is now `./tmp/appmap`. This
  directory will be created automatically if it does not exist.

## 0.2.1 (2020-07-01)

### Fixed

- `appmap.output.directory` will be created if it does not exist. Previously
  this resulted in a null pointer exception when recording.
- Parsing errors in `appmap.yml` will stop the application from running and emit
  a useful error.

## 0.2.0 (2020-06-29)

### Added

- Support for Jakarta

### Removed

- `javax.servlet` dependency

### Changed

- All logging is disabled by default. To enable logging use the `appmap.debug`
  system property.
- Loading times have been reduced significantly.

## 0.1.0 (2020-06-05)

### Added

- Name property added to appmap metadata
- HTTP request parameters are captured
- Spring path params are captured
- Spring normalized path info is captured
- Support for JUnit Jupiter

### Fixed

- Feature group metadata is now written to the correct key

## 0.0.2 (2020-05-15)

### Added

- `language` object in `metadata` now includes `version` and `engine` based on
  the Java runtime.
- `sql_query` events now include `database_type` as reported by the database
  driver.

## 0.0.1 (2020-04-28)

### Added

- Versioning has begun. The authoritative `version` is now declared in
  [`build.gradle`](build.gradle).
- Feature and feature group metadata fields are now written to scenarios
  captured from test cases.
