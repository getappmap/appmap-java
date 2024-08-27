# [1.27.0](https://github.com/getappmap/appmap-java/compare/v1.26.10...v1.27.0) (2024-08-27)


### Features

* add config parameter to disable custom stringification ([cb18931](https://github.com/getappmap/appmap-java/commit/cb1893110a459a1f24fe92057e01691345880253))

## [1.26.10](https://github.com/getappmap/appmap-java/compare/v1.26.9...v1.26.10) (2024-08-21)


### Bug Fixes

* crash due to config packages key with missing value ([ef48399](https://github.com/getappmap/appmap-java/commit/ef4839987d2de935dba444d6041216877338d125))

## [1.26.9](https://github.com/getappmap/appmap-java/compare/v1.26.8...v1.26.9) (2024-07-05)


### Bug Fixes

* NPE running init when no appmap.yml found ([6dcd0d9](https://github.com/getappmap/appmap-java/commit/6dcd0d915e4419c852d07fe213463117d63587e7))

## [1.26.8](https://github.com/getappmap/appmap-java/compare/v1.26.7...v1.26.8) (2024-05-16)


### Bug Fixes

* filename too long in some cases ([fc74f0a](https://github.com/getappmap/appmap-java/commit/fc74f0adc9b9891b3189a755cb25bb234e2c3815))

## [1.26.7](https://github.com/getappmap/appmap-java/compare/v1.26.6...v1.26.7) (2024-04-15)


### Bug Fixes

* APPMAP_OUTPUT_DIRECTORY can be set from env ([c933c45](https://github.com/getappmap/appmap-java/commit/c933c45174da6baddaaaf66b8399d91165990d92))
* resolve output directory relative to config ([deb2a84](https://github.com/getappmap/appmap-java/commit/deb2a84538006aeba42cc2171876e21187dd739e))

## [1.26.6](https://github.com/getappmap/appmap-java/compare/v1.26.5...v1.26.6) (2024-04-10)


### Bug Fixes

* don't create a log file by default ([883cd93](https://github.com/getappmap/appmap-java/commit/883cd93c47aba34f62a6ae2239a8c55a38fd0778))

## [1.26.5](https://github.com/getappmap/appmap-java/compare/v1.26.4...v1.26.5) (2024-03-21)


### Bug Fixes

* correct detection of Spark server ([571e74b](https://github.com/getappmap/appmap-java/commit/571e74b43a4cb2b165417b14bc93a761a19b5e6e))

## [1.26.4](https://github.com/getappmap/appmap-java/compare/v1.26.3...v1.26.4) (2024-02-16)


### Bug Fixes

* use the correct ClassLoader ([9803d25](https://github.com/getappmap/appmap-java/commit/9803d25ac098d31bc9215053e9cd16c8a3950559))

## [1.26.3](https://github.com/getappmap/appmap-java/compare/v1.26.2...v1.26.3) (2024-02-02)


### Bug Fixes

* avoid NPE when a Proxy method returns void ([fe538f2](https://github.com/getappmap/appmap-java/commit/fe538f2a6f9d2dc079abfcb711215019bb2500b9))
* don't locate agent jar using a CodeSource ([fd1dcd8](https://github.com/getappmap/appmap-java/commit/fd1dcd880a972877890ccb23c45bd04de652bb82))

## [1.26.2](https://github.com/getappmap/appmap-java/compare/v1.26.1...v1.26.2) (2024-01-31)


### Bug Fixes

* handle a framework classloader that filters ([690d187](https://github.com/getappmap/appmap-java/commit/690d187b3354f8e120fefab5cee572d9642d2f98))
* skip some JDK packages ([0958324](https://github.com/getappmap/appmap-java/commit/09583240dbf1f863eb52349755292f94a6fe9c45))
* strip annotations when instrumenting ([d297955](https://github.com/getappmap/appmap-java/commit/d2979554aceb27f6882a2156eed428ec3bf17dee))
* support class in unnamed package ([2667f83](https://github.com/getappmap/appmap-java/commit/2667f83ed6ac05403be8b617a7dd1910abd62052))

## [1.26.1](https://github.com/getappmap/appmap-java/compare/v1.26.0...v1.26.1) (2024-01-16)


### Bug Fixes

* add info message when saving AppMap ([c7237a1](https://github.com/getappmap/appmap-java/commit/c7237a13fd454e9d149d2fe942e5f6060e6cd081))
* don't try to add event on test failure ([fb64b52](https://github.com/getappmap/appmap-java/commit/fb64b521041cd4f3a33eeca249e0a4d3b04100de))
* make sure TestNG tests are captured correctly ([c44c222](https://github.com/getappmap/appmap-java/commit/c44c2224d9280bc1da66aa904273f6fb88b897c4))
* only track packages stats when debugging ([d2d3d99](https://github.com/getappmap/appmap-java/commit/d2d3d996950d200a7eb438390cc3d6bcc4e9cae2))

# [1.26.0](https://github.com/getappmap/appmap-java/compare/v1.25.3...v1.26.0) (2024-01-11)


### Bug Fixes

* add missing hook ([5a7dc89](https://github.com/getappmap/appmap-java/commit/5a7dc89d699d230ea0e124cc614889aed3cfb095))
* generalize the way test methods are detected ([dce4e3d](https://github.com/getappmap/appmap-java/commit/dce4e3d0a9c9498231c114acdf0418304acc15aa))
* get rid of reflection in HookConditionSystem ([bebeffd](https://github.com/getappmap/appmap-java/commit/bebeffd7cefb5022e05974cf0b18f7fd6f2c5bf8))
* improve agent performance ([241d165](https://github.com/getappmap/appmap-java/commit/241d1657cb2e1491df47210c4b1db70e7f879fd5))
* only add essential git info ([c816cb2](https://github.com/getappmap/appmap-java/commit/c816cb241a411e3e151147b94b6bcc2f55e6ab68))


### Features

* create a log file by default ([b86203f](https://github.com/getappmap/appmap-java/commit/b86203f74327c3312ea2987f2f71f128cd248190))

## [1.25.3](https://github.com/getappmap/appmap-java/compare/v1.25.2...v1.25.3) (2023-12-14)


### Bug Fixes

* make sure getters and setters are ignored ([0bc2f8f](https://github.com/getappmap/appmap-java/commit/0bc2f8fcf107a258fb4393594998b9cd422b56a7))

## [1.25.2](https://github.com/getappmap/appmap-java/compare/v1.25.1...v1.25.2) (2023-12-06)


### Bug Fixes

* support JUnit 5 test methods with parameters ([1cdd2e9](https://github.com/getappmap/appmap-java/commit/1cdd2e9562cf5bc6c1056b0ef2246cc778df9142))

## [1.25.1](https://github.com/getappmap/appmap-java/compare/v1.25.0...v1.25.1) (2023-12-06)


### Bug Fixes

* decrease logging about missing classes ([87c83c9](https://github.com/getappmap/appmap-java/commit/87c83c9b12ea72a870408f0e7802ca2420d179ac))

# [1.25.0](https://github.com/getappmap/appmap-java/compare/v1.24.1...v1.25.0) (2023-12-01)


### Bug Fixes

* manage ClassPool instances per-thread ([30092a1](https://github.com/getappmap/appmap-java/commit/30092a18962eacc9a1c3dbc8b42d2602c2b16cc1))


### Features

* record calls made through a Proxy ([0873d3d](https://github.com/getappmap/appmap-java/commit/0873d3dc3c3468aa17e174e7b6d7e0d516eccfed))

## [1.24.1](https://github.com/getappmap/appmap-java/compare/v1.24.0...v1.24.1) (2023-11-21)


### Bug Fixes

* only collect git metadata in a repo ([d4ad468](https://github.com/getappmap/appmap-java/commit/d4ad468aab7f4895ae5a7bd45691159152d355d9))

# [1.24.0](https://github.com/getappmap/appmap-java/compare/v1.23.0...v1.24.0) (2023-11-16)


### Bug Fixes

* really show the config on startup ([15f58b6](https://github.com/getappmap/appmap-java/commit/15f58b60aa87f057f7efe8dae27ae9b62bca1cc0))


### Features

* add Git metadata, improve source paths ([62624fd](https://github.com/getappmap/appmap-java/commit/62624fdf2147183c5edda155c04f84493886fc9e))

# [1.23.0](https://github.com/getappmap/appmap-java/compare/v1.22.3...v1.23.0) (2023-11-02)


### Bug Fixes

* don't use ClassLoader.loadClass ([b8cf5ca](https://github.com/getappmap/appmap-java/commit/b8cf5cad45e1ca8cee3b176dd5e02a0ed68c2213))


### Features

* add NoAppMap to control test-case recording ([b136056](https://github.com/getappmap/appmap-java/commit/b136056631d199c94859665dc7a70f17762bedbf))

## [1.22.3](https://github.com/getappmap/appmap-java/compare/v1.22.2...v1.22.3) (2023-10-24)


### Bug Fixes

* go back to hooking org.junit.jupiter.api.Test ([f15dc6d](https://github.com/getappmap/appmap-java/commit/f15dc6d1f8b7671f9908e081e0d5c76282a05245))
* instrumentation should catch Throwable ([8b2064d](https://github.com/getappmap/appmap-java/commit/8b2064d49687ea309757629270fca445078f5e98))

## [1.22.2](https://github.com/getappmap/appmap-java/compare/v1.22.1...v1.22.2) (2023-10-12)


### Bug Fixes

* only add the agent jar if it can be located ([ca4bc1b](https://github.com/getappmap/appmap-java/commit/ca4bc1b41639a64d6187c29573ef3bbfb4943cd5))

## [1.22.1](https://github.com/getappmap/appmap-java/compare/v1.22.0...v1.22.1) (2023-10-06)


### Bug Fixes

* set request recording type correctly ([b50bc06](https://github.com/getappmap/appmap-java/commit/b50bc068ff73af8af587c8235020f615ef7e47f0))

# [1.22.0](https://github.com/getappmap/appmap-java/compare/v1.21.0...v1.22.0) (2023-10-04)


### Bug Fixes

* add the agent jar to the classpath ([0e84af4](https://github.com/getappmap/appmap-java/commit/0e84af4fdda6ec1e6227d99660ade68b47119294))
* avoid Gradle class filtering ([f3c2f57](https://github.com/getappmap/appmap-java/commit/f3c2f57a162f618cb3f08bb9f5436b73b88af677))
* avoid NPE for anonymous classes ([aa250cd](https://github.com/getappmap/appmap-java/commit/aa250cd26e85da8d78df9ee3835dd7193bcfa132))
* have safeClassForName require a ClassLoader ([f26eabe](https://github.com/getappmap/appmap-java/commit/f26eabedf423ecd4ce8c097d856654798cf49a5a))
* make ActiveSession.threadSession static ([86be0b7](https://github.com/getappmap/appmap-java/commit/86be0b7ecaac75bdf9452762f7acc599a09ac7d5))


### Features

* set test_status, test_failure for JUnit 5 ([aee1156](https://github.com/getappmap/appmap-java/commit/aee115677e4a74b19331179be89d5cf7d384eea8))

# [1.21.0](https://github.com/getappmap/appmap-java/compare/v1.20.3...v1.21.0) (2023-09-12)


### Features

* make tmp/appmap the default output directory ([50581e8](https://github.com/getappmap/appmap-java/commit/50581e876681a9d62e8343f7a983dd86fc1f07db))

## [1.20.3](https://github.com/getappmap/appmap-java/compare/v1.20.2...v1.20.3) (2023-09-06)


### Bug Fixes

* handle unset appmap.output.directory ([c9d1a39](https://github.com/getappmap/appmap-java/commit/c9d1a3957a9901d2c849c935b2c461c931407893))

## [1.20.2](https://github.com/getappmap/appmap-java/compare/v1.20.1...v1.20.2) (2023-09-02)


### Bug Fixes

* don't read the servlet's InputStream ([f44e4d7](https://github.com/getappmap/appmap-java/commit/f44e4d7d4aa3a57db40a3e530e9f0ae0fe7e0101))
* enable remote recording appropriately ([ab326f7](https://github.com/getappmap/appmap-java/commit/ab326f77a9faa1fb3312c1d73ced22a2785441b3))
* only add ServletListener once ([bfe1bd8](https://github.com/getappmap/appmap-java/commit/bfe1bd83570a865fd55aa7f102d7e457ee8b4017))

## [1.20.1](https://github.com/getappmap/appmap-java/compare/v1.20.0...v1.20.1) (2023-08-22)


### Bug Fixes

* don't generate AppMaps for static resources ([8cc322c](https://github.com/getappmap/appmap-java/commit/8cc322c6158bfc67a113bda49e1b8871f7a502d2))

# [1.20.0](https://github.com/getappmap/appmap-java/compare/v1.19.0...v1.20.0) (2023-08-18)


### Bug Fixes

* only add servlet listener when appropriate ([b0aab08](https://github.com/getappmap/appmap-java/commit/b0aab086eb7293dd6fb0064576e4e608f2e79aad))
* use current thread's class loader ([691edf7](https://github.com/getappmap/appmap-java/commit/691edf7536433b0bda29f98c203538c30750a829))


### Features

* add support for appmap_dir ([2e0f4c7](https://github.com/getappmap/appmap-java/commit/2e0f4c7bd76a8d176ee0638ce7fc61fe6494c678))

# [1.19.0](https://github.com/getappmap/appmap-java/compare/v1.18.1...v1.19.0) (2023-08-07)


### Bug Fixes

* CtClassUtil.isChildOf checks all interfaces ([2b30bbf](https://github.com/getappmap/appmap-java/commit/2b30bbf6cf18b2a334ef3545b9ac5522907b7ae1))
* ignore abstract methods when instrumenting ([ab7de0a](https://github.com/getappmap/appmap-java/commit/ab7de0a2e3511a9926ffb9ec13f2bae502327b14))
* make sure events are flushed correctly ([e876ab5](https://github.com/getappmap/appmap-java/commit/e876ab554020e305bfed4bb090288ec58e8266e0))
* print the path to the config on startup ([e37df71](https://github.com/getappmap/appmap-java/commit/e37df719ce6f9c41b98f7f60fe758e83c0d9b4d3))
* set the name of a remote recording ([bc2e8a8](https://github.com/getappmap/appmap-java/commit/bc2e8a8623b9cf303e15f30264db63469295a1b0))


### Features

* add support for Spark Framework ([051ccca](https://github.com/getappmap/appmap-java/commit/051ccca52fd8e16ec43c16f395fdcf29db13285a))
* use a filter to manage remote recording ([b955ac8](https://github.com/getappmap/appmap-java/commit/b955ac8790c4d25cf255918016d0dae717cb8c9f))

## [1.18.1](https://github.com/getappmap/appmap-java/compare/v1.18.0...v1.18.1) (2023-07-03)


### Bug Fixes

* don't depend on spring-web ([ad75e17](https://github.com/getappmap/appmap-java/commit/ad75e17a441d25ca72bc81252aed0fbe7b39fa81))

# [1.18.0](https://github.com/getappmap/appmap-java/compare/v1.17.2...v1.18.0) (2023-06-30)


### Bug Fixes

* add tinylog for logging ([fe1a05a](https://github.com/getappmap/appmap-java/commit/fe1a05abb601ee46d9fe41644eb1b0b28af7cd35))
* update ClassFileTransformer.transform ([cc673ef](https://github.com/getappmap/appmap-java/commit/cc673ef1507298b19e814885cf04c395aded6b55))


### Features

* add request recording ([066a316](https://github.com/getappmap/appmap-java/commit/066a316c702397e0ef5b3dae2e53e4361d6f0f43))

## [1.17.2](https://github.com/getappmap/appmap-java/compare/v1.17.1...v1.17.2) (2023-05-09)


### Bug Fixes

* hook other HttpClient.execute overloads ([f8d3533](https://github.com/getappmap/appmap-java/commit/f8d35339a7e529aae4ad6edccfe313e4396d8ec4))

## [1.17.1](https://github.com/getappmap/appmap-java/compare/v1.17.0...v1.17.1) (2023-04-26)


### Bug Fixes

* don't set "shallow: true" by default ([f61a19d](https://github.com/getappmap/appmap-java/commit/f61a19d8197fcd20da72ebfc7e4dbb57709ea6f5))
* set attributes of http_client_request ([3f14082](https://github.com/getappmap/appmap-java/commit/3f14082ae4dc4b8f79902065e31ccd0edbc267f0))

# [1.17.0](https://github.com/getappmap/appmap-java/compare/v1.16.0...v1.17.0) (2023-04-18)


### Features

* hook Apache's HttpClient ([25201d7](https://github.com/getappmap/appmap-java/commit/25201d7bb57c5ff86e9d1ea04c3ad42f06bc67cc))

# [1.16.0](https://github.com/getappmap/appmap-java/compare/v1.15.7...v1.16.0) (2023-03-18)


### Features

* generate a default config ([9ff949b](https://github.com/getappmap/appmap-java/commit/9ff949b68081e79329006693cdd6633097fcf9ce))

## [1.15.7](https://github.com/getappmap/appmap-java/compare/v1.15.6...v1.15.7) (2023-03-03)


### Bug Fixes

* don't crash on unnamed package ([4fc4dab](https://github.com/getappmap/appmap-java/commit/4fc4dab9821573e58fc2b40c8a16042f2f9a245d))

## [1.15.6](https://github.com/getappmap/appmap-java/compare/v1.15.5...v1.15.6) (2023-02-09)


### Bug Fixes

* Don't print exception when copying across filesystems fails ([57ce08f](https://github.com/getappmap/appmap-java/commit/57ce08f9737c54384c7a14205f7ae311e4cf4984))

## [1.15.5](https://github.com/getappmap/appmap-java/compare/v1.15.4...v1.15.5) (2023-01-04)


### Bug Fixes

* eliminate possibility of NPE ([cf13c4b](https://github.com/getappmap/appmap-java/commit/cf13c4bd8e96c464854278521f89f8cae97fe85b))

## [1.15.4](https://github.com/getappmap/appmap-java/compare/v1.15.3...v1.15.4) (2022-11-30)


### Bug Fixes

* improve naming of output directory ([660b506](https://github.com/getappmap/appmap-java/commit/660b506768419da39153a4b902eed625f34fdf61))

## [1.15.3](https://github.com/getappmap/appmap-java/compare/v1.15.2...v1.15.3) (2022-11-25)


### Bug Fixes

* handle parameterized requests correctly ([c54769e](https://github.com/getappmap/appmap-java/commit/c54769e362cf3ee90ba9a0a32ae2f5eba44bae1d))

## [1.15.2](https://github.com/getappmap/appmap-java/compare/v1.15.1...v1.15.2) (2022-11-18)


### Bug Fixes

* avoid a javassist issue ([7c39c28](https://github.com/getappmap/appmap-java/commit/7c39c287e094cfb70429274a1f5ab414ef66a736))

## [1.15.1](https://github.com/applandinc/appmap-java/compare/v1.15.0...v1.15.1) (2022-06-07)


### Bug Fixes

* Record normalized_path for Spring requests ([f6a9ee6](https://github.com/applandinc/appmap-java/commit/f6a9ee65667d9feabf882a5ddb817dfeb999567d))

# [1.15.0](https://github.com/applandinc/appmap-java/compare/v1.14.1...v1.15.0) (2022-06-07)


### Features

* Allow appmap_dir, language, and add'l ([a94ea0b](https://github.com/applandinc/appmap-java/commit/a94ea0ba59d013c74d8d5a4c147ce6a25948c4c1))

## [1.14.1](https://github.com/applandinc/appmap-java/compare/v1.14.0...v1.14.1) (2022-05-11)


### Bug Fixes

* Handle missing package names ([4270ab5](https://github.com/applandinc/appmap-java/commit/4270ab5bfdb0ab08ad1452573cd8b7fbed806b7d))

# [1.14.0](https://github.com/applandinc/appmap-java/compare/v1.13.1...v1.14.0) (2022-05-10)


### Features

* Configurable method labels ([2deb582](https://github.com/applandinc/appmap-java/commit/2deb582b178732f528830741b93d6b2f17ad1100))

## [1.13.1](https://github.com/applandinc/appmap-java/compare/v1.13.0...v1.13.1) (2022-04-27)


### Bug Fixes

* Ignore extra properties in appmap.yml ([95db306](https://github.com/applandinc/appmap-java/commit/95db30629f2a51df6559045a5cb9ee1ce2052ea6))

# [1.13.0](https://github.com/applandinc/appmap-java/compare/v1.12.3...v1.13.0) (2022-04-26)


### Features

* Add elapsed time ([950f083](https://github.com/applandinc/appmap-java/commit/950f0830b08382a69806f2e0fec136723a085bfd))

## [1.12.3](https://github.com/applandinc/appmap-java/compare/v1.12.2...v1.12.3) (2022-04-26)


### Bug Fixes

* Only show whereAmI when debugging ([f09468b](https://github.com/applandinc/appmap-java/commit/f09468b084a5fe7b0a8ce49e499d5efd1790e39a))
* Order source method hooks correctly ([578f45d](https://github.com/applandinc/appmap-java/commit/578f45d0c1f5172a0e61bd19bdc71f8066af17a3))

## [1.12.2](https://github.com/applandinc/appmap-java/compare/v1.12.1...v1.12.2) (2022-04-19)


### Bug Fixes

* Don't call mocked java.sql methods ([0ba3563](https://github.com/applandinc/appmap-java/commit/0ba3563477bd27178de53ec23768c0aa547f29ca))

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
