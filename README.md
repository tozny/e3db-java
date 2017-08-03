E3DB Java SDK
====

This repo contains an E3DB SDK that can be used with both Android devices and plain Java programs.

Structure
====

The SDK contains the core E3DB operations, libsodium-based crypto implementations for Android
and Java, and two test programs.

* e3db - The core SDK.
* e3db-crypto-interfaces - Defines crypto operations used by E3DB.
* e3db-crypto-android - Implements crypto operations on Android devices.
* e3db-crypto-plain - Implements crypto operations for plain Java programs.
* e3dbtest - A sample Android application that doubles as an integration test.
* plaintest - A sample plain Java application.

Testing
====

e3dbtest defines the most complete test application, covering registration, read, write, update,
delete and query. (Sharing is not yet tested.)

The application uses a hard-coded registration token (which is safe to use and distribute). However,
you can replace the token via System properies:

* e3db.host - The host to test against (e.g., `https://staging.e3db.com`). By default, tests against
  our dev environment.
* e3db.token - Token to use for client registration. By default, uses a token created under the
  `jgbailey+dev3@tozny.com` account.

Writing Android Apps
====

Any Android app using the SDK requires the following two dependencies:

* e3db
* e3db-crypto-android

See e3dbtest/ for an example application.

Writing Java Programs
====

Java programs require the following two dependencies:

* e3db
* e3db-crypto-plain

See plaintest/ for an example application.
