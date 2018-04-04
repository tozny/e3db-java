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
import com.tozny.e3db.*;

public class MainActivity extends AppCompatActivity {

    private static String token = "ce4ed7a4cf50ac5bf231938da4f4b9b5466768e602529eeb57ad1dabb2c66f90";
    private static String clientName = "LilliTest-";
    private static String host = "https://api.e3db.com";

    private static String config = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // TODO: If saved, don't re-register client

        // TODO: Save credentials ('securely') after registering client

        // TODO: Once registered, change button to instead send data to db (after retrieving saved credentials)

        // TODO: Add method to e3db to securely save credentials on app's behalf

        final Button button = findViewById(R.id.register_client_button);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                if (MainActivity.config == null) {

                    Client.register(token, clientName + UUID.randomUUID().toString(), host, new ResultHandler<Config>() {
                        @Override
                        public void handle(Result<Config> r) {
                            if (!r.isError()) {
                                Log.d("BLAH", r.asValue().json());

                                config = r.asValue().json();

                                // write credentials to secure storage
                                //writeFile("credentials.json", r.asValue().json());
                            } else {
                                // throw to indicate registration error
                                throw new RuntimeException(r.asError().other());
                            }
                        }
                    });

                    button.setText("Send data");
                } else {
                    try {
                        Client client = new ClientBuilder()
                                .fromConfig(Config.fromJson(MainActivity.config))
                                .build();

                        Map<String, String> lyric = new HashMap<>();
                        lyric.put("line", "Say I'm the only bee in your bonnet");
                        lyric.put("song", "Birdhouse in Your Soul");
                        lyric.put("artist", "They Might Be Giants");

                        String recordType = "lyric";

                        client.write(recordType, new RecordData(lyric), null, new ResultHandler<Record>() {
                            @Override
                            public void handle(Result<Record> r) {
                                if (!r.isError()) {

                                    //Record record = r.asValue();

                                    Log.d("BLAH BLAH", r.asValue().toString());

                                } else {
                                    // an error occurred
                                    throw new RuntimeException(r.asError().other());
                                }
                            }
                        });
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
    }
}
