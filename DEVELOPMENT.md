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

* androidtest - Android crypto tests.
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

The `androidtest` project contains minimal unit tests covering the crypto library used on Android. All other
code used on Android is identical to the Plain java version, and is therefore tested using `plaintest`.

You can run the Andrdoid tests from the command line using the command `gradlew :androidtest:connectedAndroidTest`, assuming a phone is
plugged in and has USB debugging enabled.

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

* Plain Java - 'com.tozny.e3db:e3db-client-plain:2.0.1-SNAPSHOT'
* Android - 'com.tozny.e3db:e3db-client-android:2.0.1-SNAPSHOT@aar'

(The version is specified in the file `./publish/build.gradle`.)

Writing Android Apps
====

Any Android app using the SDK requires the following dependency:

* 'com.tozny.e3db:e3db-client-android:2.0.1-SNAPSHOT@aar'

Writing Java Programs
====

Java programs requires the following dependency:

* 'com.tozny.e3db:e3db-client-plain:2.0.1-SNAPSHOT'
