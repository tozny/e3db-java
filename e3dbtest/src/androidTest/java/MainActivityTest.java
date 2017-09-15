package com.tozny.e3dbtest;

import android.os.Bundle;
import android.util.ArrayMap;
import android.util.JsonReader;
import android.util.Log;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tozny.e3db.Config;
import com.tozny.e3db.Client;
import com.tozny.e3db.ClientBuilder;
import com.tozny.e3db.E3DBNotFoundException;
import com.tozny.e3db.IncomingSharingPolicy;
import com.tozny.e3db.OutgoingSharingPolicy;
import com.tozny.e3db.QueryParamsBuilder;
import com.tozny.e3db.QueryResponse;
import com.tozny.e3db.Record;
import com.tozny.e3db.RecordData;
import com.tozny.e3db.Result;
import com.tozny.e3db.ResultHandler;

import org.junit.Test;

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

import static junit.framework.Assert.*;

public class MainActivityTest {
  private static final ObjectMapper mapper = new ObjectMapper();

  public static final String INFO = "info";
  private static final String FIELD = "line";
  private static final String TYPE = "stuff";
  private Map<String, String> profiles = new HashMap<>();

  public static final String MESSAGE = "It's a simple message and I'm leaving out the whistles and bells";

  private static class CI {
    public final Client client;
    public final UUID clientId;

    private CI(Client client, UUID clientId) {
      this.client = client;
      this.clientId = clientId;
    }
  }

  private Config loadClientInfo(Bundle savedInstanceState) throws IOException {
    return Config.fromJson(savedInstanceState.getString("regInfo"));
  }

  private CI getClient() throws IOException {
    return getClient("default");
  }

  private CI getClient(String profile) throws IOException {
    final Config info = Config.fromJson(profiles.get(profile));
    final Client client = new ClientBuilder()
      .fromConfig(info)
      .build();
    return new CI(client, info.clientId);
  }

  private void write1(Client client, final ResultHandler<Record> handleResult) throws IOException {
    Map<String, String> record = new HashMap<>();
    record.put(FIELD, MESSAGE);
    client.write(TYPE, new RecordData(record), null, new ResultHandler<Record>() {
      @Override
      public void handle(Result<Record> r) {
        assertFalse("Error writing record", r.isError());
        handleResult.handle(r);
      }
    });
  }

  private void registerProfile(final String profile, final ResultHandler<Config> handler) {
    final String clientName = UUID.randomUUID().toString();
    final String host = System.getProperty("e3db.host", "https://dev.e3db.com");
    final String token = System.getProperty("e3db.token", "1f991d79091ba4aaa1f333bef1929a10ed8c3f426fb6d3b1340a1157950d5bce");

    Client.register(
      token, clientName, host, new ResultHandler<Config>() {
        @Override
        public void handle(Result<Config> r) {
          if (! r.isError()) {
            Config info = r.asValue();
            Log.i(INFO, info.clientId.toString());
            profiles.put(profile, info.json());
          }

          handler.handle(r);
        }
      });

  }

  private void read1(final UUID recordId, final ResultHandler<Record> handleResult) throws IOException {
    final Client client = getClient().client;
    client.read(recordId, new ResultHandler<Record>() {
      @Override
      public void handle(Result<Record> r) {
        assertFalse("Error reading record" + recordId, r.isError());
        handleResult.handle(r);
      }
    });
  }

