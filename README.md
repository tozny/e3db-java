# E3DB Java SDK

The Tozny End-to-End Encrypted Database (E3DB) is a storage platform
with powerful sharing and consent management features.

This repo contains an SDK that can be used with both Android
devices and plain Java programs.

## TOZNY NON-COMMERCIAL LICENSE

Tozny dual licenses this product. For commercial use, please contact
info@tozny.com. For non-commercial use, the contents of this file are
subject to the TOZNY NON-COMMERCIAL LICENSE (the "License") which
permits use of the software only by government agencies, schools,
universities, non-profit organizations or individuals on projects that
do not receive external funding other than government research grants
and contracts.  Any other use requires a commercial license. You may
not use this file except in compliance with the License. You may obtain
a copy of the License at https://tozny.com/legal/non-commercial-license.
Software distributed under the License is distributed on an "AS IS"
basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
License for the specific language governing rights and limitations under
the License. Portions of the software are Copyright (c) TOZNY LLC, 2020.
All rights reserved.

## Terms of Service

Your use of E3DB must abide by our [Terms of Service](https://tozny.com/tozny-terms-of-service/), as detailed in
the linked terms of service agreement.

# Getting Started

The E3DB SDK for Android and plain Java lets your application
interact with our end-to-end encrypted storage solution. Whether
used in an Android application or "plain" Java environment (such as a
server), the SDK presents the same API for using E3DB.

Before using the SDK, go to [Tozny's
Dashboard](https://dashboard.tozny.com/login), create an account, and go to
the `Manage Clients` section. Click the `Create Token` button under
the `Client Registration Tokens` heading. This value will allow your
app to self-register a new user with E3DB. Note that this value is not
meant to be secret and is safe to embed in your app.

# Documentation

Full API documentation for various versions can be found at the
following locations:

* [7.1.0](https://tozny.github.io/e3db-java/docs/7.1.0/) - The most recently released version of the client.
* All versions: https://tozny.github.io/e3db-java

Code examples for the most common operations can be found below.

# Using the SDK with Android

The E3DB SDK targets Android API 16 and higher. To use the SDK in your
app, add it as a dependency to your build. In Gradle, use:

```
repositories {
  mavenCentral()
  maven { url "https://dl.bintray.com/terl/lazysodium-maven" }
}

implementation ('com.tozny.e3db:e3db-client-android:7.1.0@aar') {
    transitive = true
}
```

Because the SDK contacts Tozny's E3DB service, your application also
needs to request INTERNET permissions.

# Using the SDK with Plain Java

For use with Maven, declare the following dependencies:

```
<dependencies>
  <dependency>
    <groupId>com.tozny.e3db</groupId>
    <artifactId>e3db-client-plain</artifactId>
    <version>7.1.0</version>
  </dependency>
</dependencies>
```

# Asynchronous Result Handling

The SDK supports asynchronous execution by returning all results to
callback handlers of type `ResultHandler<T>`, where `T` is the type of
the value expected. `ResultHandler` defines one method, `void
handle(Result<T> r)`, which takes one argument, returns no values, and
throws no checked exceptions.

`Result<T>` is either the result of the operation or an error. The
`isError()` method indicates which occurred. If an error did not occur,
then the `asValue()` method will return the result of the operation.

E3DB operations always occurr on a background thread. On Android,
`handle` will always be called on the UI thread (after communication
with E3DB has finished). When used with plain Java, `handle` will be
called on the same background thread used for E3DB interactions.

E3DB operations do not have timeouts defined -- you will have to
manage those within your own application.

# Registering a Client

Registering creates a new client that can be used to interact with
E3DB. Each client has a unique ID and is associated with your Tozny
account. Registering only needs to happen once for a given client --
after credentials have been stored securely, the client can be
authenticated again using the stored credentials.

```java
import com.tozny.e3db.*;
// ...

String token = "<registration token>";
String host = "https://api.e3db.com";

Client.register(token, "client1", host, new ResultHandler<Config>() {
  @Override
  public void handle(Result<Config> r) {
    if(! r.isError()) {
      Config info = r.asValue();
      // write credentials to secure storage ...
    }
    else {
      // throw to indicate registration error
      throw new RuntimeException(r.asError().other());
    }
  }
});
```

# Android-specific Features

The E3DB SDK provides special support for storing credentials securely on Android devices.

## Storing credentials securely

The E3DB SDK supports several methods for storing credentials using Android's built-in
security features. On API 23+ devices, credentials can be protected with:

  * Password
  * Fingerprint
  * Lock Screen PIN

On older devices, credentials can be protected with a password. We recommend using "Lock Screen PIN"
on newer devices, and password on older devices.

To protect credentials, first register a client as above. The credentials created on registration can be saved
and protected by requiring the user to enter their lock screen PIN as follows:

```java
import com.tozny.e3db.*;
import com.tozny.e3db.android.*;
// ...

Activity context = ...; // application context

Client.register(token, "client1", host, new ResultHandler<Config>() {
  @Override
  public void handle(Result<Config> r) {
    if(! r.isError()) {
        Config mConfig = r.asValue();
        Config.saveConfigSecurely(new AndroidConfigStore(context, KeyAuthentication.withLockScreen(), KeyAuthenticator.defaultAuthenticator(context, "")), mConfig.json(), new ConfigStore.SaveHandler() {
                @Override
                public void saveConfigDidSucceed() {
                    // configuration successfully saved
                }

                @Override
                public void saveConfigDidCancel() {
                }

                @Override
                public void saveConfigDidFail(Throwable e) {
                }
            });
    }
    else {
      // throw to indicate registration error
      throw new RuntimeException(r.asError().other());
    }
  }
});
```

The configuration can then be loaded as follows:

```java
Config.loadConfigSecurely(new AndroidConfigStore(context,  KeyAuthentication.withLockScreen(), KeyAuthenticator.defaultAuthenticator(context, "")), new ConfigStore.LoadHandler() {
    @Override
    public void loadConfigDidSucceed(String config) {
        // Config loaded successfully
    }

    @Override
    public void loadConfigDidCancel() {
        // User cancelled authentication method
    }

    @Override
    public void loadConfigNotFound() {
        // Config does not exist
    }

    @Override
    public void loadConfigDidFail(Throwable e) {
        // An error occurred while loading - user entered wrong authentication, etc.
    }
});
```

The `AndroidConfigStore` class has additional constructors for managing multiple credentials (by name). The `KeyAuthentication`
class provides static methods for other authentication types, as well methods for testing if a particular type of authentication
is supported by the device. The `KeyAuthenticator` interface can be used to provide a custom UI for gathering fingerprint, password, and
lock screen PIN if desired. It also provides the static method `defaultAuthenticator` which gives a default UI.

## Updating `android` packaging options

Some of the libraries that the SDK depends on contain duplicate, but non-essential, files. If you do not exclude them,
your Android build will fail with an error similar to `com.android.build.api.transform.TransformException: com.android.builder.packaging.DuplicateFileException`.
In order to avoid this error add the following `packagingOptions` block to the `android` section of your Gradle build file:

```groovy
android {
  ...
  packagingOptions {
    exclude 'META-INF/LICENSE'
  }
}
```

## A note about "`allowBackup`" (`AndroidManifest.xml`)

If your `AndroidManifest.xml` specifies `allowBackup="true"`, your app will fail to compile as one of E3DB's dependencies
carries its own `AndroidManifest.xml` which specifies `allowBackup="false"`. If your app does allow backups, we recommend that
you do not back up credentials or that you store them securely, as above. In any case, to correct the error, add the `tools:replace="android:allowBackup"`
attribute to the `application` element in your `AndroidManifest.xml`. For example:

```xml
<manifest ...
   xmlns:tools="http://schemas.android.com/tools">
    ...
    <application
        ...
        tools:replace="android:allowBackup">
    ...
    </application>
</manifest>
```

# Using a Client to Interact with E3DB

Once a client has been registered and credentials have been stored,
you can use the `ClientBuilder` class to create an authenticated client
that can interact with E3DB:

```java
String storedCredentials = ...; // Read from secure storage
Client client = new ClientBuilder()
  .fromConfig(Config.fromJson(storedCredentials))
  .build();
```

Now the `client` value can be used to interact with E3DB.

## Certificate Pinning

In some situations, you might want to pin a specific certificate or set
of certificates as trusted entities for SSL collections - this is not a
typical need, but does arise from time to time.

To pin certificates, instantiate an OkHttp `CertificatePinner` object and
add the certificates you want your application to explicitly trust.

The [OkHttp documentation](http://square.github.io/okhttp/3.x/okhttp/okhttp3/CertificatePinner.html)
has detailed examples for what the pinner object should look like and how
to specify the various certificates. Once instantiated, pass the object in
when registering a new client:

```java
import com.tozny.e3db.*;
// ...

String token = "<registration token>";
String host = "https://api.e3db.com";

CertificatePinner pinner = new CertificatePinner.Builder()
    .add("api.e3db.com", "sha256/sha256/Vjs8r4z+80wjNcr1YKepWQboSIRi63WsWXhIMN+eWys=")
    .build()

Client.register(token, "client1", host, pinner, new ResultHandler<Config>() {
  @Override
  public void handle(Result<Config> r) {
    if(! r.isError()) {
      Config info = r.asValue();
      // write credentials to secure storage ...
    }
    else {
      // throw to indicate registration error
      throw new RuntimeException(r.asError().other());
    }
  }
});
```

Or when constructing a client:

```java
String storedCredentials = ...; // Read from secure storage

CertificatePinner pinner = new CertificatePinner.Builder()
    .add("api.e3db.com", "sha256/sha256/Vjs8r4z+80wjNcr1YKepWQboSIRi63WsWXhIMN+eWys=")
    .build()

Client client = new ClientBuilder()
  .fromConfig(Config.fromJson(storedCredentials))
  .setCertificatePinner(pinner)
  .build();
```

## Write a record

Records are represented as a `Map` with `String`-typed keys and
`String`-typed values.

```java
Client client = ...; // Get a client instance

Map<String, String> lyric = new HashMap<>();
lyric.put("line", "Say I'm the only bee in your bonnet");
lyric.put("song", "Birdhouse in Your Soul");
lyric.put("artist", "They Might Be Giants");

String recordType = "lyric";

client.write(recordType, new RecordData(lyric), null, new ResultHandler<Record>() {
    @Override
    public void handle(Result<Record> r) {
      if(! r.isError()) {
        // record written successfully
        Record record = r.asValue();
        // Log or print record ID, e.g.:
        System.out.println("Record ID: " + record.meta().recordId());
      }
      else {
        // an error occurred
        throw new RuntimeException(r.asError().other());
      }
    }
  }
);
```

All values will be encrypted locally before being stored in E3DB.
However, field names (for example, "song" and "artist" above) will
remain unencrypted.

Any data format, such as JSON or raw bytes, can be stored as long as
it is first converted to a `String` for a write operation. Just be sure
to reverse the process later when reading the data.

## Query records

E3DB allows you to query records based on a number of criteria,
including record type. Use the `QueryParamsBuilder` object to build a
query:

```java
QueryParams params = new QueryParamsBuilder()
  .setTypes("lyric")
  .setIncludeData(true)
  .setCount(50)
  .build();

Client client = ...; // Get a client instance

client.query(params, new ResultHandler<QueryResponse>() {
   @Override
   public void handle(Result<QueryResponse> r) {
     if(! r.isError()) {
       // print list of records
       for(Record r : r.asValue().records()) {
         System.out.println("Record ID: " + r.meta().recordId());
         System.out.println("Song: " + r.data().get("song"));
       }
     }
   }
 }
);
```

`setCount` controls the number of records returned; `setIncludeData`
includes the data for each record in results (otherwise, only `meta()`
will be populated; `data()` will return an empty `Map`). Other possible
filters include:

- `setWriterIds`: Filter to records written by these IDs
- `setUserIds`: Filter to records with these user IDs
- `setRecordIds`: Filter to only the records identified by these IDs
- `setTypes`: Filter to records that match the given types
- `setIncludeAllWriters`: Set this flag to include records that have been shared
  with you, defaults to `false`

> While the Java SDK supports _writing_ plaintext meta with records, the query interface does not support filtering on that meta information at this time.

## Pagination

The `QueryResponse` object's method `last()` gives a value indicating the last
record returned. Passing this value to the `setAfter()` method on the `QueryParamsBuilder`
object will cause E3DB to return records that come "after" that value. For example, this
snippet will loop through all "lyric" records in 10-row increments:

```java
Client client = ...; // Get a client instance

// Create a parameter builder that we can re-use to
// call the `setAfter()` method over and over, for
// pagination.
final QueryParams params = new QueryParamsBuilder()
  .setTypes("lyric")
  .setIncludeData(true)
  .setCount(10);

// Allows us to modify this flag from inside the `ResultHandler`
// anonymous class below.
final AtomicReference<Boolean> done = new AtomicReference<>(false);

while(! done.get()) {
  CountDownLatch wait = new CountDownLatch(1);

  client.query(params.build(), new ResultHandler<QueryResponse>() {
     @Override
     public void handle(Result<QueryResponse> r) {
       if(! r.isError()) {
         List<Record> records = r.asValue().records();
         if(records.size() == 0) {
           // no more records, stop looping
           done.set(true);
         }
         else {
           // print list of records for this page
           for(Record r : r.asValue().records()) {
             System.out.println("Record ID: " + r.meta().recordId());
             System.out.println("Song: " + r.data().get("song"));
           }

           // set next page in parameter builder
           params.setAfter(r.asValue().last());
         }

         wait.countDown();
       }
     }
   }
  );

  wait.await(30, TimeUnit.SECONDS);
}
```

## Sharing Records

E3DB allows the writer of a record to securely share that record with
other E3DB clients. To share, you must know the client ID of the
recipient. (The client ID of a given client is contained in the
response given when registering.)

Records are shared by `type`; the below shows sharing "lyric" records
with a recipient represented by the variable `readerId`:

```java
Client client = ...; // Get a client instance
UUID readerId = ...; // Get the ID of the reader with whom we share

client.share("lyric", readerId, new ResultHandler<Void>() {
  @Override
  public void handle(Result<Void> r) {
    if(! r.isError()) {
      // record shared
    }
  }
});

```

Sharing can be revoked between clients, as well:

```java
client.revoke("lyric", readerId, new ResultHandler<Void>() {
  @Override
  public void handle(Result<Void> r) {
    if(! r.isError()) {
      // record shared
    }
  }
});

```

Note that the `Void` type means that the `Result` passed to `handle`
represents whether an error occurred or not, and nothing else. Sharing
operations do not return any useful information on success.

## Authorizers

Every E3DB client can authorize any other client to share data on their behalf. That is, the data producer
does not need to be the sole entity that enables sharing with other clients. We call the client that is allowed to
share data on a data producer's behalf the "authorizer".

Just like `share`, authorization is granted based on record types. That is, a client can only authorize another client
to share a specific record type. There is no mechanism to grant sharing of all record types (whether any exist or not).

Note that the authorizer does *not* have permission to read the data shared themselves - they are only allowed to
share data on behalf of the data producer.

To add an authorizer, use the `addAuthorizer` method:

```java
UUID authorizerId = <ID of client to share on this data producer's behalf>;
String recordType = <type of records to authorize>;

client.addAuthorizer(authorizerId, recordType, new ResultHandler<Void>() {
 @Override
 public void handle(Result<Void> r) {
   if(! r.isError()) {
     // record shared
   }
 }
});

```

Authorization can be removed with the `removeAuthorizer` methods. Authorization *can* be removed for all
record types, or for a single record type.

A client can list all clients that it has authorized to share on its behalf using the `getAuthorizers` method. Similarly,
a client can determine all the data producers that it can share on behalf of using the `getAuthorizedBy` method.

## Sharing as an Authorizer
A client that has been given permission to share records on behalf of a writer can use the `shareOnBehalfOf` method:

```java
UUID writerId = <ID of data writer>;
UUID readerId = <ID of client we are sharing with>;
String recordType = <type of records to share>;

client.shareOnBehalfOf(WriterId.writerId(writerId), recordType, readerId, new ResultHandler<Void>() {
  @Override
  public void handle(Result<Void> r) {
    if(! r.isError()) {
      // permission given to reader
    }
  }
});
```

## Local Encryption & Decryption

The E3DB SDK allows you to encrypt documents for local storage, which can
be decrypted later, by the client that created the document or any client with which
the document has been `shared`. Note that locally encrypted documents *cannot* be
written directly to E3DB -- they must be decrypted locally and written using the `write` or
`update` methods.

Local encryption (and decryption) requires two steps:

1. Create a 'writer key' (for encryption) or obtain a 'reader key' (for decryption).
2. Call `encryptDocument` (for a new document) or `encryptExisting` (for an existing `Record` instance);
for decryption, call `decryptExisting`.

The 'writer key' and 'reader key' are both `EAKInfo` objects. An `EAKInfo` object holds an
encrypted key that can be used by the intended client to encrypt or decrypt associated documents. A
writer key can be created by calling `createWriterKey`; a 'reader key' can be obtained by calling
`getReaderKey`. (Note that the client calling `getReaderKey` will only receive a key if the writer
of those records has given access to the calling client through the `share` operation.)

Here is an example of encrypting a document locally:

```java
Client client = ...; // Get a client instance

Map<String, String> lyric = new HashMap<>();
lyric.put("line", "Say I'm the only bee in your bonnet");
lyric.put("song", "Birdhouse in Your Soul");
lyric.put("artist", "They Might Be Giants");

String recordType = "lyric";
client.createWriterKey(recordType, new ResultHandler<LocalEAKInfo>() {
    @Override
    public void handle(Result<LocalEAKInfo> r) {
        if(r.isError())
            throw new Error(r.asError().other());

        LocalEAKInfo key = r.asValue();
        LocalEncryptedRecord encrypted = client.encryptRecord(recordType, new RecordData(lyric), null, key);
        String encodedRecord = encrypted.encode();
        // Write `encodedRecord` to storage
    }
});
```

Note that the `LocalEAKInfo` instance is safe to store with the
encrypted data, as it is also encrypted. You can use the `encode` and
`decode` methods to convert `LocalEAKInfo` instances to and from
strings.

The client can decrypt the given record as follows:

```java
LocalEncryptedRecord encrypted = LocalEncryptedRecord.decode(...); // decode encrypted record from a string
LocalEAKInfo writerKey = LocalEAKInfo.decode(...); // decode LocalEAKInfo instance from a string

LocalRecord decrypted = client.decryptExisting(encrypted, writerKey);
```

## Local Decryption of Shared Records

When two clients have a sharing relationship, the "reader" can locally decrypt any documents encrypted
by the "writer," without using E3DB for storage.

The 'writer' must first share records with a 'reader', using the `share` method. The 'reader' can
then decrypt any locally encrypted records as follows:

```java
Client reader = ...; // Get a client instance

LocalEncryptedRecord encrypted = ...; // read encrypted record from local storage
UUID writerID = ...; // ID of writer that produced record
String recordType = "lyric";

reader.getReaderKey(writerID, writerID, reader.clientId(), recordType, new ResultHandler<LocalEAKInfo>() {
    @Override
    public void handle(Result<LocalEAKInfo> r) {
        if(r.isError())
            throw new Error(r.asError().other());

        LocalEAKInfo readerKey = r.asValue();
        LocalRecord decrypted = reader.decryptExisting(encrypted, readerKey);
    }
});
```

## Document Signing & Verification

Every E3DB client created with this SDK is capable of signing
documents and verifying the signature associated with a document. By
attaching signatures to documents, clients can be confident in:

  * Document integrity - the document's contents have not been altered
    (because the signature will not match).
  * Proof-of-authorship - The author of the document held the private
    signing key associated with the given public key when the document
    was created.

To create a signature, use the `sign` method:

```java
Client client = ...; // Get a client instance

final String recordType = "lyric";
final Map<String, String> plain = new HashMap<>();
plain.put("frabjous", "Filibuster vigilantly");
final Map<String, String> data = new HashMap<>();
data.put("Jabberwock", "Not to put too fine a point on it");
UUID writerId = client.clientId();
UUID userId = client.clientId();

LocalRecord local = new LocalRecord(data, new LocalMeta(writerId, userId, recordType, plain));
SignedDocument<LocalRecord> signed = client.sign(local);
```

To verify a document, use the `verify` method. Here, we use the same
`signed` instance as above. (Note that, in general, `verify` requires
the public signing key of the client that wrote the record):

```java
Config clientConfig = ...; // Retrieve config for client
if(! client.verify(signed, clientConfig.publicSigningKey)) {
  // Document failed verification, indicate an error as appropriate
}
```
## Reading and Writing Large Files

E3DB supports the storage of large encrypted files, using a similar interface for reading and writing records.
The SDK will handle encrypting and uploading the file. Similarly, it will download and decrypt files as well.

To write a file, use the `writeFile` method. For example, assuming the program can read this file (`README.md`):

```
...
File readmeFile = new File("README.md");
String recordType = "docs";

client.writeFile(recordType, readmeFile, null, new ResultHandler<RecordMeta>() {
    @Override
    public void handle(Result<RecordMeta> r) {
      if(! r.isError()) {
        // file written successfully
        RecordMeta record = r.asValue();
        // Log or print record ID, e.g.:
        System.out.println("Record ID: " + record.meta().recordId());
      }
      else {
        // an error occurred
        throw new RuntimeException(r.asError().other());
      }
    }
  }
);
```

Similarly, to read a file, use the `readFile` method. The `File` argument should be the destination that the plaintext
file will be written to. If the file already exists, it will be overwritten. Continuing the example above, you
could read the file written as follows:

```
...
UUID readmeRecordId = ...; // Record ID of file written previously
File destinationFile = new File("README-2.md");

client.readFile(readmeRecordId, destinationFile, new ResultHandler<RecordMeta>() {
    @Override
    public void handle(Result<RecordMeta> r) {
      if(r.isError()) {
        // an error occurred
        throw new RuntimeException(r.asError().other());
      }

      // `README-2.md` will contain contents of file, which we write to standard out.
      try {
        System.out.println("README-2.md: " + new String(Files.readAllBytes(destinationFile), "UTF-8"));
      }
      catch (IOException e) {
       // Handle error where file isn't found ...
      }
    }
  }
);
```

### Storage Requirements
When uploading a file, the SDK expects to be able to (temporarily) store an encrypted version of the plaintext
file in the same directory. Once the upload finishes (with or without error), the temporary file will be deleted.

When downloading a file, you must provide a location to which the file can be written. The SDK will save the encrypted
file to storage in the same directory as to where the plaintext file will be written. The SDK will decrypt
the encrypted file in that directory, ultimately writing the plaintext file to the destination given.

In both cases, uploading and downloading, the SDK expects at least twice as much free storage as the size of the plaintext or
encrypted file.

### Query Results

Query results may include large file records (uploaded via the `writeFile` method). A large file (vs. just a record) is indicated
when the `file()` method on `RecordMeta` returns a non-`null` value. The contents of the file will _not_ be included in
query results, even if `setIncludeData` is `true`. Use the `readFile` method to retrieve the contents of the file.

Note that the `data()` method will return an empty map (0 size) when the record refers to a large file. `data()` will never
return `null`, however.

### Reading Records

If the `read()` method is used to a read a record that refers to a file, the result will be the same as when a query
result contains a file record. Namely, the record's `data()` method will return an empty (0-size), non-`null` map. The
`file()` method on the object returned by `meta() will be non-`null`.

## Exceptions

The following E3DB-specific exceptions can be thrown:

* `E3DBException` - The base class for all E3DB-related exceptions. If a
  server-side error occurred that could not be classified, this
  exception will be thrown
* `E3DBForbiddenException` - The client does not have authorization to
  perform the given operation.
* `E3DBUnauthorizedException` - The client failed to authenticate with
  E3DB.
* `E3DBVersionException` - An update or delete was performed using a
  record with an out-of-date version.
* `E3DBNotFoundException` - The requested item could not be retrieved.
* `E3DBClientNotFoundException` - The given client (accessed via ID or
  email) could not be found.
* `E3DBVerificationException` - Thrown when signature verification fails while
  decrypting a locally-encrypted document.

# `e3db-fips` Submodule

The `e3db-fips` submodule is not used during the course of normal development and should be ignored. Only developers with
access to the submodule's repository should run submodule-related commands.
