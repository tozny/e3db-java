/*
 * TOZNY NON-COMMERCIAL LICENSE
 *
 * Tozny dual licenses this product. For commercial use, please contact
 * info@tozny.com. For non-commercial use, the contents of this file are
 * subject to the TOZNY NON-COMMERCIAL LICENSE (the "License") which
 * permits use of the software only by government agencies, schools,
 * universities, non-profit organizations or individuals on projects that
 * do not receive external funding other than government research grants
 * and contracts.  Any other use requires a commercial license. You may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at https://tozny.com/legal/non-commercial-license.
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations under
 * the License. Portions of the software are Copyright (c) TOZNY LLC, 2018.
 * All rights reserved.
 *
 */

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
