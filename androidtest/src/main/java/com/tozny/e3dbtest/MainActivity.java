package com.tozny.e3dbtest;

import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import com.tozny.e3db.*;
import com.tozny.e3db.crypto.AndroidConfigStorageHelper;
import com.tozny.e3db.crypto.KeyProtection;

public class MainActivity extends AppCompatActivity {

    private static String token = "ce4ed7a4cf50ac5bf231938da4f4b9b5466768e602529eeb57ad1dabb2c66f90";
    private static String clientName = "LilliTest-";
    private static String host = "https://api.e3db.com";

    private static Client client = null;

    private Button button = null;
    private TextView label = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // TODO: If saved, don't re-register client

        // TODO: Save credentials ('securely') after registering client

        // TODO: Once registered, change button to instead send data to db (after retrieving saved credentials)

        // TODO: Add method to e3db to securely save credentials on app's behalf

        button = findViewById(R.id.register_client_button);
        label = findViewById(R.id.hello_label);

        try {

            AndroidConfigStorageHelper configStorageHelper = new AndroidConfigStorageHelper(MainActivity.this, "config", KeyProtection.withNone());
            //Config.removeConfigSecurely(configStorageHelper);
            Config config = Config.loadConfigSecurely(configStorageHelper);

            client = new ClientBuilder()
                    .fromConfig(config)
                    .build();

            button.setText("Send data");
            label.setText("Config loaded.");

        } catch (Exception e) {
            e.printStackTrace();

            label.setText(e.getLocalizedMessage());
        }

        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                if (MainActivity.client == null) {

                    Client.register(token, clientName + UUID.randomUUID().toString(), host, new ResultHandler<Config>() {
                        @Override
                        public void handle(Result<Config> r) {
                            if (!r.isError()) {
                                try {
                                    AndroidConfigStorageHelper configStorageHelper = new AndroidConfigStorageHelper(MainActivity.this, "config", KeyProtection.withNone());
                                    Config.saveConfigSecurely(configStorageHelper, r.asValue());

                                    client = new ClientBuilder()
                                            .fromConfig(r.asValue())
                                            .build();

                                    button.setText("Send data");
                                    label.setText("Client registered and config saved");

                                } catch (Exception e) {
                                    e.printStackTrace();
                                    label.setText(e.getLocalizedMessage());
                                }

                            } else {
                                label.setText("ERROR: " + (r.asError().error() == null ? r.asError().toString() : r.asError().error().getMessage()));

                            }
                        }
                    });

                } else {
                    Map<String, String> lyric = new HashMap<>();
                    lyric.put("line", "Say I'm the only bee in your bonnet");
                    lyric.put("song", "Birdhouse in Your Soul");
                    lyric.put("artist", "They Might Be Giants");

                    String recordType = "lyric";

                    client.write(recordType, new RecordData(lyric), null, new ResultHandler<Record>() {
                        @Override
                        public void handle(Result<Record> r) {
                            if (!r.isError()) {
                                label.setText("Record updated...");

                            } else {
                                label.setText("ERROR: " + (r.asError().error() == null ? r.asError().toString() : r.asError().error().getMessage()));

                            }
                        }
                    });

                    QueryParams params = new QueryParamsBuilder()
                            .setTypes("lyric")
                            .setIncludeData(true)
                            .setCount(100)
                            .build();

                    client.query(params, new ResultHandler<QueryResponse>() {
                        @Override
                        public void handle(Result<QueryResponse> r) {
                            if (!r.isError()) {
                                label.setText("Records currently set: " + r.asValue().records().size());

                            } else {
                                label.setText("ERROR: " + (r.asError().error() == null ? r.asError().toString() : r.asError().error().getMessage()));

                            }
                        }
                    });
                }
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
    }
}
