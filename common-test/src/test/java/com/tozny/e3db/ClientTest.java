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

package com.tozny.e3db;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import okhttp3.CertificatePinner;
import org.junit.*;

import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;


public class ClientTest {
  private static final ObjectMapper mapper;
  private static final Map<String, String> profiles = new HashMap<>();
  private static final boolean INFINITE_WAIT = false;
  private static final int TIMEOUT = 5;
  private static final Charset UTF8 = Charset.forName("UTF-8");

  private static final String FIELD = "line";
  private static final String TYPE = "stuff";
  private static final String MESSAGE = "It's a simple message and I'm leaving out the whistles and bells";
  private static final String host;
  private static final String token;

  static {
    mapper = new ObjectMapper();
    mapper.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);

    try {
      // ensure we initialize Android-specific config before running
      // these tests.
      Class.forName("com.tozny.e3db.AppConfig");
    } catch (ClassNotFoundException e) {

    }

    String h = System.getProperty("e3db.host", System.getenv("DEFAULT_API_URL"));
    if(h == null)
      host = "https://api.e3db.com";
    else
      host = h;

    String t = System.getProperty("e3db.token", System.getenv("REGISTRATION_TOKEN"));
    if(t == null)
     throw new Error("Registration token must be set via environment variable or system property.");
    else
      token = t;
  }

  /**
   * Runs an asynchronous action that requires a countdown latch
   * to indicate when it has finished. After {@link #TIMEOUT} seconds, an error is thrown to
   * indicate the action did not completed in time.
   */
  private static void withTimeout(AsyncAction action) {
     try {
       CountDownLatch wait = new CountDownLatch(1);
       action.act(wait);
       if (!wait.await(INFINITE_WAIT ? Integer.MAX_VALUE : TIMEOUT, TimeUnit.SECONDS))
         throw new Error("Timed out.");
     }
     catch(Exception e) {
       throw new Error(e);
     }
  }

  /**
   * Wraps a result handler with logic to trigger a countdown latch, regardless of
   * the result of the handler.
   *
   * Should be used to wrap result handlers that contain assertion failures or that throw
   * exceptions. Otherwise, the count down latch may not be triggered and the test will
   * not complete until timeout occurs.
   * @param <T>
   */
  private static final class ResultWithWaiting<T> implements ResultHandler<T> {
    private final CountDownLatch waiter;
    private final ResultHandler<T> handler;

    public ResultWithWaiting(CountDownLatch waiter, ResultHandler<T> handler) {
      this.waiter = waiter;
      this.handler = handler;
    }

    @Override
    public void handle(Result<T> r) {
      try {
        if(handler != null)
          handler.handle(r);
      }
      finally {
        waiter.countDown();
      }
    }
  }

  private static class CI {
    public final Client client;
    public final Config clientConfig;

    private CI(Client client, Config clientConfig) {
      this.client = client;
      this.clientConfig = clientConfig;
    }
  }

  private static void registerProfile(final String profile, final ResultHandler<Config> handler) {
    final String clientName = UUID.randomUUID().toString();

    withTimeout(new AsyncAction() {
      @Override
      public void act(final CountDownLatch wait) throws Exception {
        Client.register(
            token, clientName, host, new ResultWithWaiting<Config>(wait, new ResultHandler<Config>() {
            @Override
            public void handle(Result<Config> r) {
              if (!r.isError()) {
                Config info = r.asValue();
                profiles.put(profile, info.json());
              }

              if(handler == null) {
                if (r.isError())
                  throw new Error(r.asError().other());
              }
              else
                handler.handle(r);
          }}));
      }
    });
  }

  private CI getClient() throws IOException {
    return getClient("default");
  }

  private CI getClient(String profile) throws IOException {
    final Config info = Config.fromJson(profiles.get(profile));
    final Client client = new ClientBuilder()
      .fromConfig(info)
      .build();
    return new CI(client, info);
  }

  private void writeRecord(Client client, final ResultHandler<Record> handleResult) throws IOException {
    writeRecord(client, TYPE, handleResult);
  }

  private void writeRecord(Client client, String recordType, final ResultHandler<Record> handleResult) throws IOException {
    Map<String, String> record = new HashMap<>();
    record.put(FIELD, MESSAGE);
    client.write(recordType, new RecordData(record), null, new ResultHandler<Record>() {
      @Override
      public void handle(Result<Record> r) {
        handleResult.handle(r);
      }
    });
  }

  @BeforeClass
  public static void registerDefault() throws Exception {
    registerProfile("default", null);
  }

  @Test
  public void testValidCertificatePin() throws IOException {
    final String clientName = UUID.randomUUID().toString();
    final String host = System.getProperty("e3db.host", "https://dev.e3db.com");
    final String token = System.getProperty("e3db.token", "1f991d79091ba4aaa1f333bef1929a10ed8c3f426fb6d3b1340a1157950d5bce");

    final CertificatePinner certificatePinner = new CertificatePinner.Builder()
            .add("dev.e3db.com", "sha256/YLh1dUR9y6Kja30RrAn7JKnbQG/uEtLMkBgFF2Fuihg=")
            .build();

    withTimeout(new AsyncAction() {
      @Override
      public void act(final CountDownLatch wait) throws Exception {
        Client.register(
                token, clientName, host, certificatePinner, new ResultWithWaiting<Config>(wait, new ResultHandler<Config>() {
                  @Override
                  public void handle(Result<Config> r) {
                    if (!r.isError()) assertTrue(true);

                    if (r.isError()) {
                      fail("Invalid certificate pin");
                      throw new Error(r.asError().other());
                    }
                  }}));
      }
    });
  }

  @Test
  public void testInValidCertificatePin() throws IOException {
    final String clientName = UUID.randomUUID().toString();
    final CertificatePinner certificatePinner = new CertificatePinner.Builder()
            .add(URI.create(host).getHost(), "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=")
            .build();

    withTimeout(new AsyncAction() {
      @Override
      public void act(final CountDownLatch wait) throws Exception {
        Client.register(
                token, clientName, host, certificatePinner, new ResultWithWaiting<Config>(wait, new ResultHandler<Config>() {
                  @Override
                  public void handle(Result<Config> r) {
                    if (!r.isError()) fail("Certificate pin should have been invalid!");

                    if (r.isError()) {
                      assertTrue(true);
                    }
                  }}));
      }
    });
  }

  @Test
  public void testAccessKeys() throws IOException {
    final CI clientInfo = getClient();
    final Config info = Config.fromJson(profiles.get("default"));
    final Map<String, String> data = new HashMap<>();
    final String type = UUID.randomUUID().toString() + "-type";

    data.put("galumphing", "Who watches over you");
    withTimeout(new AsyncAction() {
      @Override
      public void act(CountDownLatch wait) throws Exception {
        clientInfo.client.write(type, new RecordData(data), null,
          new ResultWithWaiting<Record>(wait, new ResultHandler<Record>() {
            @Override
            public void handle(Result<Record> r) {
              if(r.isError())
                throw new Error(r.asError().other());
            }
          }));
      }
    });

    withTimeout(new AsyncAction() {
      @Override
      public void act(CountDownLatch wait) throws Exception {
        clientInfo.client.getReaderKey(clientInfo.client.clientId(), clientInfo.client.clientId(), type,
          new ResultWithWaiting<>(wait, new ResultHandler<LocalEAKInfo>() {
            @Override
            public void handle(Result<LocalEAKInfo> r) {
              if(r.isError())
                throw new Error(r.asError().other());

              EAKInfo eakInfo = r.asValue();
              assertEquals("Authorizer should be this client.", eakInfo.getAuthorizerId(), clientInfo.client.clientId());
            }
          }));
      }
    });

    withTimeout(new AsyncAction() {
      @Override
      public void act(CountDownLatch wait) throws Exception {
        clientInfo.client.createWriterKey(type, new ResultWithWaiting<>(wait, new ResultHandler<LocalEAKInfo>() {
          @Override
          public void handle(Result<LocalEAKInfo> r) {
            if(r.isError())
              throw new Error(r.asError().other());

            EAKInfo eakInfo = r.asValue();
            assertEquals("Authorizer should be this client.", eakInfo.getAuthorizerId(), clientInfo.client.clientId());
          }
        }));
      }
    });
  }

  @Test
  public void testQuery() throws Exception {
    final AtomicReference<UUID> recordId = new AtomicReference<>();
    final Client client = getClient().client;
    withTimeout(new AsyncAction() {
      @Override
      public void act(CountDownLatch wait) throws Exception {
        writeRecord(client, new ResultWithWaiting<Record>(wait, new ResultHandler<Record>() {
          @Override
          public void handle(Result<Record> r) {
            if (r.isError())
              throw new Error(r.asError().other());

            recordId.set(r.asValue().meta().recordId());
          }
        }));

      }
    });

    withTimeout(new AsyncAction() {
      @Override
      public void act(CountDownLatch wait) throws Exception {
        client.query(new QueryParamsBuilder().setRecords(recordId.get()).setIncludeData(true).build(), new ResultWithWaiting<QueryResponse>(wait, new ResultHandler<QueryResponse>() {
          @Override
          public void handle(Result<QueryResponse> r) {
            if (r.isError())
              throw new Error(r.asError().other());

            assertEquals("Expected only 1 record", 1, r.asValue().records().size());
            assertEquals("Type did not match", TYPE, r.asValue().records().get(0).meta().type());
            assertEquals("Field did not match", MESSAGE, r.asValue().records().get(0).data().get(FIELD));
          }
        }));
      }
    });

    withTimeout(new AsyncAction() {
      @Override
      public void act(CountDownLatch wait) throws Exception {
        client.query(new QueryParamsBuilder().setTypes(TYPE).setIncludeData(true).build(), new ResultWithWaiting<QueryResponse>(wait, new ResultHandler<QueryResponse>() {
          @Override
          public void handle(Result<QueryResponse> r) {
            if (r.isError())
              throw new Error(r.asError().other());

            assertTrue("Expected some records", r.asValue().records().size() > 0);
            assertEquals("Type did not match", TYPE, r.asValue().records().get(0).meta().type());
          }
        }));
      }
    });
  }

  @Test
  public void testDelete() throws IOException, InterruptedException {
    final Client client = getClient().client;

    withTimeout(new AsyncAction() {
      @Override
      public void act(final CountDownLatch wait) throws Exception {
        writeRecord(client, new ResultHandler<Record>() {
          @Override
          public void handle(Result<Record> r) {
            if(r.isError())
              throw new Error(r.asError().other());

            final Record record = r.asValue();
            Map<String, String> fields = new HashMap<>();
            final String updatedMessage = "Not to put too fine a point on it";
            fields.put(FIELD, updatedMessage);
            client.delete(record.meta().recordId(), record.meta().version(), new ResultHandler<Void>() {
              @Override
              public void handle(Result<Void> r) {
                if(r.isError())
                  throw new Error(r.asError().other());

                client.read(record.meta().recordId(), new ResultHandler<Record>() {
                  @Override
                  public void handle(Result<Record> r) {
                    assertTrue("Should not be able to read record.", r.isError());
                    assertTrue("Should have got 404.", r.asError().error() instanceof E3DBNotFoundException);
                    new ResultWithWaiting<Void>(wait, new ResultHandler<Void>() {
                      @Override
                      public void handle(final Result<Void> r1) {
                        if(r1.isError())
                          throw new Error(r1.asError().other());
                      }
                    }).handle(new ValueResult<Void>(null));
                  }
                });
              }
            });
          }
        });
      }
    });
  }

  @Test
  public void testWrite() throws IOException, InterruptedException {
    final Client client = getClient().client;
    final AtomicReference<UUID> recordId = new AtomicReference<>();
    withTimeout(new AsyncAction() {
      @Override
      public void act(CountDownLatch wait) throws Exception {
        writeRecord(getClient().client, new ResultWithWaiting<Record>(wait, new ResultHandler<Record>() {
          @Override
          public void handle(Result<Record> r) {
            if(r.isError())
              throw new Error(r.asError().other());

            recordId.set(r.asValue().meta().recordId());
          }
        }));
      }
    });

    withTimeout(new AsyncAction() {
      @Override
      public void act(final CountDownLatch wait) throws Exception {
        client.read(recordId.get(), new ResultHandler<Record>() {
          @Override
          public void handle(Result<Record> r) {
            new ResultWithWaiting<Record>(wait, new ResultHandler<Record>() {
              @Override
              public void handle(final Result<Record> r1) {
                if(r1.isError())
                  throw new Error(r1.asError().other());

                assertEquals("line field did not match", MESSAGE, r1.asValue().data().get(FIELD));
              }
            }).handle(r);
          }
        });
      }
    });
  }

  @Test
  public void testPaging() throws InterruptedException, IOException {
    final Client client = getClient().client;
    final AtomicInteger pages = new AtomicInteger(0);
    final AtomicReference<Boolean> done = new AtomicReference<>(false);
    final AtomicLong last = new AtomicLong(-1L);
    final QueryParamsBuilder paramsBuilder = new QueryParamsBuilder().setTypes(TYPE).setIncludeData(true).setCount(1);

    withTimeout(new AsyncAction() {
      @Override
      public void act(CountDownLatch wait) throws Exception {
        writeRecord(client, new ResultWithWaiting<Record>(wait, new ResultHandler<Record>() {
          @Override
          public void handle(Result<Record> r) {
            if (r.isError())
              throw new Error(r.asError().other());
          }
        }));
      }
    });

    withTimeout(new AsyncAction() {
      @Override
      public void act(CountDownLatch wait) throws Exception {
        writeRecord(client, new ResultWithWaiting<Record>(wait, new ResultHandler<Record>() {
          @Override
          public void handle(Result<Record> r) {
            if (r.isError())
              throw new Error(r.asError().other());
          }
        }));
      }
    });

    while (!done.get()) {
      final CountDownLatch wait3 = new CountDownLatch(1);
      client.query(paramsBuilder.build(), new ResultWithWaiting<QueryResponse>(wait3, new ResultHandler<QueryResponse>() {
        @Override
        public void handle(Result<QueryResponse> r) {
          if(r.isError())
            throw new Error(r.asError().other());

          List<Record> records = r.asValue().records();
          if(records.size() == 0) {
            done.set(true);
          }
          else {
            pages.set(pages.get() + 1);
            paramsBuilder.setAfter(r.asValue().last());
          }
        }
      }));

      if(! wait3.await(30, TimeUnit.SECONDS)) {
        fail("Timed out during query execution. Pages: " + pages.get() + "; done: " + done.get() + "; last: " + last.get());
      }
    }

    assertTrue("At least 2 pages of data expected", pages.get() > 1);
    assertTrue("Query never finished looping", done.get());
  }

  @Test
  public void testRecordDataUpdate() throws IOException, InterruptedException {
    final Client client = getClient().client;
    final String updatedMessage = "Not to put too fine a point on it";
    withTimeout(new AsyncAction() {
      @Override
      public void act(final CountDownLatch wait) throws Exception {
        writeRecord(client, new ResultHandler<Record>() {
          @Override
          public void handle(Result<Record> r) {
            if(r.isError())
              throw new Error(r.asError().other());

            final Record record = r.asValue();
            Map<String, String> fields = new HashMap<>();
            fields.put(FIELD, updatedMessage);
            client.update(LocalUpdateMeta.fromRecordMeta(record.meta()), new RecordData(fields), null, new ResultWithWaiting<>(wait, new ResultHandler<Record>() {
              @Override
              public void handle(Result<Record> r) {
                if(r.isError())
                  throw new RuntimeException(r.asError().other());

                assertEquals("Record not updated", r.asValue().data().get(FIELD), updatedMessage);
              }
            }));
          }
        });
      }
    });
  }

  @Test
  public void testRecordDataCustomUpdateMetaClass() throws IOException, InterruptedException {
    final Client client = getClient().client;
    final String updatedMessage = "Not to put too fine a point on it";
    withTimeout(new AsyncAction() {
      @Override
      public void act(final CountDownLatch wait) throws Exception {
        writeRecord(client, new ResultHandler<Record>() {
          @Override
          public void handle(Result<Record> r) {
            if(r.isError())
              throw new Error(r.asError().other());

            final Record record = r.asValue();

            Map<String, String> fields = new HashMap<>();
            fields.put(FIELD, updatedMessage);

            client.update(new UpdateMeta() {
              @Override
              public String getType() {
                return record.meta().type();
              }

              @Override
              public UUID getRecordId() {
                return record.meta().recordId();
              }

              @Override
              public String getVersion() {
                return record.meta().version();
              }
            }, new RecordData(fields), null, new ResultWithWaiting<>(wait, new ResultHandler<Record>() {
              @Override
              public void handle(Result<Record> r) {
                if(r.isError())
                  throw new RuntimeException(r.asError().other());

                assertEquals("Record not updated", r.asValue().data().get(FIELD), updatedMessage);
              }
            }));
            ;
          }
        });
      }
    });
  }


  @Test
  public void testPlaintextMetaUpdate() throws InterruptedException, IOException {
    final AtomicReference<Client> clientRef = new AtomicReference<>();
    final AtomicReference<Record> recordRef = new AtomicReference<>();
    registerProfile(UUID.randomUUID().toString(), new ResultHandler<Config>() {
      @Override
      public void handle(Result<Config> r) {
        if(r.isError())
          throw new Error(r.asError().other());

        clientRef.set(new ClientBuilder().fromConfig(r.asValue()).build());
      }
    });

    withTimeout(new AsyncAction() {
      @Override
      public void act(CountDownLatch wait) throws Exception {
        writeRecord(clientRef.get(), new ResultWithWaiting<Record>(wait, new ResultHandler<Record>() {
          @Override
          public void handle(Result<Record> r) {
            if(r.isError())
              throw new Error(r.asError().other());

            recordRef.set(r.asValue());
          }
        }));
      }
    });

    final Map<String, String> plain = new HashMap<>();
    plain.put("foo", "bar");
    final JsonNode plainJson = mapper.readTree(mapper.writeValueAsString(plain));

    // Update with new plaintext
    withTimeout(new AsyncAction() {
      @Override
      public void act(CountDownLatch wait) throws Exception {
        clientRef.get().update(LocalUpdateMeta.fromRecordMeta(recordRef.get().meta()), new RecordData(recordRef.get().data()), plain, new ResultWithWaiting<Record>(wait, new ResultHandler<Record>() {
          @Override
          public void handle(Result<Record> r) {
            if (r.isError())
              throw new Error(r.asError().other());

            recordRef.set(r.asValue());
          }
        }));
      }
    });

    withTimeout(new AsyncAction() {
      @Override
      public void act(CountDownLatch wait) throws Exception {
        clientRef.get().read(recordRef.get().meta().recordId(), new ResultWithWaiting<Record>(wait, new ResultHandler<Record>() {
          @Override
          public void handle(Result<Record> r) {
            try {
              if(r.isError())
                throw new Error(r.asError().other());

              assertTrue("Plaintext missing.", r.asValue().meta().plain() != null);
              JsonNode jsonNode = mapper.readTree(mapper.writeValueAsString(r.asValue().meta().plain()));
              assertTrue("Plaintext key 'foo' missing.", jsonNode.has("foo"));
              assertEquals("Plaintext value incorrect", plainJson, jsonNode);
            } catch (IOException e) {
              throw new Error(e);
            }
          }
        }));
      }
    });

    // Update with null plaintext
    withTimeout(new AsyncAction() {
      @Override
      public void act(CountDownLatch wait) throws Exception {
        clientRef.get().update(LocalUpdateMeta.fromRecordMeta(recordRef.get().meta()), new RecordData(recordRef.get().data()), null, new ResultWithWaiting<Record>(wait, new ResultHandler<Record>() {
          @Override
          public void handle(Result<Record> r) {
            if (r.isError())
              throw new Error(r.asError().other());

            recordRef.set(r.asValue());
          }
        }));
      }
    });

    withTimeout(new AsyncAction() {
      @Override
      public void act(CountDownLatch wait) throws Exception {
        clientRef.get().read(recordRef.get().meta().recordId(), new ResultWithWaiting<Record>(wait, new ResultHandler<Record>() {
          @Override
          public void handle(Result<Record> r) {
            if(r.isError())
              throw new Error(r.asError().other());

            assertTrue("Plaintext missing.", r.asValue().meta().plain() != null);
            assertEquals("Plaintext should be empty.", 0, r.asValue().meta().plain().size());
          }
        }));
      }
    });

    // Update with empty plaintext
    withTimeout(new AsyncAction() {
      @Override
      public void act(CountDownLatch wait) throws Exception {
        clientRef.get().update(LocalUpdateMeta.fromRecordMeta(recordRef.get().meta()), new RecordData(recordRef.get().data()), new HashMap<String, String>(), new ResultWithWaiting<Record>(wait, new ResultHandler<Record>() {
          @Override
          public void handle(Result<Record> r) {
            if (r.isError())
              throw new Error(r.asError().other());

            recordRef.set(r.asValue());
          }
        }));
      }
    });

    withTimeout(new AsyncAction() {
      @Override
      public void act(CountDownLatch wait) throws Exception {
        clientRef.get().read(recordRef.get().meta().recordId(), new ResultWithWaiting<Record>(wait, new ResultHandler<Record>() {
          @Override
          public void handle(Result<Record> r) {
            if(r.isError())
              throw new Error(r.asError().other());

            assertTrue("Plaintext missing.", r.asValue().meta().plain() != null);
            assertEquals("Plaintext should be empty.", 0, r.asValue().meta().plain().size());
          }
        }));
      }
    });
  }

  @Test
  public void testSharing() throws Exception {
    // create a client to share with
    final String shareProfile = UUID.randomUUID().toString();
    final String fromProfile = UUID.randomUUID().toString();
    final String recordType = "lyric";
    final Map<String, String> cleartext = new HashMap<>();
    cleartext.put("song", "triangle man");
    cleartext.put(FIELD, "Is he a dot, or is he a speck?");
    final AtomicReference<UUID> recordIdRef = new AtomicReference<>();

    registerProfile(shareProfile, null);
    registerProfile(fromProfile, null);

    final CI from = getClient(fromProfile);
    final CI to = getClient(shareProfile);

    withTimeout(new AsyncAction() {
      @Override
      public void act(CountDownLatch wait) throws Exception {
        from.client.write(recordType, new RecordData(cleartext), null, new ResultWithWaiting<Record>(wait, new ResultHandler<Record>() {
          @Override
          public void handle(Result<Record> r) {
            if(r.isError())
              throw new Error(r.asError().other());
            recordIdRef.set(r.asValue().meta().recordId());
          }
        }));
      }
    });

    withTimeout(new AsyncAction() {
      @Override
      public void act(CountDownLatch wait) throws Exception {
        from.client.share(recordType, to.client.clientId(), new ResultWithWaiting<Void>(wait, new ResultHandler<Void>() {
          @Override
          public void handle(Result<Void> r) {
            if(r.isError())
              throw new Error(r.asError().other());
          }
        }));
      }
    });

    // query the record using share client to see if it exists
    withTimeout(new AsyncAction() {
      @Override
      public void act(CountDownLatch wait) throws Exception {
        to.client.read(recordIdRef.get(), new ResultWithWaiting<Record>(wait, new ResultHandler<Record>() {
          @Override
          public void handle(Result<Record> r) {
            if(r.isError())
              throw new Error(r.asError().other());

            Record record = r.asValue();
            assertEquals("Song field did not match", "triangle man", record.data().get("song"));
            assertEquals("line field did not match", "Is he a dot, or is he a speck?", record.data().get(FIELD));
          }
        }));
      }
    });

    // check from shares
    withTimeout(new AsyncAction() {
      @Override
      public void act(CountDownLatch wait) throws Exception {
        from.client.getIncomingSharing(new ResultWithWaiting<List<IncomingSharingPolicy>>(wait, new ResultHandler<List<IncomingSharingPolicy>>() {
          @Override
          public void handle(Result<List<IncomingSharingPolicy>> r) {
            if(r.isError())
              throw new Error(r.asError().other());

            assertTrue("From client should not have any records shared.", r.asValue().size() == 0);
          }
        }));
      }
    });

    withTimeout(new AsyncAction() {
      @Override
      public void act(CountDownLatch wait) throws Exception {
        from.client.getOutgoingSharing(new ResultWithWaiting<List<OutgoingSharingPolicy>>(wait, new ResultHandler<List<OutgoingSharingPolicy>>() {
          @Override
          public void handle(Result<List<OutgoingSharingPolicy>> r) {
            if(r.isError())
              throw new Error(r.asError().other());

            assertTrue("From client should share only one record type.", r.asValue().size() == 1);
            assertEquals("From client should share be sharing with to client.", to.client.clientId(), r.asValue().get(0).readerId);
            assertEquals("Record type did not match", recordType, r.asValue().get(0).type);
          }
        }));
      }
    });

    // check to shares
    withTimeout(new AsyncAction() {
      @Override
      public void act(CountDownLatch wait) throws Exception {
        to.client.getIncomingSharing(new ResultWithWaiting<List<IncomingSharingPolicy>>(wait, new ResultHandler<List<IncomingSharingPolicy>>() {
          @Override
          public void handle(Result<List<IncomingSharingPolicy>> r) {
            if(r.isError())
              throw new Error(r.asError().other());

            assertTrue("To client should have one record type shared.", r.asValue().size() == 1);
            assertEquals("Reader of shared record type did not match.", from.client.clientId(), r.asValue().get(0).writerId);
            assertEquals("Record type did not match", recordType, r.asValue().get(0).type);
          }
        }));
      }
    });

    withTimeout(new AsyncAction() {
      @Override
      public void act(CountDownLatch wait) throws Exception {
        to.client.getOutgoingSharing(new ResultWithWaiting<List<OutgoingSharingPolicy>>(wait, new ResultHandler<List<OutgoingSharingPolicy>>() {
          @Override
          public void handle(Result<List<OutgoingSharingPolicy>> r) {
            if(r.isError())
              throw new Error(r.asError().other());

            assertTrue("To client should not have any records shared.", r.asValue().size() == 0);
          }
        }));
      }
    });

    // revoke the record
    withTimeout(new AsyncAction() {
      @Override
      public void act(CountDownLatch wait) throws Exception {
        from.client.revoke(recordType, to.client.clientId(), new ResultWithWaiting<Void>(wait, new ResultHandler<Void>() {
          @Override
          public void handle(Result<Void> r) {
            if(r.isError())
              throw new Error(r.asError().other());
          }
        }));
      }
    });

    withTimeout(new AsyncAction() {
      @Override
      public void act(CountDownLatch wait) throws Exception {
        to.client.read(recordIdRef.get(), new ResultWithWaiting<Record>(wait, new ResultHandler<Record>() {
          @Override
          public void handle(Result<Record> r) {
            assertTrue("Should not be able to read record " + recordIdRef.get(), r.isError());
            assertTrue("Should have got 404.", r.asError().error() instanceof E3DBNotFoundException);
          }
        }));
      }
    });
  }

  @Test
  public void testWritePlain() throws InterruptedException, IOException {
    final AtomicReference<Client> clientRef = new AtomicReference<>();
    Map<String, String> poem = new HashMap<>();
    poem.put("line", "All mimsy were the borogoves,");
    final RecordData data = new RecordData(poem);

    registerProfile(UUID.randomUUID().toString(), new ResultHandler<Config>() {
      @Override
      public void handle(Result<Config> r) {
        if(r.isError())
          throw new Error(r.asError().other());

        clientRef.set(new ClientBuilder().fromConfig(r.asValue()).build());
      }
    });

    {
      // Write record
      final AtomicReference<Record> recordRef = new AtomicReference<>();
      final Map<String, String> plain = new HashMap<>();
      plain.put("author", "Lewis Carrol");
      final JsonNode plainJson = mapper.readTree(mapper.writeValueAsString(plain));
      withTimeout(new AsyncAction() {
        @Override
        public void act(CountDownLatch wait) throws Exception {
          clientRef.get().write("poem", data, plain, new ResultWithWaiting<Record>(wait, new ResultHandler<Record>() {
            @Override
            public void handle(Result<Record> r) {
              if(r.isError()) {
                throw new Error(r.asError().other());
              }

              recordRef.set(r.asValue());
            }
          }));
        }
      });

      withTimeout(new AsyncAction() {
        @Override
        public void act(CountDownLatch wait) throws Exception {
          clientRef.get().read(recordRef.get().meta().recordId(), new ResultWithWaiting<Record>(wait, new ResultHandler<Record>() {
            @Override
            public void handle(Result<Record> r) {
              try {
                if(r.isError())
                  throw new Error(r.asError().other());

                assertEquals("Plain did not match", plainJson, mapper.readTree(mapper.writeValueAsString(r.asValue().meta().plain())));
              } catch (IOException e) {
                throw new Error(e);
              }
            }
          }));
        }
      });
    }

    {
      // Write record
      final AtomicReference<Record> recordRef = new AtomicReference<>();
      final Map<String, String> plain = new HashMap<>();
      final JsonNode plainJson = mapper.readTree(mapper.writeValueAsString(plain));
      withTimeout(new AsyncAction() {
        @Override
        public void act(CountDownLatch wait) throws Exception {
          clientRef.get().write("poem", data, plain, new ResultWithWaiting<Record>(wait, new ResultHandler<Record>() {
            @Override
            public void handle(Result<Record> r) {
              if(r.isError())
                throw new Error(r.asError().other());

              recordRef.set(r.asValue());
            }
          }));
        }
      });

      withTimeout(new AsyncAction() {
        @Override
        public void act(CountDownLatch wait) throws Exception {
          clientRef.get().read(recordRef.get().meta().recordId(), new ResultWithWaiting<Record>(wait, new ResultHandler<Record>() {
            @Override
            public void handle(Result<Record> r) {
              try {
                if(r.isError())
                  throw new Error(r.asError().other());

                assertEquals("Plain did not match", plainJson, mapper.readTree(mapper.writeValueAsString(r.asValue().meta().plain())));
              } catch (IOException e) {
                throw new Error(e);
              }
            }
          }));
        }
      });
    }
  }

  @Test
  public void testVariadic() {
    QueryParamsBuilder builder = new QueryParamsBuilder();
    builder.setTypes((String[]) null);
    assertTrue(builder.getTypes() == null);
    builder.setTypes();
    assertTrue(builder.getTypes().size() == 0);
    builder.setTypes((String) null);
    assertTrue(builder.getTypes().size() == 1);
    builder.setTypes(null, null);
    assertTrue(builder.getTypes().size() == 2);
  }

  @Test
  public void testEncryptDecrypt() throws IOException, E3DBException {
    CI clientInfo = getClient();
    final Client client = clientInfo.client;
    final String type = UUID.randomUUID().toString() + "-type";
    final AtomicReference<EAKInfo> eakInfoRef = new AtomicReference<>();
    withTimeout(new AsyncAction() {
      @Override
      public void act(CountDownLatch wait) throws Exception {
        client.createWriterKey(type, new ResultWithWaiting<>(wait, new ResultHandler<LocalEAKInfo>() {
          @Override
          public void handle(Result<LocalEAKInfo> r) {
            if(r.isError())
              throw new Error(r.asError().other());

            eakInfoRef.set(r.asValue());
          }
        }));
      }
    });

    EAKInfo eakInfo = eakInfoRef.get();
    assertNotNull("EAK not returned", eakInfo);

    Map<String, String> plain = new HashMap<>();
    plain.put("frabjous", "Filibuster vigilantly");
    Map<String, String> data = new HashMap<>();
    data.put("Jabberwock", "Not to put too fine a point on it");

    EncryptedRecord encrypted = client.encryptRecord(type, new RecordData(data), plain, eakInfo);
    assertEquals("Writer ID not equal to client ID", encrypted.meta().writerId(), client.clientId());
    assertEquals("User ID not equal to client ID", encrypted.meta().userId(), client.clientId());
    assertEquals("Types not equal", encrypted.meta().type(), type);
    assertEquals("Plain meta not equal", mapper.writeValueAsString(encrypted.meta().plain()), mapper.writeValueAsString(plain));

    LocalRecord decrypted = client.decryptExisting(encrypted, eakInfo);
    assertEquals("Writer ID not equal to client ID", decrypted.meta().writerId(), client.clientId());
    assertEquals("User ID not equal to client ID", decrypted.meta().userId(), client.clientId());
    assertEquals("Types not equal", decrypted.meta().type(), type);
    assertEquals("Plain meta not equal", mapper.writeValueAsString(decrypted.meta().plain()), mapper.writeValueAsString(plain));

    assertEquals("Data not decrypted", decrypted.data().get("Jabberwock"), "Not to put too fine a point on it");
  }

  @Test
  public void testEncryptDecryptTwoClients() throws IOException, E3DBException {
    CI clientInfo1 = getClient();
    final Client client1 = clientInfo1.client;
    final String type = UUID.randomUUID().toString() + "-type";
    final AtomicReference<EAKInfo> writerKeyRef = new AtomicReference<>();

    final String client2Name = UUID.randomUUID().toString();
    registerProfile(client2Name, null);
    CI clientInfo2 = getClient(client2Name);
    final Client client2 = clientInfo2.client;

    withTimeout(new AsyncAction() {
      @Override
      public void act(CountDownLatch wait) throws Exception {
        client1.share(type, client2.clientId(), new ResultWithWaiting<Void>(wait, null));
      }
    });

    withTimeout(new AsyncAction() {
      @Override
      public void act(CountDownLatch wait) throws Exception {
        client1.createWriterKey(type, new ResultWithWaiting<>(wait, new ResultHandler<LocalEAKInfo>() {
          @Override
          public void handle(Result<LocalEAKInfo> r) {
            if(r.isError())
              throw new Error(r.asError().other());

            writerKeyRef.set(r.asValue());
          }
        }));
      }
    });

    EAKInfo writerKey = writerKeyRef.get();
    assertNotNull("EAK not returned", writerKey);

    Map<String, String> plain = new HashMap<>();
    plain.put("frabjous", "Filibuster vigilantly");
    Map<String, String> data = new HashMap<>();
    data.put("Jabberwock", "Not to put too fine a point on it");

    EncryptedRecord encrypted = client1.encryptRecord(type, new RecordData(data), plain, writerKey);
    assertEquals("Writer ID not equal to client ID", encrypted.meta().writerId(), client1.clientId());
    assertEquals("User ID not equal to client ID", encrypted.meta().userId(), client1.clientId());
    assertEquals("Types not equal", encrypted.meta().type(), type);
    assertEquals("Plain meta not equal", mapper.writeValueAsString(encrypted.meta().plain()), mapper.writeValueAsString(plain));


    final AtomicReference<EAKInfo> readerKeyRef = new AtomicReference<>();
    withTimeout(new AsyncAction() {
      @Override
      public void act(CountDownLatch wait) throws Exception {
        client2.getReaderKey(client1.clientId(), client1.clientId(), type, new ResultWithWaiting<>(wait, new ResultHandler<LocalEAKInfo>() {
          @Override
          public void handle(Result<LocalEAKInfo> r) {
            if(r.isError())
              throw new Error(r.asError().other());
            
            readerKeyRef.set(r.asValue());
          }
        }));
      }
    });

    EAKInfo readerKey = readerKeyRef.get();
    assertNotNull("EAK not returned", readerKey);
    
    LocalRecord decrypted = client2.decryptExisting(encrypted, readerKey);
    assertEquals("Writer ID not equal to client ID", decrypted.meta().writerId(), client1.clientId());
    assertEquals("User ID not equal to client ID", decrypted.meta().userId(), client1.clientId());
    assertEquals("Types not equal", decrypted.meta().type(), type);
    assertEquals("Plain meta not equal", mapper.writeValueAsString(decrypted.meta().plain()), mapper.writeValueAsString(plain));

    assertEquals("Data not decrypted", decrypted.data().get("Jabberwock"), "Not to put too fine a point on it");
  }

  @Test
  public void testSigning() throws IOException {
    final CI clientInfo1 = getClient();
    final Client client = clientInfo1.client;
    final String recordType = "lyric";
    final Map<String, String> data = new HashMap<>();
    data.put("Jabberwock", "Not to put too fine a point on it");
    {
      final Map<String, String> plain = new HashMap<>();
      plain.put("frabjous", "Filibuster vigilantly");

      LocalRecord local = new LocalRecord(data, new LocalMeta(client.clientId(), client.clientId(), recordType, plain));

      SignedDocument<LocalRecord> sign = client.sign(local);
      assertNotNull("Signed document is null", sign);
      assertNotNull("Signature absent", sign.signature());
      assertFalse("Signature empty", sign.signature().trim().length() == 0);
      assertTrue("Unable to verify document", client.verify(sign, clientInfo1.clientConfig.publicSigningKey));
    }

    {
      // read a remote record, sign it, verify signature
      withTimeout(new AsyncAction() {
        @Override
        public void act(CountDownLatch wait) throws Exception {
          client.write(recordType, new RecordData(data), null, new ResultWithWaiting<>(wait, new ResultHandler<Record>() {
            @Override
            public void handle(Result<Record> r) {
              if(r.isError())
                throw new RuntimeException(r.asError().other());

              assertTrue("Unable to verify document.", client.verify(client.sign(r.asValue()),
                  clientInfo1.clientConfig.publicSigningKey));

              {
                LocalRecord local = new LocalRecord(r.asValue().data(), LocalMeta.fromRecordMeta(r.asValue().meta()));
                assertTrue("Unable to verify document.", client.verify(client.sign(local),
                    clientInfo1.clientConfig.publicSigningKey)
                );
              }
            }
          }));
        }
      });
    }
  }

  @Test
  public void testPlainMetaSerialization() throws IOException {
    final CI clientInfo1 = getClient();
    final Client client = clientInfo1.client;
    final String recordType = "lyric";
    final Map<String, String> data = new HashMap<>();
    data.put("Jabberwock", "Not to put too fine a point on it");
    final Map<String, String> plain = new HashMap<>();

    LocalRecord local1 = new LocalRecord(data, new LocalMeta(client.clientId(), client.clientId(), recordType, null));
    LocalRecord local2 = new LocalRecord(data, new LocalMeta(client.clientId(), client.clientId(), recordType, plain));

    assertEquals("Absent plain and empty plain should serialize the same", local1.toSerialized(), local2.toSerialized());

    SignedDocument<LocalRecord> sign1 = client.sign(local1);
    SignedDocument<LocalRecord> sign2 = client.sign(local2);
    assertTrue("Unable to verify document", client.verify(sign1, clientInfo1.clientConfig.publicSigningKey));
    assertTrue("Unable to verify document", client.verify(sign2, clientInfo1.clientConfig.publicSigningKey));
  }

  @Test
  public void testExternalEncryptedRecord() throws IOException {
    if(Platform.crypto.suite() == CipherSuite.Sodium) {
      final CI clientInfo1 = getClient();
      final Client client = clientInfo1.client;
      final JsonNode extEncryptedRecord = mapper.readTree("{\"doc\":{\"data\":{\"test_field\":\"QWfE7PpAjTgih1E9jyqSGex32ouzu1iF3la8fWNO5wPp48U2F5Q6kK41_8hgymWn.HW-dBzttfU6Xui-o01lOdVqchXJXqfqQ.eo8zE8peRC9qSt2ZOE8_54kOF0bWBEovuZ4.zO56Or0Pu2IFSzQZRpuXLeinTHQl7g9-\"},\"meta\":{\"plain\":{\"client_pub_sig_key\":\"fcyEKo6HSZo9iebWAQnEemVfqpTUzzR0VNBqgJJG-LY\",\"server_sig_of_client_sig_key\":\"ZtmkUb6MJ-1LqpIbJadYl_PPH5JjHXKrBspprhzaD8rKM4ejGD8cJsSFO1DlR-r7u-DKsLUk82EJF65RnTmMDQ\"},\"type\":\"ticket\",\"user_id\":\"d405a1ce-e528-4946-8682-4c2369a26604\",\"writer_id\":\"d405a1ce-e528-4946-8682-4c2369a26604\"},\"rec_sig\":\"YsNbSXy0mVqsvgArmdESe6SkTAWFui8_NBn8ZRyxBfQHmJt7kwDU6szEqiRIaoZGrHsqgwS3uduLo_kzG6UeCA\"},\"sig\":\"iYc7G6ersNurZRr7_lWqoilr8Ve1d6HPZPPyC4YMXSvg7QvpUAHvjv4LsdMMDthk7vsVpoR0LYPC_SkIip7XCw\"}");
      final JsonNode doc = extEncryptedRecord.get("doc");
      final EncryptedRecord record = LocalEncryptedRecord.decode(mapper.writeValueAsString(doc));

      assertTrue("Unable to verify document.", client.verify(new SignedDocument<EncryptedRecord>() {
        @Override
        public String signature() {
          return extEncryptedRecord.get("sig").asText();
        }

        @Override
        public EncryptedRecord document() {
          return record;
        }
      }, doc.get("meta").get("plain").get("client_pub_sig_key").asText()));
    }
  }

  @Test
  public void testEncryptSigning() throws IOException {
    final CI clientInfo1 = getClient();
    final Client client = clientInfo1.client;
    final String recordType = "lyric";
    final Map<String, String> plain = new HashMap<>();
    plain.put("frabjous", "Filibuster vigilantly");
    final Map<String, String> data = new HashMap<>();
    data.put("Jabberwock", "Not to put too fine a point on it");

    final LocalRecord local = new LocalRecord(data, new LocalMeta(client.clientId(), client.clientId(), recordType, plain));

    withTimeout(new AsyncAction() {
      @Override
      public void act(CountDownLatch wait) throws Exception {
        client.createWriterKey(recordType, new ResultWithWaiting<>(wait, new ResultHandler<LocalEAKInfo>() {
          @Override
          public void handle(Result<LocalEAKInfo> result) {
            EAKInfo eak = result.asValue();
            final EncryptedRecord encrypted = client.encryptExisting(local, eak);

            assertNotNull(encrypted.signature());

            SignedDocument<LocalRecord> signed = new SignedDocument<LocalRecord>() {
              @Override
              public String signature() {
                return encrypted.signature();
              }

              @Override
              public LocalRecord document() {
                return local;
              }
            };

            assertTrue(client.verify(signed, clientInfo1.clientConfig.publicSigningKey));
          }
        }));
      }
    });
  }

  @Test
  public void testEncodeDecodeLocal() throws IOException, E3DBException {
    final CI clientInfo1 = getClient();
    final Client client = clientInfo1.client;
    final String recordType = "signedLyric";
    final AtomicReference<EAKInfo> eak = new AtomicReference<>();

    withTimeout(new AsyncAction() {
      @Override
      public void act(CountDownLatch wait) throws Exception {
        client.createWriterKey(recordType, new ResultWithWaiting<>(wait, new ResultHandler<LocalEAKInfo>() {
          @Override
          public void handle(Result<LocalEAKInfo> result) {
            if(result.isError())
              throw new Error(result.asError().other());

            eak.set(result.asValue());
          }
        }));
      }
    });

    Map<String, String> plain = new HashMap<>();
    plain.put("hello", "world");

    Map<String, String> data = new HashMap<>();
    data.put("Jabberwock", "Not to put too fine a point on it");

    LocalRecord unencrypted = new LocalRecord(data, new LocalMeta(client.clientId(), client.clientId(), recordType, null));
    LocalRecord unencryptedWithPlain = new LocalRecord(data, new LocalMeta(client.clientId(), client.clientId(), recordType, plain));
    LocalRecord unencryptedWithEmptyPlain = new LocalRecord(data, new LocalMeta(client.clientId(), client.clientId(), recordType, new HashMap<String, String>()));

    // encrypt, encode, decode
    {
      LocalEncryptedRecord encrypted = client.encryptExisting(unencrypted, eak.get());
      String encodedEncrypted = encrypted.encode();
      EncryptedRecord decodedEncrypted = LocalEncryptedRecord.decode(encodedEncrypted);
      assertNull("Plain should be absent. (" + encodedEncrypted + ")", decodedEncrypted.meta().plain());
      assertEquals("type not equal. (" + encodedEncrypted + ")", unencrypted.meta().type(), decodedEncrypted.meta().type());
      assertEquals("userId not equal. (" + encodedEncrypted + ")", unencrypted.meta().userId(), decodedEncrypted.meta().userId());
      assertEquals("writerId not equal. (" + encodedEncrypted + ")", unencrypted.meta().writerId(), decodedEncrypted.meta().writerId());
      assertEquals("Signature not equal. (" + encodedEncrypted + ")", encrypted.signature(), decodedEncrypted.signature());
      assertEquals("Encrypted data not equal. (" + encodedEncrypted + ")", encrypted.data().get("Jabberwock"), decodedEncrypted.data().get("Jabberwock"));
    }

    {
      LocalEncryptedRecord encrypted = client.encryptExisting(unencryptedWithPlain, eak.get());
      String encodedEncrypted = encrypted.encode();
      EncryptedRecord decodedEncrypted = LocalEncryptedRecord.decode(encodedEncrypted);
      assertEquals("type not equal. (" + encodedEncrypted + ")", unencrypted.meta().type(), decodedEncrypted.meta().type());
      assertEquals("userId not equal. (" + encodedEncrypted + ")", unencrypted.meta().userId(), decodedEncrypted.meta().userId());
      assertEquals("writerId not equal. (" + encodedEncrypted + ")", unencrypted.meta().writerId(), decodedEncrypted.meta().writerId());
      assertEquals("Signature not equal. (" + encodedEncrypted + ")", encrypted.signature(), decodedEncrypted.signature());
      assertEquals("Encrypted data not equal. (" + encodedEncrypted + ")", encrypted.data().get("Jabberwock"), decodedEncrypted.data().get("Jabberwock"));
      assertEquals("Plain should be equal. (" + encodedEncrypted + ")", unencryptedWithPlain.meta().plain().get("hello"), decodedEncrypted.meta().plain().get("hello"));
    }

    {
      LocalEncryptedRecord encrypted = client.encryptExisting(unencryptedWithEmptyPlain, eak.get());
      String encodedEncrypted = encrypted.encode();
      EncryptedRecord decodedEncrypted = LocalEncryptedRecord.decode(encodedEncrypted);
      assertEquals("Plain should be empty but present. (" + encodedEncrypted + ")", 0, decodedEncrypted.meta().plain().size());
      assertEquals("type not equal. (" + encodedEncrypted + ")", unencrypted.meta().type(), decodedEncrypted.meta().type());
      assertEquals("userId not equal. (" + encodedEncrypted + ")", unencrypted.meta().userId(), decodedEncrypted.meta().userId());
      assertEquals("writerId not equal. (" + encodedEncrypted + ")", unencrypted.meta().writerId(), decodedEncrypted.meta().writerId());
      assertEquals("Signature not equal. (" + encodedEncrypted + ")", encrypted.signature(), decodedEncrypted.signature());
      assertEquals("Encrypted data not equal. (" + encodedEncrypted + ")", encrypted.data().get("Jabberwock"), decodedEncrypted.data().get("Jabberwock"));
    }

    // encrypt, encode, decode, decrypt
    {
      LocalEncryptedRecord encryptedRecord = client.encryptExisting(unencrypted, eak.get());
      String encodedEncrypted = encryptedRecord.encode();
      EncryptedRecord decodedEncrypted = LocalEncryptedRecord.decode(encodedEncrypted);
      LocalRecord decodedDecrypted = client.decryptExisting(decodedEncrypted, eak.get());
      assertNull("Plain should be absent.", decodedDecrypted.meta().plain());
      assertEquals("type not equal.", unencrypted.meta().type(), decodedDecrypted.meta().type());
      assertEquals("userId not equal.", unencrypted.meta().userId(), decodedDecrypted.meta().userId());
      assertEquals("writerId not equal.", unencrypted.meta().writerId(), decodedDecrypted.meta().writerId());
      assertEquals("Data not decrypted", data.get("Jabberwock"), decodedDecrypted.data().get("Jabberwock"));
    }

    {
      LocalEncryptedRecord encryptedRecord = client.encryptExisting(unencryptedWithPlain, eak.get());
      String encodedEncrypted = encryptedRecord.encode();
      EncryptedRecord decodedEncrypted = LocalEncryptedRecord.decode(encodedEncrypted);
      LocalRecord decodedDecrypted = client.decryptExisting(decodedEncrypted, eak.get());
      assertEquals("Plain should be equal. (" + encodedEncrypted + ")", unencryptedWithPlain.meta().plain().get("hello"), decodedDecrypted.meta().plain().get("hello"));
      assertEquals("type not equal.", unencrypted.meta().type(), decodedDecrypted.meta().type());
      assertEquals("userId not equal.", unencrypted.meta().userId(), decodedDecrypted.meta().userId());
      assertEquals("writerId not equal.", unencrypted.meta().writerId(), decodedDecrypted.meta().writerId());
      assertEquals("Signature not equal.", encryptedRecord.signature(), decodedEncrypted.signature());
      assertEquals("Data not decrypted", data.get("Jabberwock"), decodedDecrypted.data().get("Jabberwock"));
    }

    {
      LocalEncryptedRecord encryptedRecord = client.encryptExisting(unencryptedWithEmptyPlain, eak.get());
      String encodedEncrypted = encryptedRecord.encode();
      LocalRecord decodedDecrypted = client.decryptExisting(LocalEncryptedRecord.decode(encodedEncrypted), eak.get());
      assertEquals("Plain should be empty but present. (" + encodedEncrypted + ")", 0, decodedDecrypted.meta().plain().size());
      assertEquals("type not equal.", unencrypted.meta().type(), decodedDecrypted.meta().type());
      assertEquals("userId not equal.", unencrypted.meta().userId(), decodedDecrypted.meta().userId());
      assertEquals("writerId not equal.", unencrypted.meta().writerId(), decodedDecrypted.meta().writerId());
      assertEquals("Data not decrypted", data.get("Jabberwock"), decodedDecrypted.data().get("Jabberwock"));
    }

    // badly encoded
    try {
      LocalEncryptedRecord.decode("foo");
      fail("Should not decode.");
    }
    catch(IOException ex) {

    }

    try {
      LocalEncryptedRecord.decode(client.encryptExisting(unencrypted, eak.get()).encode().substring(1));
      fail("Should not decode.");
    }
    catch(IOException ex) {

    }
  }

  @Test
  public void testEncodeDecodeWrite() throws IOException, E3DBException {
    final AtomicReference<Client> client1 = new AtomicReference<>();
    final AtomicReference<Client> client2 = new AtomicReference<>();
    final AtomicReference<LocalEAKInfo> writerEak = new AtomicReference<>();
    final AtomicReference<LocalEAKInfo> readerEak = new AtomicReference<>();
    final String recordType = "signedLyric";

    registerProfile(UUID.randomUUID().toString(), new ResultHandler<Config>() {
      @Override
      public void handle(Result<Config> r) {
        if(r.isError())
          throw new Error(r.asError().other());

        client1.set(new ClientBuilder().fromConfig(r.asValue()).build());
      }
    });

    registerProfile(UUID.randomUUID().toString(), new ResultHandler<Config>() {
      @Override
      public void handle(Result<Config> r) {
        if(r.isError())
          throw new Error(r.asError().other());

        client2.set(new ClientBuilder().fromConfig(r.asValue()).build());
      }
    });

    withTimeout(new AsyncAction() {
      @Override
      public void act(CountDownLatch wait) throws Exception {
        client1.get().createWriterKey(recordType, new ResultWithWaiting<>(wait, new ResultHandler<LocalEAKInfo>() {
          @Override
          public void handle(Result<LocalEAKInfo> r) {
            if (r.isError()) {
              if (r.asError().other() instanceof Error)
                throw (Error) r.asError().other();
              else
                throw new Error(r.asError().other());
            }

            writerEak.set(r.asValue());
          }
        }));
      }
    });

    withTimeout(new AsyncAction() {
      @Override
      public void act(CountDownLatch wait) throws Exception {
        client1.get().share(recordType, client2.get().clientId(), new ResultWithWaiting<Void>(wait, new ResultHandler<Void>() {
          @Override
          public void handle(Result<Void> r) {
            if (r.isError()) {
              if (r.asError().other() instanceof Error)
                throw (Error) r.asError().other();
              else
                throw new Error(r.asError().other());
            }

            new AtomicReference<Void>().set(r.asValue());
          }
        }));
      }
    });

    withTimeout(new AsyncAction() {
      @Override
      public void act(CountDownLatch wait) throws Exception {
        client2.get().getReaderKey(client1.get().clientId(), client1.get().clientId(), recordType, new ResultWithWaiting<>(wait, new ResultHandler<LocalEAKInfo>() {
          @Override
          public void handle(Result<LocalEAKInfo> r) {
            if (r.isError()) {
              if (r.asError().other() instanceof Error)
                throw (Error) r.asError().other();
              else
                throw new Error(r.asError().other());
            }

            readerEak.set(r.asValue());
          }
        }));
      }
    });

    Map<String, String> plain = new HashMap<>();
    plain.put("hello", "world");
    Map<String, String> data = new HashMap<>();
    data.put("Jabberwock", "Not to put too fine a point on it");

    LocalRecord unencrypted = new LocalRecord(data, new LocalMeta(client1.get().clientId(), client1.get().clientId(), recordType, null));
    LocalRecord unencryptedWithPlain = new LocalRecord(data, new LocalMeta(client1.get().clientId(), client1.get().clientId(), recordType, plain));
    LocalRecord unencryptedWithEmptyPlain = new LocalRecord(data, new LocalMeta(client1.get().clientId(), client1.get().clientId(), recordType, new HashMap<String, String>()));
    // client1: encrypt, encode
    // client2: decode, decrypt
    {
      LocalEncryptedRecord encrypted = client1.get().encryptExisting(unencrypted, writerEak.get());
      String encodedEncrypted = encrypted.encode();
      LocalRecord decodedDecrypted = client2.get().decryptExisting(LocalEncryptedRecord.decode(encodedEncrypted), readerEak.get());
      assertNull("Plain should be absent. (" + encodedEncrypted + ")", decodedDecrypted.meta().plain());
      assertEquals("type not equal. (" + encodedEncrypted + ")", unencrypted.meta().type(), decodedDecrypted.meta().type());
      assertEquals("userId not equal. (" + encodedEncrypted + ")", unencrypted.meta().userId(), decodedDecrypted.meta().userId());
      assertEquals("writerId not equal. (" + encodedEncrypted + ")", unencrypted.meta().writerId(), decodedDecrypted.meta().writerId());
      assertEquals("Decrypted data not equal. (" + encodedEncrypted + ")", unencrypted.data().get("Jabberwock"), decodedDecrypted.data().get("Jabberwock"));
    }

    {
      // Use a custom EncryptedRecord implementation.
      LocalEncryptedRecord encrypted = client1.get().encryptExisting(unencrypted, writerEak.get());
      final String encodedEncrypted = encrypted.encode();
      EncryptedRecord decoded = new EncryptedRecord() {
        private LocalEncryptedRecord x = LocalEncryptedRecord.decode(encodedEncrypted);

        @Override
        public ClientMeta meta() {
          return x.meta();
        }

        @Override
        public Map<String, String> data() {
          return x.data();
        }

        @Override
        public String signature() {
          return x.signature();
        }

        @Override
        public String toSerialized() {
          return x.toSerialized();
        }

        @Override
        public EncryptedRecord document() {
          return x.document();
        }
      };
      LocalRecord decodedDecrypted = client2.get().decryptExisting(decoded, readerEak.get());
      assertNull("Plain should be absent. (" + encodedEncrypted + ")", decodedDecrypted.meta().plain());
      assertEquals("type not equal. (" + encodedEncrypted + ")", unencrypted.meta().type(), decodedDecrypted.meta().type());
      assertEquals("userId not equal. (" + encodedEncrypted + ")", unencrypted.meta().userId(), decodedDecrypted.meta().userId());
      assertEquals("writerId not equal. (" + encodedEncrypted + ")", unencrypted.meta().writerId(), decodedDecrypted.meta().writerId());
      assertEquals("Decrypted data not equal. (" + encodedEncrypted + ")", unencrypted.data().get("Jabberwock"), decodedDecrypted.data().get("Jabberwock"));
    }

    {
      LocalEncryptedRecord encrypted = client1.get().encryptExisting(unencryptedWithPlain, writerEak.get());
      String encodedEncrypted = encrypted.encode();
      LocalRecord decodedDecrypted = client2.get().decryptExisting(LocalEncryptedRecord.decode(encodedEncrypted), readerEak.get());
      assertEquals("Plain should be equal. (" + encodedEncrypted + ")", unencryptedWithPlain.meta().plain().get("hello"), decodedDecrypted.meta().plain().get("hello"));
      assertEquals("type not equal. (" + encodedEncrypted + ")", unencrypted.meta().type(), decodedDecrypted.meta().type());
      assertEquals("userId not equal. (" + encodedEncrypted + ")", unencrypted.meta().userId(), decodedDecrypted.meta().userId());
      assertEquals("writerId not equal. (" + encodedEncrypted + ")", unencrypted.meta().writerId(), decodedDecrypted.meta().writerId());
      assertEquals("Decrypted data not equal. (" + encodedEncrypted + ")", unencrypted.data().get("Jabberwock"), decodedDecrypted.data().get("Jabberwock"));
    }

    {
      LocalEncryptedRecord encrypted = client1.get().encryptExisting(unencryptedWithEmptyPlain, writerEak.get());
      String encodedEncrypted = encrypted.encode();
      LocalRecord decodedDecrypted = client2.get().decryptExisting(LocalEncryptedRecord.decode(encodedEncrypted), readerEak.get());
      assertEquals("Plain should be empty but present. (" + encodedEncrypted + ")", 0, decodedDecrypted.meta().plain().size());
      assertEquals("type not equal. (" + encodedEncrypted + ")", unencrypted.meta().type(), decodedDecrypted.meta().type());
      assertEquals("userId not equal. (" + encodedEncrypted + ")", unencrypted.meta().userId(), decodedDecrypted.meta().userId());
      assertEquals("writerId not equal. (" + encodedEncrypted + ")", unencrypted.meta().writerId(), decodedDecrypted.meta().writerId());
      assertEquals("Decrypted data not equal. (" + encodedEncrypted + ")", unencrypted.data().get("Jabberwock"), decodedDecrypted.data().get("Jabberwock"));
    }
  }

  @Test
  public void testEAK() throws IOException, E3DBException {
    final CI clientInfo1 = getClient();
    final Client client = clientInfo1.client;
    final String type = UUID.randomUUID().toString();
    final AtomicReference<LocalEAKInfo> info = new AtomicReference<>();

    withTimeout(new AsyncAction() {
      @Override
      public void act(CountDownLatch wait) throws Exception {
        client.createWriterKey(type, new ResultWithWaiting<>(wait, new ResultHandler<LocalEAKInfo>() {
          @Override
          public void handle(Result<LocalEAKInfo> r) {
            if (r.isError()) {
              if (r.asError().other() instanceof Error)
                throw (Error) r.asError().other();
              else
                throw new Error(r.asError().other());
            }

            info.set(r.asValue());
          }
        }));
      }
    });

    // esnure we can encode/decode EAKs.
    String encodedEAK = info.get().encode();
    final LocalEAKInfo eak = LocalEAKInfo.decode(encodedEAK);
    Map<String, String> data = new HashMap<>();
    data.put("vorpal", "Bluebird of friendliness");
    assertNotNull("EAK not decoded: " + encodedEAK, eak);

    // ensure we can use a different EAKInfo implementation for encrypt/decrypt.
    EAKInfo localEAK = new EAKInfo() {
      @Override
      public String getKey() {
        return eak.getKey();
      }

      @Override
      public String getPublicKey() {
        return eak.getPublicKey();
      }

      @Override
      public UUID getAuthorizerId() {
        return eak.getAuthorizerId();
      }

      @Override
      public UUID getSignerId() {
        return eak.getSignerId();
      }

      @Override
      public String getSignerSigningKey() {
        return eak.getSignerSigningKey();
      }
    };
    String localEncryptedRecord = client.encryptRecord(type, new RecordData(data), null, localEAK).encode();

    LocalRecord decoded = client.decryptExisting(LocalEncryptedRecord.decode(localEncryptedRecord), localEAK);
    assertEquals("Decrypted record not equal to original.", data.get("vorpal"), decoded.data().get("vorpal"));
  }

  @Test
  public void testWriteFile() throws Throwable {
    final CI clientInfo1 = getClient();
    final Client client = clientInfo1.client;
    final String type = UUID.randomUUID().toString();
    final File plain = File.createTempFile("clientTest", ".txt");
    plain.deleteOnExit();
    String message = "Stately, plump Buck Mulligan came from the stairhead, bearing a bowl of\n" +
                         "lather on which a mirror and a razor lay crossed. A yellow dressinggown,\n" +
                         "ungirdled, was sustained gently behind him on the mild morning air.";
    FileOutputStream out = new FileOutputStream(plain);
    try {
      out.write(message.getBytes(UTF8));
    }
    finally {
      out.close();
    }

    final AtomicReference<RecordMeta> result = new AtomicReference<>();
    withTimeout(new AsyncAction() {
      @Override
      public void act(CountDownLatch wait) throws Exception {
        client.writeFile(type, plain, null, new ResultWithWaiting<RecordMeta>(wait, new ResultHandler<RecordMeta>() {
          @Override
          public void handle(Result<RecordMeta> r) {
            if (r.isError()) {
              if (r.asError().other() instanceof Error)
                throw (Error) r.asError().other();
              else
                throw new Error(r.asError().other());
            }

            result.set(r.asValue());
          }
        }));
      }
    });

    if(result.get() == null) {
      fail("Unable to write file.");
    }
  }

  @Test
  public void testAddAuthorizer() throws IOException {
    final CI writer = getClient();
    final String recordType = UUID.randomUUID().toString();
    final CI authorizer;
    {
      final AtomicReference<Config> result = new AtomicReference<>();
      String profile = UUID.randomUUID().toString();
      registerProfile(profile, null);
      authorizer = getClient(profile);
    }

    {
      final AtomicReference<Boolean> result = new AtomicReference<>();
      withTimeout(new AsyncAction() {
        @Override
        public void act(CountDownLatch wait) throws Exception {
          writer.client.addAuthorizer(authorizer.clientConfig.clientId, recordType, new ResultWithWaiting<>(wait, new ResultHandler<Void>() {
            @Override
            public void handle(Result r) {
              result.set(! r.isError());
            }
          }));
        }
      });

      if(! result.get())
        fail("Failed to add authorizer.");
    }
  }

  @Test
  public void testRemoveAuthorizer() throws IOException {
    final CI writer = getClient();
    final String recordType = UUID.randomUUID().toString();
    final CI authorizer;
    {
      final AtomicReference<Config> result = new AtomicReference<>();
      String profile = UUID.randomUUID().toString();
      registerProfile(profile, null);
      authorizer = getClient(profile);
    }

    {
      final AtomicReference<Boolean> result = new AtomicReference<>();
      withTimeout(new AsyncAction() {
        @Override
        public void act(CountDownLatch wait) throws Exception {
          writer.client.removeAuthorizer(authorizer.clientConfig.clientId, recordType, new ResultWithWaiting<>(wait, new ResultHandler<Void>() {
            @Override
            public void handle(Result r) {
              result.set(! r.isError());
            }
          }));
        }
      });

      if(! result.get())
        fail("Failed to remove authorizer.");
    }

    {
      final AtomicReference<Boolean> result = new AtomicReference<>();
      withTimeout(new AsyncAction() {
        @Override
        public void act(CountDownLatch wait) throws Exception {
          writer.client.removeAuthorizer(authorizer.clientConfig.clientId, new ResultWithWaiting<>(wait, new ResultHandler<Void>() {
            @Override
            public void handle(Result r) {
              result.set(! r.isError());
            }
          }));
        }
      });

      if(! result.get())
        fail("Failed to remove authorizer.");
    }
  }

  @Test
  public void testShareOnBehalfOf() throws IOException {
    final CI writer = getClient();
    final String recordType = UUID.randomUUID().toString().substring(0, 8);
    final CI reader;
    {
      final AtomicReference<Config> result = new AtomicReference<>();
      String profile = UUID.randomUUID().toString();
      registerProfile(profile, null);
      reader = getClient(profile);
    }
    final CI authorizer;
    {
      final AtomicReference<Config> result = new AtomicReference<>();
      String profile = UUID.randomUUID().toString();
      registerProfile(profile, null);
      authorizer = getClient(profile);
    }

    final Record writtenRecord;
    {
      final AtomicReference<Record> result = new AtomicReference<>();
      withTimeout(new AsyncAction() {
        @Override
        public void act(CountDownLatch wait) throws Exception {
          writeRecord(writer.client, recordType, new ResultWithWaiting<>(wait, new ResultHandler<Record>() {
            @Override
            public void handle(Result<Record> r) {
              if (r.isError()) {
                if (r.asError().other() instanceof Error)
                  throw (Error) r.asError().other();
                else
                  throw new Error(r.asError().other());
              }

              result.set(r.asValue());
            }
          }));
        }
      });

      writtenRecord = result.get();

      if(writtenRecord == null)
        fail("Did not write record.");
    }

    {
      final AtomicReference<E3DBNotFoundException> result = new AtomicReference<>();
      withTimeout(new AsyncAction() {
        @Override
        public void act(CountDownLatch wait) throws Exception {
          reader.client.read(writtenRecord.meta().recordId(), new ResultWithWaiting<>(wait, new ResultHandler<Record>() {
            @Override
            public void handle(Result<Record> r) {
              if(r.asError() != null && r.asError().other() instanceof E3DBNotFoundException)
                result.set((E3DBNotFoundException) r.asError().other());
            }
          }));
        }
      });

      if(result.get() == null)
        fail("Should not be able read record yet.");
    }

    {
      final AtomicReference<Boolean> result = new AtomicReference<>();
      withTimeout(new AsyncAction() {
        @Override
        public void act(CountDownLatch wait) throws Exception {
          writer.client.addAuthorizer(authorizer.clientConfig.clientId, recordType, new ResultWithWaiting<>(wait, new ResultHandler<Void>() {
            @Override
            public void handle(Result r) {
              if(r.isError())
                r.asError().other().printStackTrace();
              result.set(! r.isError());
            }
          }));
        }
      });

      if(! result.get())
        fail("Failed to add authorizer.");
    }

    {
      final AtomicReference<Boolean> result = new AtomicReference<>();
      withTimeout(new AsyncAction() {
        @Override
        public void act(CountDownLatch wait) throws Exception {
          authorizer.client.shareOnBehalfOf(WriterId.writerId(writer.client.clientId()), recordType, reader.client.clientId(), new ResultWithWaiting<>(wait, new ResultHandler<Void>() {
            @Override
            public void handle(Result r) {
              if(r.isError())
                r.asError().other().printStackTrace();
              result.set(! r.isError());
            }
          }));
        }
      });

      if(! result.get())
        fail("Failed to share with reader.");
    }

    {
      final AtomicReference<Record> result = new AtomicReference<>();
      withTimeout(new AsyncAction() {
        @Override
        public void act(CountDownLatch wait) throws Exception {
          reader.client.read(writtenRecord.meta().recordId(), new ResultWithWaiting<>(wait, new ResultHandler<Record>() {
            @Override
            public void handle(Result<Record> r) {
              if (r.isError()) {
                if (r.asError().other() instanceof Error)
                  throw (Error) r.asError().other();
                else
                  throw new Error(r.asError().other());
              }

              result.set(r.asValue());
            }
          }));
        }
      });

      if(result.get() == null)
        fail("Unable to read record.");
    }

    {
      final AtomicReference<Boolean> result = new AtomicReference<>();
      withTimeout(new AsyncAction() {
        @Override
        public void act(CountDownLatch wait) throws Exception {
          authorizer.client.revokeOnBehalfOf(WriterId.writerId(writer.client.clientId()), recordType, reader.client.clientId(), new ResultWithWaiting<>(wait, new ResultHandler<Void>() {
            @Override
            public void handle(Result r) {
              if(r.isError())
                r.asError().other().printStackTrace();
              result.set(! r.isError());
            }
          }));
        }
      });

      if(! result.get())
        fail("Failed to revoke reader.");
    }

    {
      final AtomicReference<E3DBNotFoundException> result = new AtomicReference<>();
      withTimeout(new AsyncAction() {
        @Override
        public void act(CountDownLatch wait) throws Exception {
          reader.client.read(writtenRecord.meta().recordId(), new ResultWithWaiting<>(wait, new ResultHandler<Record>() {
            @Override
            public void handle(Result<Record> r) {
              if(r.asError() != null && r.asError().other() instanceof E3DBNotFoundException)
                result.set((E3DBNotFoundException) r.asError().other());
            }
          }));
        }
      });

      if(result.get() == null)
        fail("Should not be able to read record.");
    }
  }

  @Test
  public void testAuthorizeBeforeWriting() throws IOException {
    final CI writer = getClient();
    final String recordType = UUID.randomUUID().toString().substring(0, 8);
    final CI reader;
    {
      final AtomicReference<Config> result = new AtomicReference<>();
      String profile = UUID.randomUUID().toString();
      registerProfile(profile, null);
      reader = getClient(profile);
    }
    final CI authorizer;
    {
      final AtomicReference<Config> result = new AtomicReference<>();
      String profile = UUID.randomUUID().toString();
      registerProfile(profile, null);
      authorizer = getClient(profile);
    }

    {
      final AtomicReference<Boolean> result = new AtomicReference<>();
      withTimeout(new AsyncAction() {
        @Override
        public void act(CountDownLatch wait) throws Exception {
          writer.client.addAuthorizer(authorizer.clientConfig.clientId, recordType, new ResultWithWaiting<>(wait, new ResultHandler<Void>() {
            @Override
            public void handle(Result r) {
              if(r.isError())
                r.asError().other().printStackTrace();
              result.set(! r.isError());
            }
          }));
        }
      });

      if(! result.get())
        fail("Failed to add authorizer.");
    }

    {
      final AtomicReference<Boolean> result = new AtomicReference<>();
      withTimeout(new AsyncAction() {
        @Override
        public void act(CountDownLatch wait) throws Exception {
          authorizer.client.shareOnBehalfOf(WriterId.writerId(writer.client.clientId()), recordType, reader.client.clientId(), new ResultWithWaiting<>(wait, new ResultHandler<Void>() {
            @Override
            public void handle(Result r) {
              if(r.isError())
                r.asError().other().printStackTrace();
              result.set(! r.isError());
            }
          }));
        }
      });

      if(! result.get())
        fail("Failed to share with reader.");
    }

    final Record writtenRecord;
    {
      final AtomicReference<Record> result = new AtomicReference<>();
      withTimeout(new AsyncAction() {
        @Override
        public void act(CountDownLatch wait) throws Exception {
          writeRecord(writer.client, recordType, new ResultWithWaiting<>(wait, new ResultHandler<Record>() {
            @Override
            public void handle(Result<Record> r) {
              if (r.isError()) {
                if (r.asError().other() instanceof Error)
                  throw (Error) r.asError().other();
                else
                  throw new Error(r.asError().other());
              }

              result.set(r.asValue());
            }
          }));
        }
      });

      writtenRecord = result.get();

      if(writtenRecord == null)
        fail("Did not write record.");
    }

    {
      final AtomicReference<Record> result = new AtomicReference<>();
      withTimeout(new AsyncAction() {
        @Override
        public void act(CountDownLatch wait) throws Exception {
          reader.client.read(writtenRecord.meta().recordId(), new ResultWithWaiting<>(wait, new ResultHandler<Record>() {
            @Override
            public void handle(Result<Record> r) {
              if (r.isError()) {
                if (r.asError().other() instanceof Error)
                  throw (Error) r.asError().other();
                else
                  throw new Error(r.asError().other());
              }

              result.set(r.asValue());
            }
          }));
        }
      });

      if(result.get() == null)
        fail("Unable to read record.");
    }

    {
      final AtomicReference<Boolean> result = new AtomicReference<>();
      withTimeout(new AsyncAction() {
        @Override
        public void act(CountDownLatch wait) throws Exception {
          authorizer.client.revokeOnBehalfOf(WriterId.writerId(writer.client.clientId()), recordType, reader.client.clientId(), new ResultWithWaiting<>(wait, new ResultHandler<Void>() {
            @Override
            public void handle(Result r) {
              if(r.isError())
                r.asError().other().printStackTrace();
              result.set(! r.isError());
            }
          }));
        }
      });

      if(! result.get())
        fail("Failed to revoke reader.");
    }

    {
      final AtomicReference<E3DBNotFoundException> result = new AtomicReference<>();
      withTimeout(new AsyncAction() {
        @Override
        public void act(CountDownLatch wait) throws Exception {
          reader.client.read(writtenRecord.meta().recordId(), new ResultWithWaiting<>(wait, new ResultHandler<Record>() {
            @Override
            public void handle(Result<Record> r) {
              if (r.isError() && r.asError().other() instanceof E3DBNotFoundException)
                result.set((E3DBNotFoundException) r.asError().other());
           }
          }));
        }
      });

      if(result.get() == null)
        fail("Should not be able to read record.");
    }
  }

  @Test
  public void testGetAuthorized() throws IOException {
    final CI writer = getClient();
    final String recordType = UUID.randomUUID().toString();
    final CI authorizer;
    {
      final AtomicReference<Config> result = new AtomicReference<>();
      String profile = UUID.randomUUID().toString();
      registerProfile(profile, null);
      authorizer = getClient(profile);
    }

    {
      final AtomicReference<Boolean> result = new AtomicReference<>();
      withTimeout(new AsyncAction() {
        @Override
        public void act(CountDownLatch wait) throws Exception {
          writer.client.addAuthorizer(authorizer.clientConfig.clientId, recordType, new ResultWithWaiting<>(wait, new ResultHandler<Void>() {
            @Override
            public void handle(Result r) {
              result.set(! r.isError());
            }
          }));
        }
      });

      if(! result.get())
        fail("Failed to add authorizer.");
    }

    {
      final AtomicReference<List<AuthorizerPolicy>> result = new AtomicReference<>();
      withTimeout(new AsyncAction() {
        @Override
        public void act(CountDownLatch wait) throws Exception {
          writer.client.getAuthorizers(new ResultWithWaiting<>(wait, new ResultHandler<List<AuthorizerPolicy>>() {
            @Override
            public void handle(Result<List<AuthorizerPolicy>> r) {
              if (r.isError()) {
                if (r.asError().other() instanceof Error)
                  throw (Error) r.asError().other();
                else
                  throw new Error(r.asError().other());
              }

              result.set(r.asValue());
            }
          }));
        }
      });

      if(result.get() == null || result.get().size() == 0)
        fail("Authorizer list is empty.");

      boolean found = false;
      for(AuthorizerPolicy policy : result.get()) {
        found = found || policy.authorizerId().equals(authorizer.client.clientId()) && policy.recordType().equalsIgnoreCase(recordType);
      }
      if(! found)
        fail("Authorizer not found in authorizer list.");
    }

    {
      final AtomicReference<List<AuthorizerPolicy>> result = new AtomicReference<>();
      withTimeout(new AsyncAction() {
        @Override
        public void act(CountDownLatch wait) throws Exception {
          authorizer.client.getAuthorizedBy(new ResultWithWaiting<>(wait, new ResultHandler<List<AuthorizerPolicy>>() {
            @Override
            public void handle(Result<List<AuthorizerPolicy>> r) {
              if (r.isError()) {
                if (r.asError().other() instanceof Error)
                  throw (Error) r.asError().other();
                else
                  throw new Error(r.asError().other());
              }

              result.set(r.asValue());
            }
          }));
        }
      });

      if(result.get() == null || result.get().size() == 0)
        fail("Authorized by list is empty.");

      boolean found = false;
      for(AuthorizerPolicy policy : result.get()) {
        found = found || policy.authorizedBy().equals(writer.client.clientId()) && policy.recordType().equalsIgnoreCase(recordType);
      }
      if(! found)
        fail("Writer not found in authorized by list.");
    }
  }
}
