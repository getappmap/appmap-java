## [1.12.1](https://github.com/applandinc/appmap-java/compare/v1.12.0...v1.12.1) (2022-04-12)


### Bug Fixes

* Move Labels into separate artifact ([3527e65](https://github.com/applandinc/appmap-java/commit/3527e65c7f30ad1d9c1401d365612b4336c48b88))

# [1.12.0](https://github.com/applandinc/appmap-java/compare/v1.11.0...v1.12.0) (2022-03-13)


### Bug Fixes

* Eliminate whereAmI at agent startup ([0396572](https://github.com/applandinc/appmap-java/commit/0396572973b1641d0405ac4dc5662a584d1054bb))


### Features

* Add @Labels annotation ([60c99bb](https://github.com/applandinc/appmap-java/commit/60c99bb4615ad0eeffab96bf444e069d24b9387b))

# [1.11.0](https://github.com/applandinc/appmap-java/compare/v1.10.0...v1.11.0) (2022-02-28)


### Bug Fixes

* Allow hook application to be ordered ([d111e0a](https://github.com/applandinc/appmap-java/commit/d111e0aa4c773d3e201a2be9e7fb731e77fb9291))


### Features

* http_server_requests for Http Core servers ([0b2d09c](https://github.com/applandinc/appmap-java/commit/0b2d09c20b8919f552e2693933d270151ad504d3))

# [1.10.0](https://github.com/applandinc/appmap-java/compare/v1.9.0...v1.10.0) (2022-02-16)


### Features

* Add support for Http Core ([baa0ba6](https://github.com/applandinc/appmap-java/commit/baa0ba6f29e71c301d2f0fa2242c19d1bdb3b35c))

# [1.9.0](https://github.com/applandinc/appmap-java/compare/v1.8.1...v1.9.0) (2022-01-18)


### Features

* Record non-public methods ([28a8fe9](https://github.com/applandinc/appmap-java/commit/28a8fe9a98dced02aace75d93741244b50800c69))

## [1.8.1](https://github.com/applandinc/appmap-java/compare/v1.8.0...v1.8.1) (2021-12-13)


### Bug Fixes

* Fix path and exclude item regexp ([18c028f](https://github.com/applandinc/appmap-java/commit/18c028f603a6f6cf8a9129be87e2b0bd1e0a6bf0))

# [1.8.0](https://github.com/applandinc/appmap-java/compare/v1.7.0...v1.8.0) (2021-12-13)


### Features

* Record HTTP server request and response headers ([009358a](https://github.com/applandinc/appmap-java/commit/009358a4532afe84d16d6d739178c6b9a4530ac9))

# [1.7.0](https://github.com/applandinc/appmap-java/compare/v1.6.1...v1.7.0) (2021-10-26)


### Features

* Update output from validate command ([318deab](https://github.com/applandinc/appmap-java/commit/318deab155e7faccc9572071f5ea966d36f9090b))

## [1.6.1](https://github.com/applandinc/appmap-java/compare/v1.6.0...v1.6.1) (2021-10-03)


### Bug Fixes

* Avoid ConcurrentModificationException ([c6d1b7b](https://github.com/applandinc/appmap-java/commit/c6d1b7bcce34aaa7898a678973a4bb6e64ac4abf))

# [1.6.0](https://github.com/applandinc/appmap-java/compare/v1.5.0...v1.6.0) (2021-09-13)


### Bug Fixes

* Abort if no config found ([cd57f2f](https://github.com/applandinc/appmap-java/commit/cd57f2f84d687c7c1959fdf0111cc5b8cc33c306))


### Features

* Look for config in parent directories ([1be744b](https://github.com/applandinc/appmap-java/commit/1be744b412388b3cc4b273c624e6a986912a2a60))

# [1.5.0](https://github.com/applandinc/appmap-java/compare/v1.4.1...v1.5.0) (2021-08-30)


### Features

* Add validate subcommand to the CLI ([a0650b0](https://github.com/applandinc/appmap-java/commit/a0650b0e8391f7ac39c71c83261687e9f5abac02))

## [1.4.1](https://github.com/applandinc/appmap-java/compare/v1.4.0...v1.4.1) (2021-08-14)


### Bug Fixes

* Guard NPE when checking the shallow flag on a package ([ff5c418](https://github.com/applandinc/appmap-java/commit/ff5c4184c4878af5826ed2e8126fd8daf907775c))
* Retry move as copy when the file is locked ([91ea858](https://github.com/applandinc/appmap-java/commit/91ea858badf792ed1129640715010ebd45d8698b))

# [1.4.0](https://github.com/applandinc/appmap-java/compare/v1.3.0...v1.4.0) (2021-08-09)


### Bug Fixes

* Memoize package name ([0a74b37](https://github.com/applandinc/appmap-java/commit/0a74b375d356753619574c3285c98d9235b03282))
* Record to designated output directory ([5787108](https://github.com/applandinc/appmap-java/commit/57871083ef43472da759c65da25e26ec6bebf0ae))


### Features

* Ignore common methods ([53e537d](https://github.com/applandinc/appmap-java/commit/53e537d703bcc4f4995bb6c2a4f5609297f529aa))
* Implement 'shallow' mode ([ebf10ea](https://github.com/applandinc/appmap-java/commit/ebf10eaf6e95585917ddadc63e1b8422be11d5b6))

# [1.3.0](https://github.com/applandinc/appmap-java/compare/v1.2.0...v1.3.0) (2021-08-02)


### Bug Fixes

* Improve robustness and logging of parent interface and superclass access ([a24f056](https://github.com/applandinc/appmap-java/commit/a24f056f423fb39173e5d47f667c65002d954c49))
* Keep events 'open' until the next event is received ([9e12099](https://github.com/applandinc/appmap-java/commit/9e12099bf3c602b594c4927f0588db42c75d4711))
* Remove '-f' option to 'wait' ([637f904](https://github.com/applandinc/appmap-java/commit/637f90478eb1a76a1b0567905b1531a1b238e4b1))


### Features

* Add session recording checkpoint ([04d9293](https://github.com/applandinc/appmap-java/commit/04d92936f662f6bf2757cf00e12eab42426291fb))
* Apply different fields to call, return, sql, etc ([3d8db1f](https://github.com/applandinc/appmap-java/commit/3d8db1fd7491665964269bf03fb2567d78cc7c8e)), closes [#50](https://github.com/applandinc/appmap-java/issues/50) [#44](https://github.com/applandinc/appmap-java/issues/44)
* appmap.debug enables additional debug info ([67e935e](https://github.com/applandinc/appmap-java/commit/67e935eafc303799ead1dcac4174116e4a2368e8))
* Expand and modularize the integration test suite ([ad06488](https://github.com/applandinc/appmap-java/commit/ad06488b0c54a364b20ddd657783d3f70d32c165))
* Record return event parent_id ([40be795](https://github.com/applandinc/appmap-java/commit/40be795a390b0008281ed3456b10e6a017565f45))
* Write source location and test status metadata ([397e200](https://github.com/applandinc/appmap-java/commit/397e2009901cff391830955ab6db3cfa91418161))

# [1.2.0](https://github.com/applandinc/appmap-java/compare/v1.1.0...v1.2.0) (2021-07-26)


### Bug Fixes

* Fix some path bugs and get the tests working ([ec32e23](https://github.com/applandinc/appmap-java/commit/ec32e2364c0a1f44d60c8c4907d7b5e2cc617a1e))
* Remove unused class RecordServlet ([1bd39de](https://github.com/applandinc/appmap-java/commit/1bd39de4585845d790140b0d7735716d643b76e8))


### Features

* 'init' command suggests appmap.yml with project name and packages ([299bb70](https://github.com/applandinc/appmap-java/commit/299bb70714b706448dd0a56439540737e2894f81))
* 'status' command reports project properties ([cb4693d](https://github.com/applandinc/appmap-java/commit/cb4693d568ff891ee62db8f4c456fbf4215b0d1a))
* 'status' command reports test commands ([058ae0e](https://github.com/applandinc/appmap-java/commit/058ae0ee64d099fcecb6427ffed62c3568c8212b))
* Add stub for agent 'init' command ([55d9fc8](https://github.com/applandinc/appmap-java/commit/55d9fc87db2df54b7f5d6c9a50613012c7ae50d7))
* Add stub for agent 'status' command ([7f5fd02](https://github.com/applandinc/appmap-java/commit/7f5fd02abe9e99497f17a0265e33495e7c2dc0ed))
* Helper commands can direct output to a file ([bf1c699](https://github.com/applandinc/appmap-java/commit/bf1c6992aac43742074f21b6d72acee9365f0181)), closes [#89](https://github.com/applandinc/appmap-java/issues/89)

# [1.1.0](https://github.com/applandinc/appmap-java/compare/v1.0.4...v1.1.0) (2021-05-13)


### Features

* Add a property to enable automatic recording at boot time ([7c83614](https://github.com/applandinc/appmap-java/commit/7c83614d630cd9d995cc34856035fd31a7ff810a))

## [1.0.4](https://github.com/applandinc/appmap-java/compare/v1.0.3...v1.0.4) (2021-04-08)


### Bug Fixes

* Don't append System path to class pools ([681d74e](https://github.com/applandinc/appmap-java/commit/681d74ec843a5b4c7acf317114cd54766a3b2d87))
* Provide better error message when encountering an unknown event ([c69a877](https://github.com/applandinc/appmap-java/commit/c69a87779a5a33994b295aade6cfcfd188d3cc37))

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
