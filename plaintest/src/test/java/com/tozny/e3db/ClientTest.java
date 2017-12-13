package com.tozny.e3db;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import org.junit.*;

import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.*;

import java.io.IOException;
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

  private static final String FIELD = "line";
  private static final String TYPE = "stuff";
  public static final String MESSAGE = "It's a simple message and I'm leaving out the whistles and bells";

  static {
    mapper = new ObjectMapper();
    mapper.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
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
    final String host = System.getProperty("e3db.host", "https://dev.e3db.com");
    final String token = System.getProperty("e3db.token", "1f991d79091ba4aaa1f333bef1929a10ed8c3f426fb6d3b1340a1157950d5bce");

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

  private void write1(Client client, final ResultHandler<Record> handleResult) throws IOException {
    Map<String, String> record = new HashMap<>();
    record.put(FIELD, MESSAGE);
    client.write(TYPE, new RecordData(record), null, new ResultHandler<Record>() {
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
          new ResultWithWaiting<EAKInfo>(wait, new ResultHandler<EAKInfo>() {
            @Override
            public void handle(Result<EAKInfo> r) {
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
        clientInfo.client.createWriterKey(type, new ResultWithWaiting<EAKInfo>(wait, new ResultHandler<EAKInfo>() {
          @Override
          public void handle(Result<EAKInfo> r) {
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
        write1(client, new ResultWithWaiting<Record>(wait, new ResultHandler<Record>() {
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
        write1(client, new ResultHandler<Record>() {
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
        write1(getClient().client, new ResultWithWaiting<Record>(wait, new ResultHandler<Record>() {
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
        write1(client, new ResultWithWaiting<Record>(wait, new ResultHandler<Record>() {
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
        write1(client, new ResultWithWaiting<Record>(wait, new ResultHandler<Record>() {
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
        write1(client, new ResultHandler<Record>() {
          @Override
          public void handle(Result<Record> r) {
            if(r.isError())
              throw new Error(r.asError().other());

            final Record record = r.asValue();
            Map<String, String> fields = new HashMap<>();
            fields.put(FIELD, updatedMessage);
            client.update(record.meta(), new RecordData(fields), null, new ResultHandler<Record>() {
              @Override
              public void handle(Result<Record> r) {
                if(! r.isError())
                  assertEquals("Record not updated", r.asValue().data().get(FIELD), updatedMessage);
                new ResultWithWaiting<Record>(wait, new ResultHandler<Record>() {
                  @Override
                  public void handle(final Result<Record> r1) {
                    if(r1.isError())
                      throw new Error(r1.asError().other());
                  }
                }).handle(r);
              }
            });
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
        write1(clientRef.get(), new ResultWithWaiting<Record>(wait, new ResultHandler<Record>() {
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
        clientRef.get().update(recordRef.get().meta(), new RecordData(recordRef.get().data()), plain, new ResultWithWaiting<Record>(wait, new ResultHandler<Record>() {
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
        clientRef.get().update(recordRef.get().meta(), new RecordData(recordRef.get().data()), null, new ResultWithWaiting<Record>(wait, new ResultHandler<Record>() {
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
        clientRef.get().update(recordRef.get().meta(), new RecordData(recordRef.get().data()), new HashMap<String, String>(), new ResultWithWaiting<Record>(wait, new ResultHandler<Record>() {
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
    final String recordType = "lyric";
    final Map<String, String> cleartext = new HashMap<>();
    cleartext.put("song", "triangle man");
    cleartext.put(FIELD, "Is he a dot, or is he a speck?");
    final AtomicReference<UUID> recordIdRef = new AtomicReference<>();

    registerProfile(shareProfile, null);

    final CI from = getClient();
    final CI to = getClient(shareProfile);
    Config toConfig = Config.fromJson(profiles.get(shareProfile));
    Config fromConfig = Config.fromJson(profiles.get("default"));

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
  public void testEncryptDecrypt() throws IOException {
    CI clientInfo = getClient();
    final Client client = clientInfo.client;
    final String type = UUID.randomUUID().toString() + "-type";
    final AtomicReference<EAKInfo> eakInfoRef = new AtomicReference<>();
    withTimeout(new AsyncAction() {
      @Override
      public void act(CountDownLatch wait) throws Exception {
        client.createWriterKey(type, new ResultWithWaiting<EAKInfo>(wait, new ResultHandler<EAKInfo>() {
          @Override
          public void handle(Result<EAKInfo> r) {
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

    Record encrypted = client.encryptRecord(type, new RecordData(data), plain, eakInfo);
    assertEquals("Writer ID not equal to client ID", encrypted.meta().writerId(), client.clientId());
    assertEquals("User ID not equal to client ID", encrypted.meta().userId(), client.clientId());
    assertEquals("Types not equal", encrypted.meta().type(), type);
    assertEquals("Plain meta not equal", mapper.writeValueAsString(encrypted.meta().plain()), mapper.writeValueAsString(plain));

    Record decrypted = client.decryptExisting(encrypted, eakInfo);
    assertEquals("Writer ID not equal to client ID", decrypted.meta().writerId(), client.clientId());
    assertEquals("User ID not equal to client ID", decrypted.meta().userId(), client.clientId());
    assertEquals("Types not equal", decrypted.meta().type(), type);
    assertEquals("Plain meta not equal", mapper.writeValueAsString(decrypted.meta().plain()), mapper.writeValueAsString(plain));

    assertEquals("Data not decrypted", decrypted.data().get("Jabberwock"), "Not to put too fine a point on it");
  }

  @Test
  public void testEncryptDecryptTwoClients() throws IOException {
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
        client1.createWriterKey(type, new ResultWithWaiting<EAKInfo>(wait, new ResultHandler<EAKInfo>() {
          @Override
          public void handle(Result<EAKInfo> r) {
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

    Record encrypted = client1.encryptRecord(type, new RecordData(data), plain, writerKey);
    assertEquals("Writer ID not equal to client ID", encrypted.meta().writerId(), client1.clientId());
    assertEquals("User ID not equal to client ID", encrypted.meta().userId(), client1.clientId());
    assertEquals("Types not equal", encrypted.meta().type(), type);
    assertEquals("Plain meta not equal", mapper.writeValueAsString(encrypted.meta().plain()), mapper.writeValueAsString(plain));


    final AtomicReference<EAKInfo> readerKeyRef = new AtomicReference<>();
    withTimeout(new AsyncAction() {
      @Override
      public void act(CountDownLatch wait) throws Exception {
        client2.getReaderKey(client1.clientId(), client1.clientId(), type, new ResultWithWaiting<EAKInfo>(wait, new ResultHandler<EAKInfo>() {
          @Override
          public void handle(Result<EAKInfo> r) {
            if(r.isError())
              throw new Error(r.asError().other());
            
            readerKeyRef.set(r.asValue());
          }
        }));
      }
    });

    EAKInfo readerKey = readerKeyRef.get();
    assertNotNull("EAK not returned", readerKey);
    
    Record decrypted = client2.decryptExisting(encrypted, writerKey);
    assertEquals("Writer ID not equal to client ID", decrypted.meta().writerId(), client1.clientId());
    assertEquals("User ID not equal to client ID", decrypted.meta().userId(), client1.clientId());
    assertEquals("Types not equal", decrypted.meta().type(), type);
    assertEquals("Plain meta not equal", mapper.writeValueAsString(decrypted.meta().plain()), mapper.writeValueAsString(plain));

    assertEquals("Data not decrypted", decrypted.data().get("Jabberwock"), "Not to put too fine a point on it");
  }

  @Test
  public void testSigning() throws IOException {
    CI clientInfo1 = getClient();
    final Client client = clientInfo1.client;
    final String recordType = "lyric";
    final Map<String, String> plain = new HashMap<>();
    plain.put("frabjous", "Filibuster vigilantly");
    final Map<String, String> data = new HashMap<>();
    data.put("Jabberwock", "Not to put too fine a point on it");

    Record local = new LocalRecord(data, new LocalMeta(client.clientId(), client.clientId(), recordType, plain));

    SignedDocument<Record> sign = client.sign(local);
    assertNotNull("Signed document is null", sign);
    assertNotNull("Signature absent", sign.signature());
    assertFalse("Signature empty", sign.signature().trim().length() == 0);
    assertTrue("Unable to verify document", client.verify(sign, clientInfo1.clientConfig.publicSigningKey));
  }

}
