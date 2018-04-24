E3DB Java SDK
====

This repo contains an E3DB SDK that can be used with both Android devices and plain Java programs.

Android Studio
====

This project is compatible with Android Studio 3.0 and higher.

Structure
====

The SDK contains the core E3DB operations, libsodium-based crypto implementations for Android
and Java, and tests.

* e3db - The core SDK.
* e3db/e3db-crypto-stubs - Used as placeholders when compiling the e3db project, but not ever shipped
  or used at runtime.
* e3db-crypto-interfaces - Defines crypto operations used by E3DB.
* e3db-crypto-android - Implements crypto operations on Android devices.
* e3db-crypto-plain - Implements crypto operations for plain Java programs.
* publish/android & publish/plain - Contains gradle scripts for publishing our library as an AAR (for
  Android) and as a JAR (for plain Java).

* androidtest - Android-specific tests.
* plaintest - Plain java integration tests.

Plain Java Testing
====

The plaintest/src/test directory contains a set of integration tests that cover
basic functionality: register, read, write, query, and sharing.

The tests use a hard-coded registration token (which is safe to use and distribute). However,
you can replace the token via System properies:

* e3db.host - The host to test against (e.g., `https://staging.e3db.com`). By default, tests against
  our dev environment.
* e3db.token - Token to use for client registration.

Android Testing
====

The `androidtest` project contains Android-specific tests (for crypto operations as well as secure configuration storage).

You can run the Android tests from the command line using the command `gradlew :androidtest:connectedAndroidTest`, assuming a phone is
plugged in and has USB debugging enabled.

The `androidtest` project also contain an app which exercises all secure configuration storage options. You can register
a client and protect the generated configuration with a password, lock screen PIN, fingerprint, or not at all (depending
on API level of the device, of course). The app is not intended to be used as an automated test, but as a tool for manual
testing.

Note that to run the app, you must publish the SDK locally

Testing Single Methods
----

The android gradle plugin supports testing single classes and methods using a system property. For
example, to test the `testValidSignature` method in the `com.tozny.e3db.crypto.AndroidCryptoTest` class, use:

```
> gradlew '-Pandroid.testInstrumentationRunnerArguments.class=com.tozny.e3db.crypto.AndroidCryptoTest#testValidSignature' :androidtest:connectedAndroidTest
```

Creating Javadocs
=====

To generate documentation, run the following command from the root of the repo:

```
$ ./gradlew :e3db:javadoc
```

A directory named `docs\<version>` (where _version_ can be found in `./publish/build.gradle`) will be
created, containing the generated javadocs.

Please ensure that the `javadoc` task does not generate any warnings or errors.

Publishing
====

The SDK can be published for plain Java and for Android. To publish to your local Maven repository, run the following command:

$ gradlew :publish:plain:publishToMavenLocal :publish:android:publishToMavenLocal

The published artifacts have different names, but should share versions. They are:

* Plain Java - 'com.tozny.e3db:e3db-client-plain:<version>'
* Android - 'com.tozny.e3db:e3db-client-android:<version>@aar'

(The version is specified in the file `./publish/build.gradle`.)

Writing Android Apps
====

Any Android app using the SDK requires the following dependency:

* 'com.tozny.e3db:e3db-client-android:2.0.1-SNAPSHOT@aar'

Writing Java Programs
====

Java programs requires the following dependency:

* 'com.tozny.e3db:e3db-client-plain:2.0.1-SNAPSHOT'

Benchmarking
====

The SDK can be benchmarked on Android devices and using plain Java (for desktop/server
environments). Android benchmarks are in the `android-benchmarks` directory, while plain
Java benchmarks are in the `plain-benchmarks` directory.

Plain Java
====

Plain Java benchmarks are implemented using the Java Microbenchmark Harness (JMH) from the
OpenJDK project. Maven must be used to build the JAR containing the benchmarks:

```bash
$ mvn clean install 
```

That command produces an executable jar, which can be used to run all the benchmarks:

```bash
$ java -jar target\benchmarks.jar -wi 1 -tu  ms -rf CSV -r 5 -bm avgt -f 1
```

(Pass the `-help` argument  for a complete list of arguments.)

Android
====

Android benchmarks are implemented using the [Spanner project](https://github.com/cmelchior/spanner).

Benchmarks are run like any other on-device Android unit test. From the root of this repository,
with a device connected via USB, run the following:

```bash
$ ./gradlew :android-benchmark:connectedDeviceTest
```

Results are available via `logcat` output, using the tag `BenchmarkResults`. For example:

```
03-13 13:18:27.818 8335-8353/com.tozny.e3db.benchmark I/BenchmarkResults: com.tozny.e3db.benchmark.CryptoBenchmark1B#decrypt: min/median/mean/max/99% (ms): 1.792e+00/1.802e+00/1.804e+00/1.838e+00/1.838e+00
03-13 13:19:18.292 8335-8353/com.tozny.e3db.benchmark I/BenchmarkResults: com.tozny.e3db.benchmark.CryptoBenchmark1B#encrypt: min/median/mean/max/99% (ms): 1.143e+00/1.148e+00/1.150e+00/1.157e+00/1.157e+00
03-13 13:20:09.698 8335-8353/com.tozny.e3db.benchmark I/BenchmarkResults: com.tozny.e3db.benchmark.CryptoBenchmark1B#sign: min/median/mean/max/99% (ms): 8.154e-01/8.187e-01/8.192e-01/8.224e-01/8.224e-01
```

etc.