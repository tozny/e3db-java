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
* common-test - Tests that can be run on both Android and using plain java. This directory does not hold a project
that can be executed independently.
* androidtest - Android-specific tests.
* plaintest - Plain java integration tests.

Plain Java & Android Testing
====

The `common-test` directory holds tests that can be run on Android and using "plain" Java. These tests are
not executed independently, but as part of the `plaintest` and `androidtest` projects.

The common-test/src/test directory contains a set of integration tests that cover
basic functionality: register, read, write, query, and sharing.

To run the tests, you must supply a registration token from the Tozny console. We recommend *not* using your
"main" account, as each test will register multiple clients which can clutter the client management page.

Registration Token Setup
====

There are multiple ways to set a registration token for use when when running tests:

* Environment Variable - Set 'REGISTRATION_TOKEN'
* Project Property - At the command line, pass the `-P` option to the gradle wrapper:
```bash
$ ./gradlew -PREGISTRATION_TOKEN="..." :androidtest:connectedAndroidTest
$ ./gradlew -PREGISTRATION_TOKEN="..." :plaintest:test
```
* `local.properties` file - Set REGISTRATION_TOKEN in the `local.properties` file (only this method will allow IntelliJ to execute tests)

Tests will fail to run if a registration token is not set.

Note that `DEFAULT_API_URL` can also be set in a similar way to test against any self-hosted E3DB instances. If not set,
tests will run against `https://api.e3db.com`.

"FileNotFoundException" When Testing
====

The lazysodium library attempts to delete the libsodium.dll (or .so) file that it unpacks when the JVM
exits. On Windows, this results in a number of errors printed to the console such as:

```
 java.io.FileNotFoundException: C:\<...>\Temp\nativeutils11369415283358\libsodium.dll
 (The process cannot access the file because it is being used by another process)
```

These errors are *benign* and can be ignored.

Plain Java Testing
====

The `plaintest` directory contains test specific to the crypto libraries used on the plain Java platform. You
can run these test from the command line using teh command `gradlew :plaintest:test`.

You must set the `REGISTRATION_TOKEN` environment variable to supply a registration token.

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

Running a Subset of Tests
----

`plaintest` and `androidtest` allow you to run a subset of the test suite, but they differ in how you specify the test
to run:

* plain -- Use the `--tests` argument at the command line to specify a set (or a wildcard pattern matching any given
tests). For example, to test the `testEncodeDecodeLocal` method in the `ClientTest` class:

```
> ./gradlew :plaintest:test --tests *testEncodeDecodeLocal
```

(The `*` is necessary as the fully-qualified name of the test is actually `com.tozny.e3db.ClientTest.testEncodeDecodeLocal`.)

* android - The android gradle plugin supports testing single classes and methods using a system property. For
example, to test the `testValidSignature` method in the `com.tozny.e3db.AndroidCryptoTest` class, use:

```
> gradlew '-Pandroid.testInstrumentationRunnerArguments.class=com.tozny.e3db.AndroidCryptoTest#testValidSignature' :androidtest:connectedAndroidTest
```

For the ability to run a subset of the test suite, use `adb`, as documented in
the article "Test from the Command Line" at https://developer.android.com/studio/test/command-line.

Creating Javadocs
=====

To generate documentation, run the following command from the root of the repo:

```
$ ./gradlew :e3db:javadoc
```

A directory named `docs\<version>` (where _version_ can be found in `./publish/build.gradle`) will be
created, containing the generated javadocs.

Please ensure that the `javadoc` task does not generate any warnings or errors.

Lint
====

Linting over the Android source ensures that the library stays compatible with the SDKs it targets. Prior
to publishing, run lint like so:

```
$ ./gradlew :publish:android:lint
```

(We use the `publish` project to ensure we only lint over Android-specific and common sources.)

Publishing
====

The SDK can be published for plain Java and for Android. To publish to your local Maven repository, run the following command:

$ gradlew :publish:publishLocal

The published artifacts have different names, but should share versions. They are:

* Plain Java - 'com.tozny.e3db:e3db-client-plain:<version>'
* Android - 'com.tozny.e3db:e3db-client-android:<version>@aar'

(The version is specified in the file `./publish/build.gradle`.)

Publishing a Release
====

To generate javadocs for new versions of the SDK to publish your system / IDE must be using Java 1.8