  private void share1(final ResultHandler<Void> handleResult) throws IOException, InterruptedException {
    // create a client to share with
    final String shareProfile = UUID.randomUUID().toString();
    {
      final CountDownLatch wait = new CountDownLatch(1);
      registerProfile(shareProfile, new ResultHandler<Config>() {
        @Override
        public void handle(Result<Config> r) {
          assertFalse("Error creating shared profile.", r.isError());
          wait.countDown();
        }
      });
      wait.await(30, TimeUnit.SECONDS);
    }

    final CI from = getClient();
    final CI to = getClient(shareProfile);
    Config toConfig = Config.fromJson(profiles.get(shareProfile));
    Config fromConfig = Config.fromJson(profiles.get("default"));

    // Log.i("info", "to: " + toConfig.json());
    // Log.i("info", "from: " + fromConfig.json());

    final String recordType = "lyric";
    Map<String, String> cleartext = new HashMap<>();
    cleartext.put("song", "triangle man");
    cleartext.put(FIELD, "Is he a dot, or is he a speck?");
    final AtomicReference<UUID> recordIdRef = new AtomicReference<>();

    // Use default client to write a record
    {
      final CountDownLatch wait = new CountDownLatch(2);
      from.client.write(recordType, new RecordData(cleartext), null, new ResultHandler<Record>() {
        @Override
        public void handle(Result<Record> r) {
          assertFalse("Error writing record", r.isError());
          recordIdRef.set(r.asValue().meta().recordId());
          wait.countDown();
        }
      });
      wait.await(30, TimeUnit.SECONDS);

      from.client.share(recordType, to.clientId, new ResultHandler<Void>() {
        @Override
        public void handle(Result<Void> r) {
          assertFalse("Error sharing lyric record.", r.isError());
          wait.countDown();
        }
      });
      wait.await(30, TimeUnit.SECONDS);
    }

    // Log.i("info", "Shared record: " + recordIdRef.get() + " from client " + from.clientId + " to " + to.clientId);

    // query the record using share client to see if it exists
    {
      final CountDownLatch wait = new CountDownLatch(1);
      to.client.read(recordIdRef.get(), new ResultHandler<Record>() {
        @Override
        public void handle(Result<Record> r) {
          assertFalse("Error reading shared record.", r.isError());
          Record record = r.asValue();
          assertEquals("Song field did not match", "triangle man", record.data().get("song"));
          assertEquals("line field did not match", "Is he a dot, or is he a speck?", record.data().get(FIELD));
          wait.countDown();
        }
      });

      wait.await(30, TimeUnit.SECONDS);
    }

    {
      // check from shares
      from.client.getIncomingSharing(new ResultHandler<List<IncomingSharingPolicy>>() {
        @Override
        public void handle(Result<List<IncomingSharingPolicy>> r) {
          assertFalse("Incoming sharing gave an error", r.isError());
          assertTrue("From client should not have any records shared.", r.asValue().size() == 0);
        }
      });

      from.client.getOutgoingSharing(new ResultHandler<List<OutgoingSharingPolicy>>() {
        @Override
        public void handle(Result<List<OutgoingSharingPolicy>> r) {
          assertFalse("Outgoing sharing gave an error", r.isError());
          assertTrue("From client should share only one record type.", r.asValue().size() == 1);
          assertEquals("From client should share be sharing with to client.", to.clientId, r.asValue().get(0).readerId);
          assertEquals("Record type did not match", recordType, r.asValue().get(0).type);

        }
      });
    }

    {
      // check to shares
      to.client.getIncomingSharing(new ResultHandler<List<IncomingSharingPolicy>>() {
        @Override
        public void handle(Result<List<IncomingSharingPolicy>> r) {
          assertFalse("Incoming sharing gave an error", r.isError());
          assertTrue("To client should have one record type shared.", r.asValue().size() == 1);
          assertEquals("Reader of shared record type did not match.", from.clientId, r.asValue().get(0).writerId);
          assertEquals("Record type did not match", recordType, r.asValue().get(0).type);
        }
      });

      to.client.getOutgoingSharing(new ResultHandler<List<OutgoingSharingPolicy>>() {
        @Override
        public void handle(Result<List<OutgoingSharingPolicy>> r) {
          assertFalse("Outgoing sharing gave an error", r.isError());
          assertTrue("To client should not have any records shared.", r.asValue().size() == 0);
        }
      });
    }

    // revoke the record
    {
      final CountDownLatch wait = new CountDownLatch(2);

      from.client.revoke(recordType, to.clientId, new ResultHandler<Void>() {
        @Override
        public void handle(Result<Void> r) {
          assertFalse("Error revoking shared record.", r.isError());
          wait.countDown();
        }
      });
      wait.await(30, TimeUnit.SECONDS);

      to.client.read(recordIdRef.get(), new ResultHandler<Record>() {
        @Override
        public void handle(Result<Record> r) {
          assertTrue("Should not be able to read record " + recordIdRef.get(), r.isError());
          assertTrue("Should have got 404.", r.asError().error() instanceof E3DBNotFoundException);
          wait.countDown();
        }
      });

      wait.await(30, TimeUnit.SECONDS);
      handleResult.handle(null);
    }
  }

  private void delete1(final ResultHandler<Void> handleResult) throws IOException {
    final Client client = getClient().client;

    write1(client, new ResultHandler<Record>() {
      @Override
      public void handle(Result<Record> r) {
        final Record record = r.asValue();
        Log.i(INFO, "Wrote record " + record.meta().recordId() + " with version " + record.meta().version());
        Map<String, String> fields = new HashMap<>();
        final String updatedMessage = "Not to put too fine a point on it";
        fields.put(FIELD, updatedMessage);
        client.delete(record.meta().recordId(), record.meta().version(), new ResultHandler<Void>() {
          @Override
          public void handle(Result<Void> r) {
            assertFalse("Error deleting record", r.isError());
            client.read(record.meta().recordId(), new ResultHandler<Record>() {
              @Override
              public void handle(Result<Record> r) {
                assertTrue("Should not be able to read record.", r.isError());
                assertTrue("Should have got 404.", r.asError().error() instanceof E3DBNotFoundException);
                handleResult.handle(null);
              }
            });
          }
        });
      }
    });
  }

