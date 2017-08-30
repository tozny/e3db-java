package com.tozny.e3dbtest;

import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.tozny.e3db.Config;
import com.tozny.e3db.Client;
import com.tozny.e3db.ClientBuilder;
import com.tozny.e3db.E3DBNotFoundException;
import com.tozny.e3db.Record;
import com.tozny.e3db.RecordData;
import com.tozny.e3db.Result;
import com.tozny.e3db.ResultHandler;
import com.tozny.e3db.crypto.AndroidCrypto;

import org.apache.commons.io.IOUtils;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

  public static final String INFO = "info";

  private enum Step {
    REGISTRATION,
    BEGIN,
    WRITE1,
    READ1,
    UPDATE1,
    SHARE1,
    DELETE1
  }

  public static final String COMPLETED = "completed";
  public static final String MESSAGE = "It's a simple message and I'm leaving out the whistles and bells";
  private final AndroidCrypto crypto = new AndroidCrypto();

  private Config loadClientInfo(Bundle savedInstanceState) throws IOException {
    return Config.fromJson(savedInstanceState.getString("regInfo"));
  }

  private Client getClient() throws IOException {
    String doc = new String(IOUtils.toByteArray(openFileInput("e3db.json")), "UTF-8");
    return new ClientBuilder()
      .fromClientInfo(Config.fromJson(doc))
      .build();
  }

  private void write1(ResultHandler<Record> handler) throws IOException {
    final Context ctx = this;
    Client client = getClient();

    Map<String, String> record = new HashMap<>();
    record.put("line", MESSAGE);
    client.write("stuff", new RecordData(record), null, handler);
  }

  private void registration(final String host, String token) {
    final String clientName = UUID.randomUUID().toString();
    final Context ctx = this;

    Client.register(
      token, clientName, host, new ResultHandler<Config>() {
        @Override
        public void handle(Result<Config> r) {
          try {
            if (! r.isError()) {
              Config info = r.asValue();
              Log.i(INFO, info.clientId.toString());
              FileOutputStream fileOutputStream = openFileOutput("e3db.json", Context.MODE_PRIVATE);
              try {
                fileOutputStream.write(info.json().getBytes());
                Intent regComplete = new Intent(ctx, MainActivity.class);
                regComplete.putExtra(COMPLETED, Step.REGISTRATION.toString());
                ctx.startActivity(regComplete);
              } finally {
                fileOutputStream.close();
              }
            }
            else {
              throw new RuntimeException(r.asError().other());
            }
          } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
          } catch (IOException e) {
            throw new RuntimeException(e);
          }

        }
      });
  }

  private void read1(UUID recordId) throws IOException {
    final Client client = getClient();
    final Context ctx = this;
    client.read(recordId, new ResultHandler<Record>() {
      @Override
      public void handle(Result<Record> r) {
        if(! r.isError()) {
          Intent intent = new Intent(ctx, MainActivity.class);
          intent.putExtra(COMPLETED, Step.READ1.toString());
          if(MESSAGE.compareTo(r.asValue().data().get("line")) != 0) throw new RuntimeException(MESSAGE + " != " + r.asValue().data().get("line"));
          intent.putExtra("recordId", r.asValue().meta().recordId().toString());
          startActivity(intent);
        }
        else{
          throw new RuntimeException(r.asError().other());
        }
      }
    });
  }

  private void share1(UUID type) throws IOException {
    final Context ctx = this;
    Client client = getClient();
    client.share((String) null, (UUID) null, new ResultHandler<Void>() {
      @Override
      public void handle(Result<Void> r) {
        if(! r.isError()) {

        }
        else {
          throw new RuntimeException(r.asError().other());
        }
      }
    });
  }

  private void delete1() throws IOException {
    final Context ctx = this;
    final Client client = getClient();

    write1(new ResultHandler<Record>() {
      @Override
      public void handle(Result<Record> r) {
        final Record record = r.asValue();
        Log.i(INFO, "Wrote record " + record.meta().recordId() + " with version " + record.meta().version());
        Map<String, String> fields = new HashMap<>();
        final String updatedMessage = "Not to put too fine a point on it";
        fields.put("line", updatedMessage);
        client.delete(record.meta().recordId(), record.meta().version(), new ResultHandler<Void>() {
          @Override
          public void handle(Result<Void> r) {
            if(r.isError()) throw new RuntimeException("Error", r.asError().other());
            client.read(record.meta().recordId(), new ResultHandler<Record>() {
              @Override
              public void handle(Result<Record> r) {
                if(! r.isError()) throw new RuntimeException("Should not be able to read record.");
                if(! (r.asError().error() instanceof E3DBNotFoundException))
                  throw new RuntimeException("Expected E3DBNotFoundException");
              }
            });
          }
        });
      }
    });
  }

  private void update1() throws IOException {
    final Context ctx = this;
    final Client client = getClient();

    write1(new ResultHandler<Record>() {
      @Override
      public void handle(Result<Record> r) {
        final Record record = r.asValue();
        Log.i(INFO, "Wrote record " + record.meta().recordId() + " with version " + record.meta().version());
        Map<String, String> fields = new HashMap<>();
        final String updatedMessage = "Not to put too fine a point on it";
        fields.put("line", updatedMessage);
        client.update(record.meta().recordId(), record.meta().version(), new RecordData(fields), null, record.meta().type(), new ResultHandler<Record>() {
          @Override
          public void handle(Result<Record> r) {
            if(r.isError()) throw new RuntimeException("Error", r.asError().other());
            if(r.asValue().data().get("line").compareTo(updatedMessage) != 0) throw new RuntimeException(r.asValue().data().get("line") + " != " + updatedMessage);

            Intent updateComplete = new Intent(ctx, MainActivity.class);
            updateComplete.putExtra(COMPLETED, Step.UPDATE1.toString());
            ctx.startActivity(updateComplete);

          }
        });
      }
    });
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    final Context ctx = this;
    Intent intent = getIntent();
    Step completed = intent != null && intent.hasExtra(COMPLETED) ? Step.valueOf(intent.getStringExtra(COMPLETED)) : Step.BEGIN;
    try {
      switch(completed) {
        case BEGIN:
          registration(System.getProperty("e3db.host", "https://dev.e3db.com"),
            System.getProperty("e3db.token", "6eb84defa08cff9c72e63de78933d9094c5ae5259972dd2153bf37fcb6cca733"));
          break;
        case REGISTRATION:
          write1(new ResultHandler<Record>() {
            @Override
            public void handle(Result<Record> r) {
              if (!r.isError()) {
                Record record = r.asValue();
                Intent intent = new Intent(ctx, MainActivity.class);
                intent.putExtra(COMPLETED, Step.WRITE1.toString());
                intent.putExtra("recordId", record.meta().recordId().toString());
                ctx.startActivity(intent);
              } else
                throw new RuntimeException(r.asError().other());
            }
          });
          break;
        case WRITE1:
          read1(UUID.fromString(intent.getStringExtra("recordId")));
          break;
        case READ1:
          update1();
          break;
        case UPDATE1:
          delete1();
          break;
        case DELETE1:
          share1(UUID.fromString(intent.getStringExtra("type")));
          break;
        case SHARE1:
          break;
        default:
          throw new RuntimeException("Unknown step: " + completed);
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  protected void onStart() {
    super.onStart();
  }
}
