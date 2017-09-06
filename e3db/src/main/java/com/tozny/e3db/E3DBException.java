package com.tozny.e3db;

public class E3DBException extends Exception {
  public E3DBException(String message, Exception cause) {
    super(message, cause);
  }

  public E3DBException(String message) {
    super(message);
  }

  public static E3DBException find(int code, String message) {
    switch(code) {
      case 401:
        return new E3DBUnauthorizedException(code, message);
      case 403:
        return new E3DBForbiddenException(code, message);
      default:
        return new E3DBException("HTTP " + code + " " + message);
    }
  }
}