  private void update1(final ResultHandler<Record> handleResult) throws IOException {
    final Client client = getClient().client;
    final String updatedMessage = "Not to put too fine a point on it";
    write1(client, new ResultHandler<Record>() {
      @Override
      public void handle(Result<Record> r) {

        final Record record = r.asValue();
        Log.i(INFO, "Wrote record " + record.meta().recordId() + " with version " + record.meta().version());
        Map<String, String> fields = new HashMap<>();
        fields.put(FIELD, updatedMessage);
        client.update(record.meta(), new RecordData(fields), null, new ResultHandler<Record>() {
          @Override
          public void handle(Result<Record> r) {
            assertFalse("Error updating record: " + record.meta().recordId(), r.isError());
            assertEquals("Record not updated", r.asValue().data().get(FIELD), updatedMessage);
            handleResult.handle(r);
          }
        });
      }
    });
  }

  @Test
  public void test_all() {
    try {
      {
        final CountDownLatch wait = new CountDownLatch(1);
        registerProfile("default", new ResultHandler<Config>() {
          @Override
          public void handle(Result<Config> r) {
            wait.countDown();
          }
        });
        wait.await(30, TimeUnit.SECONDS);
      }

      {
        final AtomicReference<UUID> recordId = new AtomicReference<>();
        final CountDownLatch wait = new CountDownLatch(2);
        write1(getClient().client, new ResultHandler<Record>() {
          @Override
          public void handle(Result<Record> r) {
            recordId.set(r.asValue().meta().recordId());
            wait.countDown();
          }
        });
        wait.await(30, TimeUnit.SECONDS);

        read1(recordId.get(), new ResultHandler<Record>() {
          @Override
          public void handle(Result<Record> r) {
            assertEquals("line field did not match", MESSAGE,  r.asValue().data().get(FIELD));
            wait.countDown();
          }
        });
        wait.await(30, TimeUnit.SECONDS);
      }

      {
        final CountDownLatch wait = new CountDownLatch(1);
        update1(new ResultHandler<Record>() {
          @Override
          public void handle(Result<Record> r) {
            wait.countDown();
          }
        });
        wait.await(30, TimeUnit.SECONDS);
      }

      {
        final CountDownLatch wait = new CountDownLatch(1);
        delete1(new ResultHandler<Void>() {
          @Override
          public void handle(Result<Void> r) {
            wait.countDown();
          }
        });
        wait.await(30, TimeUnit.SECONDS);
      }

      {
        final CountDownLatch wait = new CountDownLatch(1);
        share1(new ResultHandler<Void>() {
          @Override
          public void handle(Result<Void> r) {
            wait.countDown();
          }
        });
        wait.await(30, TimeUnit.SECONDS);
      }

      {
        final AtomicReference<UUID> recordId = new AtomicReference<>();
        final CountDownLatch wait = new CountDownLatch(3);
        write1(getClient().client, new ResultHandler<Record>() {
          @Override
          public void handle(Result<Record> r) {
            recordId.set(r.asValue().meta().recordId());
            wait.countDown();
          }
        });
        wait.await(30, TimeUnit.SECONDS);

        final Client client = getClient().client;
        client.query(new QueryParamsBuilder().setRecords(recordId.get()).setIncludeData(true).build(), new ResultHandler<QueryResponse>() {
          @Override
          public void handle(Result<QueryResponse> r) {
            assertFalse("An error occurred during query", r.isError());
            assertEquals("Expected only 1 record", 1, r.asValue().records().size());
            assertEquals("Type did not match", TYPE, r.asValue().records().get(0).meta().type());
            assertEquals("Field did not match", MESSAGE, r.asValue().records().get(0).data().get(FIELD));
            wait.countDown();
          }
        });

        client.query(new QueryParamsBuilder().setTypes(TYPE).setIncludeData(true).build(), new ResultHandler<QueryResponse>() {
          @Override
          public void handle(Result<QueryResponse> r) {
            assertFalse("An error occurred during query", r.isError());
            assertTrue("Expected some records", r.asValue().records().size() > 0);
            assertEquals("Type did not match", TYPE, r.asValue().records().get(0).meta().type());
            wait.countDown();
          }
        });
        wait.await(30, TimeUnit.SECONDS);

        {
          final AtomicInteger pages = new AtomicInteger(0);
          final AtomicReference<Boolean> done = new AtomicReference<>(false);
          final AtomicLong last = new AtomicLong(-1L);
          final QueryParamsBuilder paramsBuilder = new QueryParamsBuilder().setTypes(TYPE).setIncludeData(true).setCount(1);
          while (!done.get()) {
            final CountDownLatch wait2 = new CountDownLatch(1);
            client.query(paramsBuilder.build(), new ResultHandler<QueryResponse>() {
              @Override
              public void handle(Result<QueryResponse> r) {
                assertFalse("An error occurred during query", r.isError());
                List<Record> records = r.asValue().records();
                if(records.size() == 0) {
                  done.set(true);
                }
                else {
                  pages.set(pages.get() + 1);
                  paramsBuilder.setAfter(r.asValue().last());
                }
                wait2.countDown();
              }
            });

            wait2.await(30, TimeUnit.SECONDS);
          }

          assertTrue("At least 2 pages of data expected", pages.get() > 1);
          assertTrue("Query never finished looping", done.get());
        }

      }
    } catch (Exception e) {
      fail("Exception during test: " + e);
    }
  }

