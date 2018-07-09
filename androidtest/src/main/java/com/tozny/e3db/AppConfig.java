package com.tozny.e3db;

import android.util.Log;
import com.tozny.e3dbtest.BuildConfig;

public class AppConfig {
  public static final String registrationToken;
  public static final String defaultApiUrl;

  static {
    Log.i("AppConfig", "Initializing system properties.");
    defaultApiUrl = BuildConfig.DEFAULT_API_URL;
    registrationToken = BuildConfig.REGISTRATION_TOKEN;

    if(defaultApiUrl == null || defaultApiUrl.trim().length() == 0) {
      Log.e("AppConfig", "Default API URL not supplied");
      throw new Error("Default API URL not supplied");
    }

    if(registrationToken == null || registrationToken.trim().length() == 0) {
      Log.e("AppConfig", "Registration token not supplied");
      throw new Error("Registration token required.");
    }

    System.setProperty("e3db.host", defaultApiUrl);
    System.setProperty("e3db.token", registrationToken);
    Log.i("AppConfig", "host: " + System.getProperty("e3db.host"));
    Log.i("AppConfig", "token: " + System.getProperty("e3db.token"));
  }

}
