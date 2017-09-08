package com.tozny.e3dbtest;

import android.os.Bundle;
import android.util.Log;

import com.tozny.e3db.Config;
import com.tozny.e3db.Client;
import com.tozny.e3db.ClientBuilder;
import com.tozny.e3db.E3DBNotFoundException;
import com.tozny.e3db.QueryParams;
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

  private void write1(final ResultHandler<Record> handleResult) throws IOException {
    Client client = getClient().client;

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

    CI from = getClient();
    CI to = getClient(shareProfile);
    Config toConfig = Config.fromJson(profiles.get(shareProfile));
    Config fromConfig = Config.fromJson(profiles.get("default"));

    // Log.i("info", "to: " + toConfig.json());
    // Log.i("info", "from: " + fromConfig.json());

    Map<String, String> cleartext = new HashMap<>();
    cleartext.put("song", "triangle man");
    cleartext.put(FIELD, "Is he a dot, or is he a speck?");
    final AtomicReference<UUID> recordIdRef = new AtomicReference<>();

    // Use default client to write a record
    {
      final CountDownLatch wait = new CountDownLatch(2);
      from.client.write("lyric", new RecordData(cleartext), null, new ResultHandler<Record>() {
        @Override
        public void handle(Result<Record> r) {
          assertFalse("Error writing record", r.isError());
          recordIdRef.set(r.asValue().meta().recordId());
          wait.countDown();
        }
      });
      wait.await(30, TimeUnit.SECONDS);

      from.client.share("lyric", to.clientId, new ResultHandler<Void>() {
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

    // revoke the record
    {
      final CountDownLatch wait = new CountDownLatch(2);

      from.client.revoke("lyric", to.clientId, new ResultHandler<Void>() {
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

    write1(new ResultHandler<Record>() {
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
    write1(new ResultHandler<Record>() {
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
        write1(new ResultHandler<Record>() {
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
        write1(new ResultHandler<Record>() {
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
}