You can configure and switch between multiple versions of Java on a Mac for using [jenv](https://www.jenv.be) as shown below:


```bash
# Install the jenv tool
brew install jenv
# Load index of non-default packages provided by homebrew
brew tap homebrew/cask
# Update brew with latest versions of open source / community JDK
brew tap adoptopenjdk/openjdk
# Install versions of SDK as needed by your project
brew install --cask adoptopenjdk8
brew install --cask adoptopenjdk11
brew install --cask adoptopenjdk14
# Add the following to your systems ~/.bash_profile or ~./bashrc file
export PATH="$HOME/.jenv/bin:$PATH"
eval "$(jenv init -)"
export JAVA_8_HOME=$(/usr/libexec/java_home -v1.8)
export JAVA_11_HOME=$(/usr/libexec/java_home -v11)
export JAVA_14_HOME=$(/usr/libexec/java_home -v14)
# Then source ~/.bashrc or sourc
[octo@ValleyOfTheForge e3db-java]$ source ~/.bashrc
# Then switch between java versions as desired
[octo@ValleyOfTheForge e3db-java]$ jenv global 1.8
[octo@ValleyOfTheForge e3db-java]$ java -version
openjdk version "1.8.0_282"
OpenJDK Runtime Environment Corretto-8.282.08.1 (build 1.8.0_282-b08)
OpenJDK 64-Bit Server VM Corretto-8.282.08.1 (build 25.282-b08, mixed mode)
# List available versions of java installed and able to switch between
[octo@ValleyOfTheForge e3db-java]$ jenv versions
  system
* 1.8 (set by /Users/octo/.jenv/version)
  1.8.0.282
  11
  11.0
  11.0.10
  14
  14.0
  14.0.2
  corretto64-1.8.0.282
  corretto64-11.0.10
  oracle64-14.0.2
[octo@ValleyOfTheForge e3db-java]$ jenv global 14
[octo@ValleyOfTheForge e3db-java]$ java -version
java version "14.0.2" 2020-07-14
Java(TM) SE Runtime Environment (build 14.0.2+12-46)
Java HotSpot(TM) 64-Bit Server VM (build 14.0.2+12-46, mixed mode, sharing)
[octo@ValleyOfTheForge e3db-java]$
```

When preparing to publish:

- Merge all changes that will be published into the master branch.
- Change the version number in `./publish/build.gradle` to the version that will be published.
- Generate javadocs (`./gradlew :e3db:javadoc`)
- Commit the newly generated javadocs folder.
- Search across all files in the repo for the previous version number, and replace it with the new version number. At least the following
  files should be updated:
  - README.md - links and version references.
  - docs\index.md - Change link to latest version of docs; move link to previous version of docs to the list of previous versions.
- Commit changes to documentation
- Merge all changes that will be published into the master branch.
- Tag the commit using the version number just published (`git tag -s -a -m "Release <version>" <version>`)
- Push changes and tags to remote
- Publish JARs to the remote repository (`./gradlew :publish:publishMavenCentral`)


To publish to maven central, the following gradle properties must be set:

* signing.keyId= <- last 8 characters of the gpg KEY ID of your private signing key, this key must be shared with a third party gpg key server such as hkp://keyserver.ubuntu.com
* signing.password= <- signing key password
* signing.secretKeyRingFile= <-full path to your secring.gpg

You can set up a gpg key and publish it to the ubuntu key servers via

```bash
gpg --gen-key
gpg -K
cd $HOME/.gnupg
gpg --export-secret-keys -o secring.gpg # You'll be prompted for the password from step one
gpg --send-keys --keyserver keyserver.ubuntu.com <KEY ID>
```

The following list are required. In order to not check into source control they are often placed in
`~/.gradle/gradle.properties` or equivalent so they are sourced automatically.
* ossrhUsername= <- user token for sonatype ossr account that has permissions to deploy form com.tozny
* ossrhPassword= <- password token for above account

* developerId= <developer id>
* developerName= <Full Name>
* developerEmail= <tozny email address>

The version published is set in `publish/build.gradle`, in the `ext` block:

```gradle
ext {
  version = "<version number>"
  ...
}
```

Assuming your OSSRH account has the correct privileges, publish by running the following task:

```bash
$ ./gradlew :publish:publishMavenCentral
```

The task will publish both the plain Java JAR and the Android AAR to the maven staging repository, actual deployment needs to be done with the [Nexus Repository Manager](https://oss.sonatype.org/#stagingRepositories.)
Instructions for that can be found [here](https://central.sonatype.org/pages/releasing-the-deployment.html)

Writing Android Apps
====

Any Android app using the SDK requires the following dependency:

* 'com.tozny.e3db:e3db-client-android:7.2.1-SNAPSHOT@aar'

Writing Java Programs
====

Java programs requires the following dependency:

* 'com.tozny.e3db:e3db-client-plain:7.2.1-SNAPSHOT'

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

IntelliJ: Updating Gradle Projects
=====

IntelliJ, on occasion, decides it doesn't like how this project is organized and fails to synchronize with errors like:

```
A problem occurred evaluating project ':e3db-fips:publish:android'.
> Could not get unknown property 'implementation' for configuration container of type org.gradle.api.internal.artifacts.configurations.DefaultConfigurationContainer.
```

To fix, just select `Sync` from the Gradle tool window. Doing a full build on the command line (e.g., `:publish:publishLocal`)
will also fix the problem.
