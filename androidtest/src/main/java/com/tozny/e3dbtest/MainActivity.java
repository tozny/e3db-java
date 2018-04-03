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
import com.tozny.e3db.Client;
import com.tozny.e3db.ResultHandler;
import com.tozny.e3db.Result;
import com.tozny.e3db.Config;

public class MainActivity extends AppCompatActivity {

    private static String token = "ce4ed7a4cf50ac5bf231938da4f4b9b5466768e602529eeb57ad1dabb2c66f90";
    private static String clientName = "LilliTest";
    private static String host = "https://api.e3db.com";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final Button button = findViewById(R.id.register_client_button);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Client.register(token, clientName, host, new ResultHandler<Config>() {
                    @Override
                    public void handle(Result<Config> r) {
                        if(! r.isError()) {
                            Log.d("BLAH", r.asValue().json());
                            // write credentials to secure storage
                            //writeFile("credentials.json", r.asValue().json());
                        }
                        else {
                            // throw to indicate registration error
                            throw new RuntimeException(r.asError().other());
                        }
                    }
                });
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();

    }
}
