/**
 * Provides a client for interacting with Tozny's End-to-End Encrypted Database (E3DB).
 *
 * <p>Use the {@link com.tozny.e3db.ClientBuilder} class to create a client; use the created client to read, write,
 * query, update, and delete records. You can also use the client to add and remove sharing rules.
 *
 * <p>This library supports both plain Java and Android applications. To use the library
 * with Android, include the following dependency in your Gradle build definition:
 *
 * <pre>
buildscript {
  repositories {
    maven {
      name "Tozny Repo"
      url "https://maven.tozny.com/repo"
    }
  }
}

compile('com.tozny.e3db:e3db-client-android:2.0.1@aar') {
   transitive = true
}
 * </pre>
 *
 * To use with plain Java, instead reference the {@code e3db-client-plain} library,
 * without the {@code @aar} annotation:
 * <pre>
&lt;repositories&gt;
  &lt;repository&gt;
    &lt;id&gt;tozny-repo&lt;/id&gt;
    &lt;name&gt;Tozny Repository&lt;/name&gt;
    &lt;url&gt;https://maven.tozny.com/repo&lt;/url&gt;
  &lt;/repository&gt;
&lt;/repositories&gt;

&lt;dependencies&gt;
  &lt;dependency&gt;
    &lt;groupId&gt;com.tozny.e3db&lt;/groupId&gt;
    &lt;artifactId&gt;e3db-client-plain&lt;/artifactId&gt;
    &lt;version&gt;2.0.1&lt;/version&gt;
  &lt;/dependency&gt;
&lt;/dependencies&gt;
 * </pre>
 */
package com.tozny.e3db;
