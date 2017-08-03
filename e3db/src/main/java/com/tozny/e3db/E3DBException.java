package com.tozny.e3db;

public class E3DBException extends Exception {
  public E3DBException(String message, Exception cause) {
    super(message, cause);
  }
  public E3DBException(String message) {
    super(message);
  }
}
