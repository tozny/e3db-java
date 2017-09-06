package com.tozny.e3db;

import okio.ByteString;

import static com.tozny.e3db.Checks.checkNotEmpty;

class Base64 {
  public static String encodeURL(byte[] bytes) {
    checkNotEmpty(bytes, "bytes");
    String s = ByteString.of(bytes).base64Url();
    int paddingIdx = s.indexOf("=");
    if(paddingIdx > -1)
      return s.substring(0, paddingIdx);
    else
      return s;
  }

  public static byte[] decodeURL(String encoded) {
    checkNotEmpty(encoded, "encoded");
    final ByteString byteString = ByteString.decodeBase64(encoded);
    if(byteString == null)
      return null;
    else
      return byteString.toByteArray();
  }
}
