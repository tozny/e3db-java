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

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
  }

  @Override
  protected void onStart() {
    super.onStart();
  }
}
