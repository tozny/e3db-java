E3DB Java SDK
====

This repo contains an E3DB SDK that can be used with both Android devices and plain Java programs.

Structure
====

The SDK contains the core E3DB operations, libsodium-based crypto implementations for Android
and Java, and two test programs.

* e3db - The core SDK.
* e3db/e3db-crypto-stubs - Used as placeholders when compiling the e3db project, but not ever shipped
  or used at runtime.
* e3db-crypto-interfaces - Defines crypto operations used by E3DB.
* e3db-crypto-android - Implements crypto operations on Android devices.
* e3db-crypto-plain - Implements crypto operations for plain Java programs.
* publish/android & publish/plain - Contains gradle scripts for publishing our library as an AAR (for
  Android) and as a JAR (for plain Java).

* e3dbtest - Android integration tests.
* plaintest - A sample plain Java application.

Testing
====

e3dbtest contains a set of integration tests that cover basic functionality: regiser, read, writer, query,
and sharing.

The tests use a hard-coded registration token (which is safe to use and distribute). However,
you can replace the token via System properies:

* e3db.host - The host to test against (e.g., `https://staging.e3db.com`). By default, tests against
  our dev environment.
* e3db.token - Token to use for client registration. By default, uses a token created under the
  `jgbailey+dev3@tozny.com` account.

To run the tests, simply execute `MainActivityTest` in Android Studio. On the command line, you
can attempt `gradlew :e3dbtest:connectedAndroidTest` to run all tests, assuming a phone is
plugged in and has USB debugging enabled.

Publishing
====

The SDK can be published for plain Java and for Android. To publish to your local Maven repository, run the following command:

$ gradlew :publish:plain:publishToMavenLocal :publish:android:publishToMavenLocal

The published artifacts have different names, but should share versions. They are:

* Plain Java - 'com.tozny.e3db:e3db-client-plain:2.0-SNAPSHOT'
* Android - 'com.tozny.e3db:e3db-client-android:2.0-SNAPSHOT@aar'

Writing Android Apps
====

Any Android app using the SDK requires the following dependency:

* 'com.tozny.e3db:e3db-client-android:2.0-SNAPSHOT@aar'

See e3dbtest/ for an example application.

Writing Java Programs
====

Java programs requires the following dependency:

* 'com.tozny.e3db:e3db-client-plain:2.0-SNAPSHOT'

See plaintest/ for an example application.
