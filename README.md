E3DB Java SDK
====

The Tozny End-to-End Encrypted Database (E3DB) is a storage platform
with powerful sharing and consent management features. [Read more on
our
blog.](https://tozny.com/blog/announcing-project-e3db-the-end-to-end-encrypted-database/)

This repo contains an E3DB SDK that can be used with both Android
devices and plain Java programs.

## Terms of Service

Your use of E3DB must abide by our [Terms of Service](terms.pdf), as detailed in
the linked document.

Getting Started
====

The E3DB SDK for Android and plain Java let's your application
interact with our end-to-end encrypted storage solution. Whether
used in an Android application or "plain" Java environment (such as a
server), the SDK presents the same API for using E3DB.

Before using the SDK, go to [Tozny's
Console](https://console.tozny.com), create an account, and go to
the `Manage Clients` section. Click the `Create Token` button under
the `Client Registration Tokens` heading. This value will allow your
app to self-register a new user with E3DB. Note that this value is not
meant to be secret and is safe to embed in your app.

## Documentation

Full API documentation for various versions can be found at the
following locations:

* [2.0](https://tozny.github.io/e3db-client-x/docs/2.0/) - The most recently released version of the client.
* [1.0.2](https://tozny.github.io/e3db-client-x/docs/1.0.2/) - An older, deprecated version of the client.

Code examples for the most common operations can be found below.

Using the SDK with Android
====

The E3DB SDK targets Android API 19 and higher. To use the SDK in your
app, add it as a dependency to your build. In Gradle, use:

```
compile('com.tozny.e3db:e3db-client-android:2.0.0@aar') {
    transitive = true
}
```

Because the SDK contacts Tozny's E3DB service, your application also
needs to request INTERNET permissions.

Using the SDK with Plain Java
====

For use with Maven, declare the following repository and dependencies:

```
<repositories>
  <repository>
    <id>tozny-repo</id>
    <name>Tozny Repository</name>
    <url>https://maven.tozny.com/repo</url>
  </repository>
</repositories>

<dependencies>
  <dependency>
    <groupId>com.tozny.e3db</groupId>
    <artifactId>e3db-client-plain</artifactId>
    <version>2.0.0</version>
  </dependency>
</dependencies>
```

libsodium
----

The plain Java SDK requires that the libsodium .so/.dll be on your
path. On Linux or MacOS, use a package manager to install libsodium.

Windows users should download a recent "MSVC" build of libsodium from
https://download.libsodium.org/libsodium/releases. Unzip the archive
and find the most recent "Release" version of libsodium.dll
for your architecture (32 or 64 bits), and copy that that file to a location
on your PATH environment variable.

Asynchronous Result Handling
====

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

Registering a Client
====

Registering creates a new client that can be used to interact with
E3DB. Each client has a unique ID and is associated with your Tozny
account. Registering only needs to happen once for a given client --
after credentials have been stored securely, the client can be
authenticated again using the stored credentials.

```java
import com.tozny.e3db.Client;
import com.tozny.e3db.ResultHandler;
import com.tozny.e3db.Result;
import com.tozny.e3db.Config;
...

String token = "<registration token>";
String host = "https://api.e3db.com";

Client.register(token, clientName, host, new ResultHandler<Config>() {
  @Override
  public void handle(Result<Config> r) {
    if(! r.isError()) {
      // write credentials to secure storage
      writeFile("credentials.json", r.asValue().json());
    }
    else {
      // throw to indicate registration error
      throw new RuntimeException(r.asError().other())
    }
  }
});

```

Using a Client to Interact with E3DB
====

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

Write a record
====

Records are represented as a `Map` with `String`-typed keys and
`String`-typed values. 

```java
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
        System.out.println("Record ID: " + record.recordId());
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

Query records
====

E3DB allows you to query records based on a number of criteria,
including record type. Use the `QueryParamsBuilder` object to build a
query: 

```java
QueryParams params = new QueryParamsBuilder()
  .setTypes("lyric")
  .setIncludeData(true)
  .setCount(50)
  .build();

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

Pagination
====

The `QueryResponse` object's method `last()` gives a value indicating the last
record returned. Passing this value to the `setAfter()` method on the `QueryParamsBuilder`
object will cause E3DB to return records that come "after" that value. For example, this
snippet will loop through all "lyric" records in 10-row increments:

```java
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

Sharing Records
====

E3DB allows the writer of a record to securely share that record with
other E3DB clients. To share, you must know the client ID of the
recipient. (The client ID of a given client is contained in the
response given when registering.)

Records are shared by `type`; the below shows sharing "lyric" records
with a recipient represented by the variable `readerId`:

```java
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


Exceptions
====

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