  @Test
  public void testUpdate() throws InterruptedException, IOException {
    final AtomicReference<Client> clientRef = new AtomicReference<>();
    final AtomicReference<Record> recordRef = new AtomicReference<>();
    {
      final CountDownLatch wait = new CountDownLatch(1);
      registerProfile(UUID.randomUUID().toString(), new ResultHandler<Config>() {
        @Override
        public void handle(Result<Config> r) {
          if(r.isError())
            fail("Exception occurred " + r.asError().other());
          clientRef.set(new ClientBuilder().fromConfig(r.asValue()).build());
          wait.countDown();
        }
      });

      wait.await(30, TimeUnit.SECONDS);
    }

    {
      // Write record
      final CountDownLatch wait = new CountDownLatch(1);
      write1(clientRef.get(), new ResultHandler<Record>() {
        @Override
        public void handle(Result<Record> r) {
          if(r.isError())
            fail("Error occurred" + r.asError().other());

          recordRef.set(r.asValue());
          wait.countDown();
        }
      });
      wait.await(30, TimeUnit.SECONDS);
    }

    Map<String, String> plain = new ArrayMap<>();
    plain.put("foo", "bar");
    final JsonNode plainJson = mapper.readTree(mapper.writeValueAsString(plain));

    // Update with new plaintext
    {
      final CountDownLatch wait = new CountDownLatch(1);
      clientRef.get().update(recordRef.get().meta(), new RecordData(recordRef.get().data()), mapper.writeValueAsString(plain), new ResultHandler<Record>() {
        @Override
        public void handle(Result<Record> r) {
          if (r.isError())
            fail("Error updating record" + r.asError().other());

          recordRef.set(r.asValue());
          wait.countDown();
        }
      });
      wait.await(30, TimeUnit.SECONDS);
    }

    {
      final CountDownLatch wait = new CountDownLatch(1);
      clientRef.get().read(recordRef.get().meta().recordId(), new ResultHandler<Record>() {
        @Override
        public void handle(Result<Record> r) {
          try {
          if(r.isError())
            fail("Error reading record" + r.asError().other());

            assertTrue("Plaintext missing.", r.asValue().meta().plain() != null);
            JsonNode jsonNode = mapper.readTree(r.asValue().meta().plain());
            assertTrue("Plaintext key 'foo' missing.", jsonNode.has("foo"));
            assertEquals("Plaintext value incorrect", plainJson, jsonNode);
            wait.countDown();
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
        }
      });

      wait.await(30, TimeUnit.SECONDS);
    }

    // Update with null plaintext
    {
      final CountDownLatch wait = new CountDownLatch(1);
      clientRef.get().update(recordRef.get().meta(), new RecordData(recordRef.get().data()), null, new ResultHandler<Record>() {
        @Override
        public void handle(Result<Record> r) {
          if (r.isError())
            fail("Error updating record" + r.asError().other());

          recordRef.set(r.asValue());
          wait.countDown();
        }
      });
      wait.await(30, TimeUnit.SECONDS);
    }

    {
      final CountDownLatch wait = new CountDownLatch(1);
      clientRef.get().read(recordRef.get().meta().recordId(), new ResultHandler<Record>() {
        @Override
        public void handle(Result<Record> r) {
          try {
            if(r.isError())
              fail("Error reading record" + r.asError().other());

            assertTrue("Plaintext missing.", r.asValue().meta().plain() != null);
            assertEquals("Plaintext should be empty.", 0, mapper.readTree(r.asValue().meta().plain()).size());
            wait.countDown();
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
        }
      });

      wait.await(30, TimeUnit.SECONDS);
    }

    // Update with empty plaintext
    {
      final CountDownLatch wait = new CountDownLatch(1);
      clientRef.get().update(recordRef.get().meta(), new RecordData(recordRef.get().data()), mapper.writeValueAsString(new ArrayMap<String, String>()), new ResultHandler<Record>() {
        @Override
        public void handle(Result<Record> r) {
          if (r.isError())
            fail("Error updating record" + r.asError().other());

          recordRef.set(r.asValue());
          wait.countDown();
        }
      });
      wait.await(30, TimeUnit.SECONDS);
    }

    {
      final CountDownLatch wait = new CountDownLatch(1);
      clientRef.get().read(recordRef.get().meta().recordId(), new ResultHandler<Record>() {
        @Override
        public void handle(Result<Record> r) {
          try {
            if(r.isError())
              fail("Error reading record" + r.asError().other());

            assertTrue("Plaintext missing.", r.asValue().meta().plain() != null);
            assertEquals("Plaintext should be empty.", 0, mapper.readTree(r.asValue().meta().plain()).size());
            wait.countDown();
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
        }
      });

      wait.await(30, TimeUnit.SECONDS);
    }

  }

  @Test
  public void testWritePlain() throws InterruptedException, IOException {
    final AtomicReference<Client> clientRef = new AtomicReference<>();
    {
      final CountDownLatch wait = new CountDownLatch(1);
      registerProfile(UUID.randomUUID().toString(), new ResultHandler<Config>() {
        @Override
        public void handle(Result<Config> r) {
          if(r.isError())
            fail("Exception occurred " + r.asError().other());
          clientRef.set(new ClientBuilder().fromConfig(r.asValue()).build());
          wait.countDown();
        }
      });

      wait.await(30, TimeUnit.SECONDS);
    }

    Map<String, String> poem = new ArrayMap<>();
    poem.put("line", "All mimsy were the borogoves,");
    RecordData data = new RecordData(poem);

    {
      // Write record
      final AtomicReference<Record> recordRef = new AtomicReference<>();
      final CountDownLatch wait1 = new CountDownLatch(1);
      Map<String, Object> plain = new ArrayMap<>();
      plain.put("author", "Lewis Carrol");
      final JsonNode plainJson = mapper.readTree(mapper.writeValueAsString(plain));
      clientRef.get().write("poem", data, mapper.writeValueAsString(plain), new ResultHandler<Record>() {
        @Override
        public void handle(Result<Record> r) {
          if(r.isError())
            fail("Error occurred" + r.asError().other());

          recordRef.set(r.asValue());
          wait1.countDown();
        }
      });
      wait1.await(30, TimeUnit.SECONDS);

      final CountDownLatch wait2 = new CountDownLatch(1);
      clientRef.get().read(recordRef.get().meta().recordId(), new ResultHandler<Record>() {
        @Override
        public void handle(Result<Record> r) {
          try {
            if(r.isError())
              fail("Read failed: " + r.asError().other());
            assertEquals("Plain did not match", plainJson, mapper.readTree(r.asValue().meta().plain()));
            wait2.countDown();
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
        }
      });
      wait2.await(30, TimeUnit.SECONDS);
    }

    {
      // Write record
      final AtomicReference<Record> recordRef = new AtomicReference<>();
      final CountDownLatch wait1 = new CountDownLatch(1);
      Map<String, Object> plain = new ArrayMap<>();
      final JsonNode plainJson = mapper.readTree(mapper.writeValueAsString(plain));
      clientRef.get().write("poem", data, mapper.writeValueAsString(plain), new ResultHandler<Record>() {
        @Override
        public void handle(Result<Record> r) {
          if(r.isError())
            fail("Error occurred" + r.asError().other());

          recordRef.set(r.asValue());
          wait1.countDown();
        }
      });
      wait1.await(30, TimeUnit.SECONDS);

      final CountDownLatch wait2 = new CountDownLatch(1);
      clientRef.get().read(recordRef.get().meta().recordId(), new ResultHandler<Record>() {
        @Override
        public void handle(Result<Record> r) {
          try {
            if(r.isError())
              fail("Read failed: " + r.asError().other());
            assertEquals("Plain did not match", plainJson, mapper.readTree(r.asValue().meta().plain()));
            wait2.countDown();
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
        }
      });
      wait2.await(30, TimeUnit.SECONDS);
    }
  }
}
