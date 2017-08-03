package com.tozny.e3db;

public class E3DBUnauthorizedException extends E3DBException {
  public final int code;
  public final String message;

  public E3DBUnauthorizedException(int code, String message) {
    super("HTTP " + code + ": " + message);
    this.code = code;
    this.message = message;
  }
}
