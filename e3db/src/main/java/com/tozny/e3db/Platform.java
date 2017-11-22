package com.tozny.e3db;

import com.tozny.e3db.crypto.AndroidCrypto;
import com.tozny.e3db.crypto.KaliumCrypto;

final class Platform {
  public static final Crypto crypto;
  static {
    if (Platform.isAndroid()) {
      crypto = new AndroidCrypto();
    } else {
      crypto = new KaliumCrypto();
    }
  }

  static boolean isAndroid() {
    boolean isAndroid = false;
    try {
      Class.forName("android.os.Build");
      isAndroid = true;
    } catch (ClassNotFoundException ignored) {
    }
    return isAndroid;
  }
}
