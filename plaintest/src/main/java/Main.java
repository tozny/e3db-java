import com.tozny.e3db.Config;
import com.tozny.e3db.Client;
import com.tozny.e3db.ClientBuilder;
import com.tozny.e3db.QueryParams;
import com.tozny.e3db.QueryResponse;
import com.tozny.e3db.Record;
import com.tozny.e3db.RecordData;
import com.tozny.e3db.Result;
import com.tozny.e3db.ResultHandler;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.NoSuchElementException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class Main {

  public static void main(String [] args) throws IOException, InterruptedException {
    // This program does not run in Android Studio correctly. Instead, use gradlew to build
    // an uber jar and run that:
    //
    // > gradlew :plaintest:shadowJar
    // > java -cp plaintest\build\libs\plaintest-all.jar Main
    final CountDownLatch queryLatch = new CountDownLatch(1);
    final Client client = new ClientBuilder()
      .fromClientInfo(Config.fromJson(new String(Files.readAllBytes(Paths.get("C:\\Users\\Justin\\.tozny\\dev1\\e3db.json")), "UTF-8")))
      .build();

    client.query(QueryParams.ALL, new ResultHandler<QueryResponse>() {
      @Override
      public void handle(Result<QueryResponse> r) {
        if(r.isError()) {
          r.asError().other().printStackTrace();
        }
        else {
          for(Record record : r.asValue().records())
            try {
              System.out.println("Got pasted item: " + record.data().get("data"));
            } catch (NoSuchElementException e) {
              System.out.println("Record " + record.meta().recordId() + " does not have a 'data' field.");
            }

          if(r.asValue( ).records().size() > 0) {
            client.query(QueryParams.ALL.buildOn().setAfter(r.asValue().last()).build(), this);
          }
        }

        queryLatch.countDown();
      }
    });

    queryLatch.await(30, TimeUnit.SECONDS);
    final CountDownLatch writeLatch = new CountDownLatch(1);

    HashMap<String, String> record = new HashMap<>();
    record.put("particle man", "When he's underwater does he get wet?");

    HashMap<String, String> plain = new HashMap<>();
    plain.put("artist", "\"They Might Be Giants\"");
    plain.put("isMetallica", "false");
    plain.put("year", "1990");

    client.write("lyric", new RecordData(record), plain, new ResultHandler<Record>() {
      @Override
      public void handle(Result<Record> r) {
        if(r.isError())
          r.asError().other().printStackTrace();
        else {
          System.out.print("Wrote record: " + r.asValue().meta().recordId().toString());
        }

        writeLatch.countDown();
      }
    });

    writeLatch.await(30, TimeUnit.SECONDS);
  }
}
